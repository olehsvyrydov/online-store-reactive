package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.api.BalanceApi;
import org.javaprojects.onlinestore.api.PaymentApi;
import org.javaprojects.onlinestore.entities.*;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.exceptions.InsufficientFundsException;
import org.javaprojects.onlinestore.models.*;
import org.javaprojects.onlinestore.repositories.*;
import org.javaprojects.onlinestore.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.*;

import static org.javaprojects.onlinestore.security.SecurityUtil.currentUser;

/**
 * This service class handles all operations related to the catalog of items,
 * including retrieving items, managing the user's basket, processing orders,
 * and interacting with external APIs for balance and payment.
 * It uses caching to optimize performance and reduce a database load.
 */
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

    /**
     * This method retrieves all items from the catalog with pagination and sorting.
     * It uses caching to avoid unnecessary database calls.
     * @param pageNumber Page number for pagination
     * @param pageSize Size of each page
     * @param searchString Search string for filtering items
     * @param sorting Sorting criteria
     * @return Flux<ItemModel> with all items in the catalog
     */
    public Flux<ItemModel> findAllItems(int pageNumber, int pageSize, String searchString, Sorting sorting) {
        return cache.findPage(pageNumber, pageSize, searchString, sorting);
    }

    /**
     * This method retrieves the total number of items in the catalog.
     * It uses caching to avoid unnecessary database calls.
     * @return Mono<Long> with the total count of items
     */
    @Cacheable(value = "itemsSize", key = "'total'")
    public Mono<Long> getItemsCount()
    {
        return itemRepository.count();
    }

    /**
     * This method retrieves an item by its ID.
     * It uses the cache to find the item and returns a Mono<ItemModel>.
     * @param id Item ID
     * @return Mono<ItemModel> with the item details or an error if not found
     */
    public Mono<ItemModel> getItemById(long id) {
        return cache.findById(id);
    }

    /**
     * This method increments the quantity of an item in the user's basket.
     * It updates the cache and returns a Mono<Void> indicating completion.
     * @param itemId Item ID to be incremented
     * @param authUser Authenticated user
     * @return Mono<Void> indicating completion
     */
    public Mono<Void> incrementQuantity(Long itemId, AuthUser authUser) {
        return cache.incrementCount(itemId, authUser.getId())
                 .then();
    }

    /**
     * This method decrements the quantity of an item in the user's basket.
     * If the new quantity is zero, it deletes the item from the basket.
     * @param itemId Item ID to be decremented
     * @param authUser Authenticated user
     * @return Mono<Void> indicating completion
     */
    public Mono<Void> decrementQuantity(Long itemId, AuthUser authUser) {
        return cache.decrementCount(itemId, authUser.getId())
            .flatMap(newValue -> {
                if (newValue <= 0) {
                    log.debug("Item count is zero, deleting item from basket");
                    return deleteItemFromBasket(itemId, authUser);
                }
                return Mono.empty();
            })
            .then();
    }

    /**
     * This method deletes an item from the user's basket.
     * It removes the item from the cart and resets the count in the cache.
     * @param itemId Item ID to be deleted
     * @param authUser Authenticated user
     * @return Mono<Void> indicating completion
     */
    public Mono<Void> deleteItemFromBasket(Long itemId, AuthUser authUser) {
        return cartRepository.removeFromCart(itemId, authUser.getId())
            .then(cache.resetCountValue(itemId, authUser.getId()))
            .then();
    }

    /**
     * This method retrieves all items in the user's basket.
     * It fetches the cart for the user, then retrieves each item by ID from the cache.
     * @return Flux<ItemModel> with all items in the basket
     */
    public Flux<ItemModel> getItemsInBasket() {
        return currentUser()
            .switchIfEmpty(Mono.error(new UserPrincipalNotFoundException("User not authenticated")))
            .flatMapMany(user ->
                cartRepository.findByUserId(user.getId())
            .flatMap(cart -> cache.findById(cart.getItemId())));
    }

    /**
     * This method updates the count of items in the basket based on the action.
     * It supports actions like "PLUS", "MINUS", and "DELETE".
     * @param id Item ID
     * @param action Action to be performed
     * @param authUser Authenticated user
     * @return Mono<Void> indicating completion or an error if the action is invalid
     */
    @Transactional
    public Mono<Void> updateCountInBasket(Long id, String action, AuthUser authUser) {
        return switch (action.toUpperCase()) {
            case "PLUS", "ADD_TO_CART" -> incrementQuantity(id, authUser);
            case "MINUS" -> decrementQuantity(id, authUser);
            case "DELETE" -> deleteItemFromBasket(id, authUser);
            default -> Mono.error(new IllegalStateException("Invalid action: " + action));
        };
    }

    /**
     * This method retrieves all orders for the authenticated user.
     * It fetches orders and their items, then maps them to OrderModel.
     * @return Flux<OrderModel> with all orders and their items
     */
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

    /**
     * This method retrieves an order by its ID for the authenticated user.
     * It fetches the order and its items, then maps them to OrderModel.
     * @param id Order ID
     * @param authUser Authenticated user
     * @return Mono<OrderModel> with the order details or an error if not found
     */
    public Mono<OrderModel> getOrderById(Long id, AuthUser authUser) {
        Mono<Order> orderMono = ordersRepository.findByIdAndUserId(id, authUser.getId())
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

    /**
     * This method processes the user's basket, charges their card, and saves the order.
     * It fetches items from the basket, calculates the total price, charges the card,
     * saves the order and its items, and cleans up the basket.
     * @param user Authenticated user
     * @return Mono<Long> with the saved order ID or -1 in case of an error
     */
    @Transactional
    public Mono<Long> buyItemsInBasket(AuthUser user) {

        return fetchOrderItems(user.getId())
            .collectList()
            .flatMap(items -> {

                BigDecimal total = getTotalPrice(items);

                Order order = new Order()
                    .setUserId(user.getId())
                    .setTotal(total);

                return chargeCard(total)
                    .then(saveOrderGraph(order, items))
                    .flatMap(savedId ->
                        cleanUpBasket(user.getId(), items)
                            .thenReturn(savedId));
            })
            .onErrorResume(e -> Mono.just(-1L));
    }


    /* ---------- helpers --------------------------------------------------- */

    /**
     * Fetch all items in the user's cart and their quantities.
     * This method retrieves the cart for the user, fetches each item by ID,
     * and combines it with the quantity from the cache.
     * @param userId User ID
     * @return Flux<OrderItem> containing items and their quantities
     */
    private Flux<OrderItem> fetchOrderItems(Long userId) {
        return cartRepository.findByUserId(userId)                     // Flux<Cart>
            .flatMap(cart ->
                itemRepository.findById(cart.getItemId())      // Mono<Item>
                    .zipWith(cache.findCountForItem(cart.getItemId()))
                    .map(tuple -> new OrderItem()
                        .setItemId(tuple.getT1().getId())
                        .setQuantity(Long.parseLong(tuple.getT2()))
                        .setItem(tuple.getT1())));
    }

    /**
     * Charge the user's card for the total amount.
     * This method retrieves the user's balance, verifies it, and then makes a payment.
     * @param amount Amount to charge
     * @return Mono<Void> indicating completion
     */
    private Mono<Void> chargeCard(BigDecimal amount) {
        return balanceApi.getBalance()
            .flatMap(this::verifyBalance)
            .flatMap(bal -> paymentApi.makePayment(amount.floatValue()))
            .flatMap(this::verifyPayment)
            .then();
    }

    /**
     * Save the order and its items in a single transaction.
     * Persist Order first, then its OrderItems.
     * @param order Order to be saved
     * @param items List of OrderItem to be saved
     * @return Mono<Long> with the saved order ID
     */
    private Mono<Long> saveOrderGraph(Order order, List<OrderItem> items) {
        return ordersRepository.save(order)                            // Mono<Order>
            .flatMap(saved -> {
                items.forEach(i ->
                    i.setOrderId(saved.getId())
                     .setOrder(saved));
                return orderItemRepository.saveAll(items)
                    .then(Mono.just(saved.getId()));
            });
    }

    /**
     * Reset Redis counters and delete the cart rows.
     * @param userId User ID
     * @param items List of OrderItem
     * @return Mono<Void> indicating completion
     */
    private Mono<Void> cleanUpBasket(Long userId, List<OrderItem> items) {
        Mono<Void> resetCache = Flux.fromIterable(items)
            .flatMap(i -> cache.resetCountValue(i.getItemId(), userId))
            .then();
        Mono<Void> clearCart  = cartRepository.deleteByUserId(userId);
        return Mono.when(resetCache, clearCart).then();
    }

    /**
     * Verify that the balance is present and sufficient.
     * @param r GetBalanceResponse
     * @return Mono<Float> with the balance if valid, or an error if not.
     */
    private Mono<Float> verifyBalance(GetBalanceResponse r) {
        if (r == null || r.getBalance() == null) {
            return Mono.error(new IllegalStateException("Balance not found"));
        }
        if (r.getBalance().compareTo(0f) <= 0) {
            return Mono.error(new InsufficientFundsException("Insufficient funds", r.getBalance()));
        }
        return Mono.just(r.getBalance());
    }

    /**
     * Verify that the payment was successful.
     * @param response UpdateBalanceResponse
     * @return Mono<T> with the response if valid, or an error if not.
     */
    private <T extends UpdateBalanceResponse> Mono<T> verifyPayment(T response) {
        if (Boolean.TRUE.equals(response.getSuccess()))
            return Mono.just(response);
        return Mono.error(new IllegalStateException("Payment failed"));
    }

    /**
     * Calculate the total price of all items in the order.
     * @param orderItems List of OrderItem
     * @return BigDecimal representing the total price
     */
    private static BigDecimal getTotalPrice(List<OrderItem> orderItems)
    {
        return orderItems.stream()
            .map(orderItem ->
                BigDecimal.valueOf(orderItem.getQuantity()).multiply(orderItem.getItem().getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

//    private Mono<Void> processPayment(Mono<GetBalanceResponse> balanceMono, float total)
//    {
//        return balanceMono
//            .flatMap(this::requireNonNullBalance)
//            .filter(bal -> bal.compareTo(0f) > 0)
//            .switchIfEmpty(Mono.error(
//                () -> new InsufficientFundsException("Insufficient funds", 0f)))
//
//            .filter(bal -> bal.compareTo(total) >= 0)
//            .switchIfEmpty(Mono.error(
//                () -> new InsufficientFundsException("Insufficient funds", total)))
//
//            .flatMap(bal -> paymentApi.makePayment(total))
//
//            .filter(response -> Boolean.TRUE.equals(response.getSuccess()))
//            .switchIfEmpty(Mono.error(() -> new IllegalStateException("Payment failed")))
//
//            .then();
//    }

    //    @Transactional
    //    public Mono<Long> buyItemsInBasket(AuthUser authUser) {
    //        Flux<Cart> cartFlux = cartRepository.findByUserId(authUser.getId());
    //
    //        Flux<Item> itemFlux = cartFlux
    //            .flatMap(cart -> itemRepository.findById(cart.getItemId()))
    //            .doOnNext(item ->
    //                log.debug("Item found in cart: {}", item)
    //            );
    //
    //        Mono<GetBalanceResponse> balanceMono = balanceApi.getBalance()
    //            .doOnNext(balance ->
    //                log.debug("Balance found: {}", balance)
    //            );
    //
    //        return itemFlux
    //            .flatMap(item ->
    //                cache.findCountForItem(item.getId())
    //                    .map(count ->
    //                        new OrderItem()
    //                            .setItemId(item.getId())
    //                            .setQuantity(Long.parseLong(count))
    //                            .setItem(item)
    //                    )
    //            )
    //            .collectList()
    //            .flatMap(orderItems -> {
    //                BigDecimal totalPrice = getTotalPrice(orderItems);
    //
    //                Order order = new Order();
    //                order.setTotal(totalPrice);
    //                order.setUserId(authUser.getId());
    //                log.debug("Order processing: Total: {}", order.getTotal());
    //
    //                return processPayment(balanceMono, totalPrice.floatValue())
    //                    .then(Mono.defer(() -> ordersRepository.save(order)
    //                        .flatMap(savedOrder -> {
    //                            orderItems.forEach(orderItem -> {
    //                                orderItem.setOrderId(savedOrder.getId());
    //                                orderItem.setOrder(order);
    //                            });
    //                            return orderItemRepository.saveAll(orderItems)
    //                                .flatMap(orderItem -> {
    //                                    Item item = orderItem.getItem();
    //                                    log.debug("Updating item stock when buy items: {}", item);
    //                                    return cache.resetCountValue(item.getId(), authUser.getId());
    //                                })
    //                                .then(cartRepository.deleteByUserId(authUser.getId()))
    //                                .thenReturn(savedOrder.getId());
    //                        })
    //                        .onErrorReturn(Exception.class, -1L)));
    //            });
    //    }


//    private Mono<Float> requireNonNullBalance(GetBalanceResponse dto) {
//        if (dto == null || dto.getBalance() == null) {
//            return Mono.error(new IllegalStateException("Balance not found"));
//        }
//        return Mono.just(dto.getBalance());
//    }
}
