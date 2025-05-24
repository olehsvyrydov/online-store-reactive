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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

import static org.javaprojects.onlinestore.utils.SecurityUtil.currentUser;

@Service
public class CatalogService {
    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private final ItemsRepository itemRepository;
    private final OrdersRepository ordersRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final BalanceApi balanceApi;
    private final PaymentApi paymentApi;
    private final CatalogRedisStore cache;

    public CatalogService(ItemsRepository itemRepository,
        OrdersRepository ordersRepository,
        CartRepository cartRepository,
        OrderItemRepository orderItemRepository,
        BalanceApi balanceApi,
        PaymentApi paymentApi,
        CatalogRedisStore cache) {
        this.itemRepository = itemRepository;
        this.ordersRepository = ordersRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
        this.balanceApi = balanceApi;
        this.paymentApi = paymentApi;
        this.cache = cache;
    }

    public Flux<ItemModel> findAllItems(int pageNumber, int pageSize, String searchString, Sorting sorting) {
        return currentUser().flatMapMany(user ->
                cache.findPage(pageNumber, pageSize, searchString, sorting, user.getId()));
    }

//    private ItemModel toModel(Item e, long count) {
//        return new ItemModel(e.getId(), e.getTitle(), e.getDescription(),
//            e.getPrice(), e.getImgPath(), count);
//    }

    @Cacheable(value = "itemsSize", key = "'total'")
    public Mono<Long> getItemsCount()
    {
        return itemRepository.count();
    }

    public Mono<ItemModel> getItemById(long id) {
        return currentUser().flatMap(user -> cache.findById(id, user.getId()));
    }

    public Mono<Void> incrementQuantity(Long itemId, Mono<AppUser> appUser) {
        return appUser.flatMap(user ->
            cache.incrementCount(itemId, user.getId()))
                 .then();
    }

    public Mono<Void> decrementQuantity(Long itemId, Mono<AppUser> appUser) {
        return appUser.flatMap(user ->
            cache.decrementCount(itemId, user.getId()))
            .flatMap(newValue -> {
                if (newValue <= 0) {
                    log.debug("Item count is zero, deleting item from basket");
                    return deleteItemFromBasket(itemId, appUser);
                }
                return Mono.empty();
            })
            .then();

//            .then(Mono.defer(() -> itemRepository.findById(itemId)))
//            .flatMap(item -> {
//                log.debug("Decrement count for item id: {}, title: {}, count: {}",
//                    item.getId(), item.getTitle(), item.getCount());
//                if (item.getCount() <= 0) {
//                    return deleteItemFromBasket(itemId, appUser);
//                }
//                return cache.decrementCount(itemId, user.getId()).then();
//            }))
//            .then();
    }

    public Mono<Void> deleteItemFromBasket(Long itemId, Mono<AppUser> appUser) {
        return appUser.flatMap(user ->
            cartRepository.removeFromCart(itemId, user.getId())
            .then(cache.resetCountValue(itemId, user.getId())))
            .then();
    }

    public Flux<ItemModel> getItemsInBasket() {
        return currentUser()
            .flatMapMany(user ->
                cartRepository.findByUserId(user.getId())
            .flatMap(cart -> cache.findById(cart.getItemId(), user.getId())));
    }

    @Transactional
    public Mono<Void> updateCountInBasket(Long id, String action) {
        Mono<AppUser> appUser = currentUser();
        return switch (action.toUpperCase()) {
            case "PLUS", "ADD_TO_CART" -> incrementQuantity(id, appUser);
            case "MINUS" -> decrementQuantity(id, appUser);
            case "DELETE" -> deleteItemFromBasket(id, appUser);
            default -> Mono.error(new IllegalStateException("Invalid action: " + action));
        };
    }

    public Flux<OrderModel> findAllOrders() {
        Flux<Order> orderFlux = currentUser().flatMapMany(user ->
            ordersRepository.findByUserId(user.getId()));

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
        Mono<Order> orderMono = currentUser().flatMap(user -> ordersRepository.findByIdAndUserId(id, user.getId())
            .switchIfEmpty(Mono.error(new IllegalStateException("Order not found"))));

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

//    @Transactional
//    public Mono<Long> buyItemsInBasket(AppUser user) {
//        return cartRepository.findByUserId(user.getId())
//            .collectList()
//            .flatMap(cartItems -> {
//                if (cartItems.isEmpty()) {
//                    return Mono.just(0L);
//                }
//
//                return Flux.fromIterable(cartItems)
//                    .flatMap(cart -> itemRepository.findById(cart.getItemId()))
//                    .collectList()
//                    .flatMap(items -> {
//                        Order order = new Order();
//                        order.setUserId(user.getId());
//                        order.setTotal(calculateTotal(items));
//
//                        return ordersRepository.save(order)
//                            .flatMap(savedOrder -> Flux.fromIterable(items)
//                                .flatMap(item -> {
//                                    OrderItem orderItem = new OrderItem()
//                                        .setOrderId(savedOrder.getId())
//                                        .setItemId(item.getId())
//                                        .setQuantity(1);
//
//                                    return orderItemRepository.save(orderItem)
//                                        .then(updateItemStock(item, user.getId()));
//                                })
//                                .then(cartRepository.deleteByUserId(user.getId()))
//                                .thenReturn(savedOrder.getId()));
//                    });
//            })
//            .onErrorResume(e -> {
//                log.error("Failed to process purchase", e);
//                return Mono.just(0L);
//            });
//    }

    @Transactional
    public Mono<Long> buyItemsInBasket(AppUser user) {
        Flux<Cart> cartFlux = cartRepository.findByUserId(user.getId());

        Flux<Item> itemFlux = cartFlux
            .flatMap(cart -> itemRepository.findById(cart.getItemId()))
            .doOnNext(item ->
                log.debug("Item found in cart: {}", item)
            );

        Mono<GetBalanceResponse> balanceMono = balanceApi.getBalance()
            .doOnNext(balance ->
                log.debug("Balance found: {}", balance)
            );

        return itemFlux
            .flatMap(item ->
                cache.findCountForItem(item.getId(), user.getId())
                    .map(count ->
                        new OrderItem()
                            .setItemId(item.getId())
                            .setQuantity(Long.parseLong(count))
                            .setItem(item)
                    )


            )
            .collectList()
            .flatMap(orderItems -> {
                BigDecimal total = orderItems.stream()
                    .map(orderItem ->
                        BigDecimal.valueOf(orderItem.getQuantity()).multiply(orderItem.getItem().getPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Order order = new Order();
                order.setTotal(total);
                order.setUserId(user.getId());
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
                                    log.debug("Updating item stock when buy items: {}", item);
                                    return cache.resetCountValue(item.getId(), user.getId());
                                })
                                .then(cartRepository.deleteByUserId(user.getId()))
                                .thenReturn(savedOrder.getId());
                        })
                        .onErrorReturn(Exception.class, -1L)));
            });
    }

//    private Mono<Void> updateItemStock(Item item, Long userId) {
//        return
//            cache.resetCountValue(item.getId(), userId)
//            .then();
//    }
//
//    private BigDecimal calculateTotal(List<Item> items) {
//        return items.stream()
//            .map(Item::getPrice)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }

    private Mono<Void> processPayment(Mono<GetBalanceResponse> balanceMono, BigDecimal total)
    {
        return balanceMono
            .flatMap(balance -> {
                if (balance == null || balance.getBalance() == null) {
                    return Mono.error(new IllegalStateException("Balance not found"));
                }
                else if (balance.getBalance().compareTo(0f) <= 0) {
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
