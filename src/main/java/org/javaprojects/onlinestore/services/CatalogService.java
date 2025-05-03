package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.entities.*;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.models.*;
import org.javaprojects.onlinestore.repositories.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogService {
    private final ItemsRepository itemRepository;
    private final OrdersRepository ordersRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;

    public CatalogService(ItemsRepository itemRepository,
        OrdersRepository ordersRepository,
        CartRepository cartRepository,
        OrderItemRepository orderItemRepository) {
        this.itemRepository = itemRepository;
        this.ordersRepository = ordersRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Mono<AllItemsModel> findAllItems(int pageNumber, int pageSize, String searchString, Sorting sorting) {
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
            .collectList()
            .zipWith(itemRepository.count())
            .map(tuple -> {
                List<ItemModel> items = tuple.getT1();
                long total = tuple.getT2();
                return new AllItemsModel(items, new PageImpl<>(items, pageable, total));
            });
    }

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

    public Mono<Void> incrementQuantity(Long itemId) {
        return cartRepository.findByItemId(itemId)
            .switchIfEmpty(cartRepository.insertToCart(itemId))
            .then(cartRepository.incrementItemCount(itemId))
            .then();
    }

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

                return ordersRepository.save(order)
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
                    });
            });
    }
}
