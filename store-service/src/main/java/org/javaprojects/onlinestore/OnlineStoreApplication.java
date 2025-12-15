package org.javaprojects.onlinestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "org.javaprojects.onlinestore.repositories")
public class OnlineStoreApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(OnlineStoreApplication.class, args);
    }
}
