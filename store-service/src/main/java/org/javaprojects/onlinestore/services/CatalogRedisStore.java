package org.javaprojects.onlinestore.services;

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
    private static final String KEY_ITEM   = "item:%d";
    private static final String Z_PRICE    = "z:price";
    private static final String Z_TITLE    = "z:title";
    private static final String ID          = "id";
    private static final String TITLE       = "title";
    private static final String DESCRIPTION = "description";
    private static final String PRICE       = "price";
    private static final String IMG         = "img";
    private static final String COUNT       = "count";
    public static final char KEY_DELIMITER  = '|';

    private final ReactiveRedisTemplate<String, String> redis;

    public CatalogRedisStore(ReactiveRedisTemplate<String, String> redis)
    {
        this.redis = redis;
    }

    public Mono<Void> save(ItemModel itemModel) {
        String key = key(itemModel.getId());
        log.debug("saving item to cache. Key: {}, Title: {}", key, itemModel.getTitle());

        Map<String, String> map = Map.of(
            ID,          String.valueOf(itemModel.getId()),
            TITLE,       itemModel.getTitle(),
            DESCRIPTION, itemModel.getDescription(),
            PRICE,       itemModel.getPrice().toPlainString(),
            IMG,         itemModel.getImgPath(),
            COUNT,       String.valueOf(itemModel.getCount())
        );

        return redis.opsForHash().putAll(key, map)
            .doOnNext(__ -> log.info("► HSET done for {}", key))
            .then(redis.opsForZSet().add(Z_PRICE, String.valueOf(itemModel.getId()), itemModel.getPrice().doubleValue()))
            .then(redis.opsForZSet().add(Z_TITLE, itemModel.getTitle() + KEY_DELIMITER + itemModel.getId(), 0))
            .then();
    }

    public Mono<ItemModel> resetCountValue(long id) {
        String key = key(id);
        return redis.opsForHash()
            .put(key, COUNT, "0")
            .then(findById(id));
    }

    public Mono<ItemModel> increment(long id, long delta) {
        String key = key(id);
        return redis.opsForHash()
            .increment(key, COUNT, delta)
            .flatMap(newCount ->
                redis.opsForHash().put(key, COUNT, newCount.toString()))
            .then(findById(id));
    }

    public Mono<ItemModel> incrementCount(long id) {
        log.debug("Incrementing the count of item. ID: [{}]", id);
        return increment(id, 1L);
    }

    public Mono<ItemModel> decrementCount(long id) {
        log.debug("Decrementing the count of item. ID: [{}]", id);
        return increment(id, -1L);
    }

    public Mono<ItemModel> findById(long id) {
        return redis.<String, String>opsForHash()
            .entries(key(id))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .filter(map -> !map.isEmpty())
            .map(this::toItemModel);
    }

    public Flux<ItemModel> findPage(int page, int size, String search, Sorting sort) {
        Mono<List<Long>> idsMono = switch (sort) {
            case PRICE, NO -> idsByPrice(page, size);
            case ALPHA -> idsByTitle(page, size);
        };
        log.debug("Find by page: Sorting: {}", sort);
        return idsMono.flatMapMany(Flux::fromIterable)
            .flatMap(this::findById)
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
    private String key(long id) {
        return KEY_ITEM.formatted(id);
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

        return itemModel;
    }
}
