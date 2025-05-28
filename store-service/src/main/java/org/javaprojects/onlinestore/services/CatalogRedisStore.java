package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.entities.Cart;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;

import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

import static org.javaprojects.onlinestore.security.SecurityUtil.*;

/**
 * This class is responsible for managing the catalog of items in the online store.
 * It provides methods to find items by ID, increment and decrement item counts,
 * and retrieve paginated lists of items with sorting and searching capabilities.
 */
@Component
public class CatalogRedisStore {
    private static final Logger log = LoggerFactory.getLogger(CatalogRedisStore.class);
    private static final String KEY_ITEM        = "item:%d";
    private static final String Z_PRICE         = "z:price";
    private static final String Z_TITLE         = "z:title";
    private static final String ID              = "id";
    private static final String TITLE           = "title";
    private static final String DESCRIPTION     = "description";
    private static final String PRICE           = "price";
    private static final String IMG             = "img";
    private static final String COUNT           = "count";
    private static final String KEY_CART_COUNT  = "count:%d:%d";
    public static final char KEY_DELIMITER      = '|';

    private final ReactiveRedisTemplate<String, String> redis;
    private final CacheLoader cacheLoader;

    public CatalogRedisStore(ReactiveRedisTemplate<String, String> redis, CacheLoader loader)
    {
        this.redis = redis;
        this.cacheLoader = loader;
    }

    /**
     * Resets the count of an item in the basket for a specific user to zero.
     * @param id ID of the item to reset.
     * @param userId ID of the user whose basket is being modified.
     * @return Mono indicating completion of the operation.
     */
    public Mono<Boolean> resetCountValue(long id, long userId) {
        String key = cartCountKey(id, userId);
        return redis.opsForValue()
            .set(key, "0");
    }

    /**
     * Increments the count of an item in the basket for a specific user.
     * If the count goes below zero, it resets the count to zero.
     * @param id ID of the item to increment.
     * @param delta Amount to increment or decrement the count by.
     * @param userId ID of the user whose basket is being modified.
     * @return Mono containing the new count of the item in the basket.
     */
    public Mono<Long> increment(long id, long delta, long userId) {
        String key = cartCountKey(id, userId);
        return redis.opsForValue()
            .increment(key, delta)
                .flatMap(newValue -> {
                    if (newValue <= 0) {
                        log.debug("Item count is 0. Resetting the count for item ID: [{}]", id);
                        return redis.opsForValue().set(key, "0")
                            .thenReturn(0L);
                    }
                    log.debug("Item count is greater than 0. New value: [{}]", newValue);
                    return redis.opsForValue().get(key)
                        .map(Long::parseLong);
                })
            .flatMap(newValue -> cacheLoader.updateItemCount(id, userId, newValue))
            .map(Cart::getQuantity);
    }

    /**
     * Increments the count of an item in the basket for a specific user.
     * @param id ID of the item to increment.
     * @param userId ID of the user whose basket is being modified.
     * @return Mono containing the new count of the item in the basket.
     */
    public Mono<Long> incrementCount(long id, long userId) {
        log.debug("Incrementing the count of item. ID: [{}]", id);
        return increment(id, 1L, userId);
    }

    /**
     * Decrements the count of an item in the basket for a specific user.
     * If the count goes below zero, it resets the count to zero.
     * @param id ID of the item to decrement.
     * @param userId ID of the user whose basket is being modified.
     * @return Mono containing the new count of the item in the basket.
     */
    public Mono<Long> decrementCount(long id, long userId) {
        log.debug("Decrementing the count of item. ID: [{}]", id);
        return increment(id, -1L, userId);
    }

    /**
     * Finds an item by its ID from Redis or loads it from the cache if not found.
     * @param id ID of the item to find.
     * @return Mono containing the ItemModel if found, or empty if not found.
     */
    public Mono<ItemModel> findById(long id) {
        return redis.<String, String>opsForHash()
            .entries(itemKey(id))
            .switchIfEmpty(cacheLoader.loadItem(id))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .flatMap(map -> findCountForItem(id)
                .doOnNext(quantity -> map.put(COUNT, quantity))
                .thenReturn(map))
            .map(this::toItemModel);
    }

    /**
     * Finds all items in the catalog with pagination and sorting.
     * @param page Page number to retrieve.
     * @param size Number of items per page.
     * @param search Search query to filter items.
     * @param sort Sorting criteria for the items.
     * @return Flux of ItemModel containing the items.
     */
    public Flux<ItemModel> findPage(int page, int size, String search, Sorting sort) {
        Flux<Long> idsMono = switch (sort) {
            case PRICE, NO -> idsByPrice(page, size);
            case ALPHA -> idsByTitle(page, size);
        };
        return idsMono
            .switchIfEmpty(cacheLoader.loadPages(page, size, search, sort))
            .doOnNext(i -> log.debug("Item ID is going to be mapped: {}", i))
            .flatMap(this::findById)
            .filter(i -> matches(i, search));
    }

    /**
     * Retrieves the total count of items in the catalog.
     * @return Mono containing the count of items.
     */
    private Flux<Long> idsByPrice(int page, int size) {

        long offset = (long) page * size;

        Range<Double> all = Range
            .from(Range.Bound.<Double>unbounded())
            .to(  Range.Bound.unbounded());

        Limit limit = Limit.limit().offset((int)offset).count(size);
        log.debug("idsByPrice: Page offset: {}", offset);
        return redis.opsForZSet()
            .rangeByScore(Z_PRICE, all, limit)
            .map(Long::parseLong);
    }
    /**
     * Retrieves item IDs sorted by title from Redis.
     * @param page Page number to retrieve.
     * @param size Number of items per page.
     * @return Flux of item IDs.
     */
    private Flux<Long> idsByTitle(int page, int size) {

        long offset = (long) page * size;

        Range<String> all = Range
            .from(Range.Bound.<String>unbounded())
            .to(  Range.Bound.unbounded());

        Limit limit = Limit.limit().offset((int)offset).count(size);

        return redis.opsForZSet()
            .rangeByLex(Z_TITLE, all, limit)
            .map(member -> Long.parseLong(
                member.substring(member.lastIndexOf(KEY_DELIMITER) + 1)));
    }
    /**
     * Constructs a Redis key for an item based on its ID.
     * @param itemId ID of the item.
     * @return Redis key as a String.
     */
    private String itemKey(long itemId) {
        return KEY_ITEM.formatted(itemId);
    }

    /**
     * Constructs a Redis key for the cart count of a specific item for a user.
     * @param itemId ID of the item.
     * @param userId ID of the user.
     * @return Redis key as a String.
     */
    private String cartCountKey(long itemId, long userId) {
        return KEY_CART_COUNT.formatted(itemId, userId);
    }

    /**
     * Checks if the item matches the search query.
     * @param i ItemModel to check.
     * @param q Search query.
     * @return true if the item matches the query, false otherwise.
     */
    private boolean matches(ItemModel i, String q) {
        if (q.isBlank()) return true;
        String l = q.toLowerCase();
        return i.getTitle().toLowerCase().contains(l)
            || i.getDescription().toLowerCase().contains(l);
    }

    /**
     * Converts a map of item properties to an ItemModel.
     * @param m Map containing item properties.
     * @return ItemModel constructed from the map.
     */
    private ItemModel toItemModel(Map<String,String> m) {
        return new ItemModel()
            .setId     (Long.parseLong(m.get(ID)))
            .setTitle  (m.get(TITLE))
            .setDescription(m.get(DESCRIPTION))
            .setPrice  (new BigDecimal(m.get(PRICE)))
            .setImgPath(m.get(IMG))
            .setCount  (Integer.parseInt(m.getOrDefault(COUNT, "0")));
    }

    /**
     * Finds the count of items in the basket for the current user.
     * If the user is anonymous, it returns "0".
     * @param itemId ID of the item to find the count for.
     * @return Mono containing the count as a String.
     */
    public Mono<String> findCountForItem(Long itemId) {
        return currentUser()
            .filter(u -> u.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(ROLE_ANONYMOUS)))
            .flatMap(authUser -> isAnonymous(itemId, authUser)
                ? Mono.just("0")
                : cacheLoader.getQuantity(itemId, authUser.getId()));
    }

    /**
     * Checks if the user is anonymous and logs a debug message if so.
     * @param itemId ID of the item being checked.
     * @param authUser The authenticated user.
     * @return true if the user is anonymous, false otherwise.
     */
    private static boolean isAnonymous(Long itemId, AuthUser authUser) {
        if (authUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(ROLE_ANONYMOUS))) {
            log.debug("Anonymous user is trying to get count of. Returning count as 0 for item ID: {}", itemId);
            return true;
        }
        return false;
    }
}
