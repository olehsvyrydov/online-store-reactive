package org.javaprojects.onlinestore.configurations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;


@Configuration
public class ApplicationConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory
        , ObjectMapper objectMapper) {
        RedisSerializer<Object> serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Object> context = builder
            .value(serializer)
            .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper objectMapper) {
        RedisSerializer<Object> json = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisSerializationContext.SerializationPair<Object> pair =
            RedisSerializationContext.SerializationPair.fromSerializer(json);

        RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(pair)
            .entryTtl(Duration.ofSeconds(60));

        return RedisCacheManager.builder(cf).cacheDefaults(cfg).build();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        return builder -> builder
            .withCacheConfiguration("items",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            new GenericJackson2JsonRedisSerializer(objectMapper))
            ))
            .withCacheConfiguration("allItems",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(1))
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            new GenericJackson2JsonRedisSerializer(objectMapper))
            ))
            .withCacheConfiguration("itemsSize",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            new GenericJackson2JsonRedisSerializer(objectMapper))
            ));
    }

}
