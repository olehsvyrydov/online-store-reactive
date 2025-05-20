package org.javaprojects.onlinestore.helpers;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Testcontainers
public class RedisTestContainer
{
    @Container
    @ServiceConnection
    public static final RedisContainer redisContainer =
        new RedisContainer(DockerImageName.parse("redis:7.4.2-bookworm"));
}
