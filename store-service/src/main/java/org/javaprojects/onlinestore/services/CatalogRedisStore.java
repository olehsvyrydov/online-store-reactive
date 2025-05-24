package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.entities.Cart;
import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.models.ItemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;

import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Mono<Void> save(Item item) {
        return cacheLoader.save(item).then();
    }

    public Mono<Boolean> resetCountValue(long id, long userId) {
        String key = cart_count_key(id, userId);
        return redis.opsForValue()
            .set(key, "0");
//            .then(findById(id, userId));
    }

    public Mono<Long> increment(long id, long delta, long userId) {
        String key = cart_count_key(id, userId);
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

    public Mono<Long> incrementCount(long id, long userId) {
        log.debug("Incrementing the count of item. ID: [{}]", id);
        return increment(id, 1L, userId);
//            .then(findById(id, userId));
    }

    public Mono<Long> decrementCount(long id, long userId) {
        log.debug("Decrementing the count of item. ID: [{}]", id);
        return increment(id, -1L, userId);
//            .onErrorComplete()
//            .then(findById(id, userId));
    }

    public Mono<ItemModel> findById(long id, long userId) {
        return redis.<String, String>opsForHash()
            .entries(item_key(id))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .flatMap(map -> {
                if (map.isEmpty()) {
                    log.debug("Item not found in cache. Loading from DB. ID: {}", id);
                    return cacheLoader.loadItem(id);
                }
                log.debug("Item found in cache. ID: {}", id);
                return Mono.just(map);
            })
            .flatMap(map ->
                findCountForItem(id, userId)
                    .doOnNext(quantity -> map.put(COUNT, quantity))
                    .thenReturn(map))
            .filter(map -> !map.isEmpty())
            .map(this::toItemModel);
    }

    public Flux<ItemModel> findPage(int page, int size, String search, Sorting sort, long userId) {
        Mono<List<Long>> idsMono = switch (sort) {
            case PRICE, NO -> idsByPrice(page, size);
            case ALPHA -> idsByTitle(page, size);
        };
        return idsMono
            .flatMap(list -> {
                if (list.isEmpty()) {
                    log.debug("No items found for page: {}, size: {}, search: {}, sort: {}, They will be loaded", page, size, search, sort);
                    return cacheLoader.loadPages(page, size, search, sort);
                }
                log.debug("Items in cache: {}", list);
                return Mono.just(list);
            })
            .flatMapMany(Flux::fromIterable)
            .doOnNext(i -> log.debug("Item ID is going to be mapped: {}", i))
            .flatMap(i -> findById(i, userId))
            .filter(i -> matches(i, search));
    }

    private Mono<List<Long>> idsByPrice(int page, int size) {

        long offset = (long) page * size;

        Range<Double> all = Range
            .from(Range.Bound.<Double>unbounded())
            .to(  Range.Bound.unbounded());

        Limit limit = Limit.limit().offset((int)offset).count(size);
        log.debug("idsByPrice: Page offset: {}", offset);
        return redis.opsForZSet()
            .rangeByScore(Z_PRICE, all, limit)
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }

    private Mono<List<Long>> idsByTitle(int page, int size) {

        long offset = (long) page * size;

        Range<String> all = Range
            .from(Range.Bound.<String>unbounded())
            .to(  Range.Bound.unbounded());

        Limit limit = Limit.limit().offset((int)offset).count(size);

        return redis.opsForZSet()
            .rangeByLex(Z_TITLE, all, limit)
            .map(member -> Long.parseLong(
                member.substring(member.lastIndexOf(KEY_DELIMITER) + 1)))
            .collect(Collectors.toList());
    }
    private String item_key(long itemId) {
        return KEY_ITEM.formatted(itemId);
    }

    private String cart_count_key(long itemId, long userId) {
        return KEY_CART_COUNT.formatted(itemId, userId);
    }

    private boolean matches(ItemModel i, String q) {
        if (q.isBlank()) return true;
        String l = q.toLowerCase();
        return i.getTitle().toLowerCase().contains(l)
            || i.getDescription().toLowerCase().contains(l);
    }

    private ItemModel toItemModel(Map<String,String> m) {
        ItemModel itemModel = new ItemModel();
        m.computeIfPresent(ID, (k,v) -> {itemModel.setId(Long.parseLong(v)); return v;});
        m.computeIfPresent(TITLE, (k,v) -> {itemModel.setTitle(v); return v;});
        m.computeIfPresent(DESCRIPTION, (k,v) -> {itemModel.setDescription(v); return v;});
        m.computeIfPresent(PRICE, (k,v) -> {itemModel.setPrice(BigDecimal.valueOf(Float.parseFloat(v))); return v;});
        m.computeIfPresent(IMG, (k,v) -> {itemModel.setImgPath(v); return v;});
        m.computeIfPresent(COUNT, (k,v) -> {itemModel.setCount(Integer.parseInt(v)); return v;});
        log.debug("Item map updated to ItemModel with id: {}", itemModel.getId());
        return itemModel;
    }

    public Mono<String> findCountForItem(Long itemId, Long userId)
    {
        return cacheLoader.getQuantity(itemId, userId);
    }
}
