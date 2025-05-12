package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.api.BalanceApi;
import org.javaprojects.onlinestore.api.PaymentApi;
import org.javaprojects.onlinestore.entities.*;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.exceptions.InsufficientFundsException;
import org.javaprojects.onlinestore.models.*;
import org.javaprojects.onlinestore.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogService {
    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private final ItemsRepository itemRepository;
    private final OrdersRepository ordersRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final BalanceApi balanceApi;
    private final PaymentApi paymentApi;

    public CatalogService(ItemsRepository itemRepository,
        OrdersRepository ordersRepository,
        CartRepository cartRepository,
        OrderItemRepository orderItemRepository,
        BalanceApi balanceApi,
        PaymentApi paymentApi) {
        this.itemRepository = itemRepository;
        this.ordersRepository = ordersRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
        this.balanceApi = balanceApi;
        this.paymentApi = paymentApi;
    }

    @Cacheable(
        value = "allItems",
        key = "{#pageNumber, #pageSize, #searchString, #sorting}",
        unless = "#result == null || #result.isEmpty()")
    public Mono<List<ItemModel>> findAllItems(int pageNumber, int pageSize, String searchString, Sorting sorting) {
        Sort sorted = switch (sorting) {
            case NO -> Sort.unsorted();
            case PRICE -> Sort.by("price").ascending();
            case ALPHA -> Sort.by("title").ascending();
        };
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sorted);
        Flux<Item> page = searchString.isEmpty()
            ? itemRepository.findBy(pageable)
            : itemRepository.findBySearchString(searchString, pageable);

        return page
            .map(i -> new ItemModel(
                i.getId(),
                i.getTitle(),
                i.getDescription(),
                i.getPrice(),
                i.getImgPath(),
                i.getCount())
            )
            .collectList();
    }

    @Cacheable(value = "itemsSize", key = "'total'")
    public Mono<Long> getItemsCount()
    {
        return itemRepository.count();
    }

    @Cacheable(value = "items", key = "#id")
    public Mono<ItemModel> getItemById(Long id) {
        return itemRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalStateException("Item not found")))
            .map(item -> new ItemModel(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getPrice(),
                item.getImgPath(),
                item.getCount()));
    }

    @CacheEvict(value = "items", key = "#itemId")
    public Mono<Void> incrementQuantity(Long itemId) {
        return cartRepository.findByItemId(itemId)
            .switchIfEmpty(cartRepository.insertToCart(itemId))
            .then(cartRepository.incrementItemCount(itemId))
            .then();
    }

    @CacheEvict(value = "items", key = "#itemId")
    public Mono<Void> decrementQuantity(Long itemId) {
        return cartRepository.decrementItemCount(itemId)
            .flatMap(item -> {
                if (item.getCount() <= 0) {
                    return cartRepository.removeFromCart(itemId);
                }
                return Mono.empty();
            })
            .then();
    }

    @CacheEvict(value = "items", key = "#itemId")
    public Mono<Void> deleteItemFromBasket(Long itemId) {
        return cartRepository.removeFromCart(itemId)
            .then(cartRepository.resetItemCount(itemId))
            .then();
    }

    public Flux<ItemModel> getItemsInBasket() {
        return cartRepository.findAll()
            .flatMap(cart -> itemRepository.findById(cart.getItemId()))
            .map(item -> new ItemModel(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getPrice(),
                item.getImgPath(),
                item.getCount()
            ));
    }

    @Transactional
    public Mono<Void> updateCountInBasket(Long id, String action) {
        return switch (action.toUpperCase()) {
            case "PLUS" -> incrementQuantity(id);
            case "MINUS" -> decrementQuantity(id);
            case "DELETE" -> deleteItemFromBasket(id);
            default -> Mono.error(new IllegalStateException("Invalid action: " + action));
        };
    }

    public Flux<OrderModel> findAllOrders() {
        Flux<Order> orderFlux = ordersRepository.findAll();

        Flux<OrderItem> orderItemFlux = orderFlux
            .flatMap(order -> orderItemRepository.findByOrderId(order.getId())
                .doOnNext(orderItem -> orderItem.setOrder(order)));

        Map<Long, OrderModel> orderModelMap = new HashMap<>();

        return orderItemFlux
            .flatMap(orderItem ->
                itemRepository.findById(orderItem.getItemId())
                    .doOnNext(orderItem::setItem).thenReturn(orderItem)
            )
            .doOnNext(orderItem -> {
                Item item = orderItem.getItem();
                ItemModel itemModel = new ItemModel(item.getId(),
                    item.getTitle(),
                    item.getDescription(),
                    item.getPrice(),
                    item.getImgPath(),
                    orderItem.getQuantity());

                orderModelMap.computeIfAbsent(orderItem.getOrderId(), k -> new OrderModel())
                        .addItem(itemModel)
                        .setId(orderItem.getOrderId())
                        .setTotalSum(orderItem.getOrder().getTotal());
            })
            .thenMany(Flux.fromIterable(orderModelMap.values()));
    }

    public Mono<OrderModel> getOrderById(Long id) {
        Mono<Order> orderMono = ordersRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalStateException("Order not found")));

        Flux<OrderItem> orderItemFlux = orderMono
            .flatMapMany(order -> orderItemRepository.findByOrderId(order.getId()));

        return orderItemFlux
            .flatMap(orderItem -> itemRepository.findById(orderItem.getItemId()))
            .zipWith(orderItemFlux)
            .collectList()
            .zipWith(orderItemFlux.collectList())
            .map(tl -> tl.getT1().stream()
                .map(t -> {
                    Item item = t.getT1();
                    OrderItem orderItem = t.getT2();
                    return new ItemModel(t.getT1().getId(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getPrice(),
                            item.getImgPath(),
                            orderItem.getQuantity());
                })
                .toList()
            )
            .zipWith(orderMono)
            .map(t -> {
                List<ItemModel> itemModels = t.getT1();
                Order order = t.getT2();
                return new OrderModel(order.getId(), itemModels, order.getTotal());
            });
    }

    @Transactional
    public Mono<Long> buyItemsInBasket() {
        Flux<Cart> cartFlux = cartRepository.findAll();

        Flux<Item> itemFlux = cartFlux
            .flatMap(cart -> itemRepository.findById(cart.getItemId()));

        Mono<GetBalanceResponse> balanceMono = balanceApi.getBalance();

        return itemFlux
                .map(item ->
                    new OrderItem()
                        .setItemId(item.getId())
                        .setQuantity(item.getCount())
                        .setItem(item)
                )
            .collectList()
            .flatMap(orderItems -> {
                BigDecimal total = orderItems.stream()
                    .map(orderItem ->
                        BigDecimal.valueOf(orderItem.getQuantity()).multiply(orderItem.getItem().getPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Order order = new Order();
                order.setTotal(total);
                log.debug("Order processing: Total: {}", order.getTotal());

                return processPayment(balanceMono, total)
                    .then(Mono.defer(() -> ordersRepository.save(order)
                    .flatMap(savedOrder -> {
                        orderItems.forEach(orderItem -> {
                            orderItem.setOrderId(savedOrder.getId());
                            orderItem.setOrder(order);
                        });
                        return orderItemRepository.saveAll(orderItems)
                            .flatMap(orderItem -> {
                                Item item = orderItem.getItem();
                                item.setCount(0);
                                return itemRepository.save(item);
                            })
                            .then(cartRepository.deleteAll())
                            .thenReturn(savedOrder.getId());
                    })
                        .onErrorReturn(Exception.class, -1L)));
            });
    }

    private Mono<Void> processPayment(Mono<GetBalanceResponse> balanceMono, BigDecimal total)
    {
        return balanceMono
            .flatMap(balance -> {
                if (balance == null) {
                    return Mono.error(new IllegalStateException("Balance not found"));
                }
                else if (balance.getBalance() == null || balance.getBalance().compareTo(0f) <= 0) {
                    return Mono.error(new InsufficientFundsException("Insufficient funds", balance.getBalance()));
                }
                if (balance.getBalance().compareTo(total.floatValue()) < 0) {
                    return Mono.error(new InsufficientFundsException("Insufficient funds", balance.getBalance()));
                }
                return paymentApi.makePayment(total.floatValue())
                    .<UpdateBalanceResponse>handle((response, sink) ->
                    {
                        log.debug("Payment response: {}", response);
                        if (response.getSuccess() == null || !response.getSuccess())
                        {
                            sink.error(new IllegalStateException("Payment failed"));
                            return;
                        }
                        sink.next(response);
                    });
            })
            .then();
    }
}
