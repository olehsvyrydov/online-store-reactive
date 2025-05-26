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
        return cache.findPage(pageNumber, pageSize, searchString, sorting);
    }

    @Cacheable(value = "itemsSize", key = "'total'")
    public Mono<Long> getItemsCount()
    {
        return itemRepository.count();
    }

    public Mono<ItemModel> getItemById(long id) {
        return cache.findById(id);
    }

    public Mono<Void> incrementQuantity(Long itemId, AuthUser authUser) {
        return cache.incrementCount(itemId, authUser.getId())
                 .then();
    }

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

    public Mono<Void> deleteItemFromBasket(Long itemId, AuthUser authUser) {
        return cartRepository.removeFromCart(itemId, authUser.getId())
            .then(cache.resetCountValue(itemId, authUser.getId()))
            .then();
    }

    public Flux<ItemModel> getItemsInBasket() {
        return currentUser()
            .switchIfEmpty(Mono.error(new UserPrincipalNotFoundException("User not authenticated")))
            .flatMapMany(user ->
                cartRepository.findByUserId(user.getId())
            .flatMap(cart -> cache.findById(cart.getItemId())));
    }

    @Transactional
    public Mono<Void> updateCountInBasket(Long id, String action, AuthUser authUser) {
        return switch (action.toUpperCase()) {
            case "PLUS", "ADD_TO_CART" -> incrementQuantity(id, authUser);
            case "MINUS" -> decrementQuantity(id, authUser);
            case "DELETE" -> deleteItemFromBasket(id, authUser);
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

    @Transactional
    public Mono<Long> buyItemsInBasket(AuthUser authUser) {
        Flux<Cart> cartFlux = cartRepository.findByUserId(authUser.getId());

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
                cache.findCountForItem(item.getId())
                    .map(count ->
                        new OrderItem()
                            .setItemId(item.getId())
                            .setQuantity(Long.parseLong(count))
                            .setItem(item)
                    )
            )
            .collectList()
            .flatMap(orderItems -> {
                BigDecimal totalPrice = getTotalPrice(orderItems);

                Order order = new Order();
                order.setTotal(totalPrice);
                order.setUserId(authUser.getId());
                log.debug("Order processing: Total: {}", order.getTotal());

                return processPayment(balanceMono, totalPrice.floatValue())
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
                                    return cache.resetCountValue(item.getId(), authUser.getId());
                                })
                                .then(cartRepository.deleteByUserId(authUser.getId()))
                                .thenReturn(savedOrder.getId());
                        })
                        .onErrorReturn(Exception.class, -1L)));
            });
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

    private Mono<Float> requireNonNullBalance(GetBalanceResponse dto) {
        if (dto == null || dto.getBalance() == null) {
            return Mono.error(new IllegalStateException("Balance not found"));
        }
        return Mono.just(dto.getBalance());
    }


        private Mono<Void> processPayment(Mono<GetBalanceResponse> balanceMono, float total)
    {
        return balanceMono
            .flatMap(this::requireNonNullBalance)
            .filter(bal -> bal.compareTo(0f) > 0)
            .switchIfEmpty(Mono.error(
                () -> new InsufficientFundsException("Insufficient funds", 0f)))

            .filter(bal -> bal.compareTo(total) >= 0)
            .switchIfEmpty(Mono.error(
                () -> new InsufficientFundsException("Insufficient funds", total)))

            .flatMap(bal -> paymentApi.makePayment(total))

            .filter(response -> Boolean.TRUE.equals(response.getSuccess()))
            .switchIfEmpty(Mono.error(() -> new IllegalStateException("Payment failed")))

            .then();
    }
}
