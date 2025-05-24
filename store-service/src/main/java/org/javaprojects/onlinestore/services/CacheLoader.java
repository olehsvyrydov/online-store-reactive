/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.entities.Cart;
import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.repositories.CartRepository;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

@Component
public class CacheLoader
{
    private static final String KEY_ITEM        = "item:%d";
    private static final String KEY_CART_COUNT  = "count:%d:%d";
    private static final String ID              = "id";
    private static final String TITLE           = "title";
    private static final String DESCRIPTION     = "description";
    private static final String PRICE           = "price";
    private static final String IMG             = "img";
    private static final String Z_PRICE         = "z:price";
    private static final String Z_TITLE         = "z:title";
    public static final char KEY_DELIMITER      = '|';

    private ReactiveRedisTemplate<String, String> redis;
    private ItemsRepository itemsRepository;
    private CartRepository cartRepository;

    private static final Logger log = LoggerFactory.getLogger(CacheLoader.class.getName());

    public CacheLoader(ReactiveRedisTemplate<String, String> redis,
        ItemsRepository itemsRepository, CartRepository cartRepository)
    {
        this.redis = redis;
        this.itemsRepository = itemsRepository;
        this.cartRepository = cartRepository;
    }

    public Mono<String> getQuantity(long itemId, long userId) {
        String key = cartCountKey(itemId, userId);
        return cartRepository.findByItemIdAndUserId(itemId, userId)
            .switchIfEmpty(Mono.just(new Cart(itemId, userId, 0L)))
            .flatMap(cart -> redis.opsForValue().set(key, String.valueOf(cart.getQuantity())))
            .then(redis.opsForValue().get(key))
            .doOnNext(count -> log.info("Item count loaded from the cache. ID: {}, User ID: {}, Count: {}",
                itemId, userId, count));
    }

    public Mono<Map<String, String>> loadItem(long itemId) {
        var key = itemKey(itemId);
        log.debug("Loading item from cache. Key: {}, itemId: {}", key, itemId);
        return itemsRepository.findById(itemId)
            .switchIfEmpty(Mono.error(new IllegalStateException("Item not found by id " + itemId)))
            .flatMap(item -> {
                Map<String, String> map = Map.of(
                    ID,          String.valueOf(item.getId()),
                    TITLE,       item.getTitle(),
                    DESCRIPTION, item.getDescription(),
                    PRICE,       item.getPrice().toPlainString(),
                    IMG,         item.getImgPath()
                );
                return
                    redis.opsForZSet().add(Z_PRICE, String.valueOf(item.getId()), item.getPrice().doubleValue())
                        .then(redis.opsForZSet().add(Z_TITLE, item.getTitle() + KEY_DELIMITER + item.getId(), 0))
                        .then(redis.<String, String>opsForHash().putAll(key, map))
                        .then(redis.<String, String>opsForHash().entries(key)
                        .collectMap(Map.Entry::getKey, Map.Entry::getValue));
            });
    }

    public Mono<List<Long>> loadPages(int page, int size, String searchString, Sorting sorting)
    {
        Sort sorted = switch (sorting) {
            case NO -> Sort.unsorted();
            case PRICE -> Sort.by("price").ascending();
            case ALPHA -> Sort.by("title").ascending();
        };
        Pageable pageable = PageRequest.of(page, size, sorted);
        return (searchString.isBlank()
                    ? itemsRepository.findBy(pageable)
                    : itemsRepository.findBySearchString(searchString, pageable))
            .flatMap(this::save)
            .collectList();
    }

    public Mono<Long> save(Item item) {
        String key = itemKey(item.getId());
        log.debug("saving item to cache. Key: {}, Title: {}", key, item.getTitle());

        Map<String, String> map = Map.of(
            ID,          String.valueOf(item.getId()),
            TITLE,       item.getTitle(),
            DESCRIPTION, item.getDescription(),
            PRICE,       item.getPrice().toPlainString(),
            IMG,         item.getImgPath()
        );

        return redis.opsForHash().putAll(key, map)
            .doOnNext(__ -> log.info("► HSET done for {}", key))
            .then(redis.opsForZSet().add(Z_PRICE, String.valueOf(item.getId()), item.getPrice().doubleValue()))
            .then(redis.opsForZSet().add(Z_TITLE, item.getTitle() + KEY_DELIMITER + item.getId(), 0))
            .thenReturn(item.getId());
    }

    private String itemKey(long itemId) {
        return KEY_ITEM.formatted(itemId);
    }

    private String cartCountKey(long itemId, long userId) {
        return KEY_CART_COUNT.formatted(itemId, userId);
    }

    public Mono<Cart> updateItemCount(long id, long userId, Long newValue)
    {
        log.debug("Updating item count in the cart. ID: [{}], User ID: [{}], Count: [{}]", id, userId, newValue);
        return cartRepository.findByItemIdAndUserId(id, userId)
            .switchIfEmpty(Mono.defer(() -> {
                log.info("Creating new cart item. ID: {}, User ID: {}, Count: {}", id, userId, 0L);
                return cartRepository.insertToCart(id, userId, 0L)
                    .then(Mono.just(new Cart(id, userId, 0L)))
                    .doOnNext(cart -> log.info("Created new cart item. ID: {}, User ID: {}, Count: {}",
                        id, userId, newValue));
            }))
            .flatMap(cart -> {
                cart.setQuantity(newValue);
                return cartRepository.save(cart);
            })
            .doOnNext(cart -> log.info("Updated item count in the cart. ID: {}, User ID: {}, Count: {}",
                id, userId, newValue));
    }
}
