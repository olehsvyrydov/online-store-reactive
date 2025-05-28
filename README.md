# Online-store
This is a simple site. The project if for educational goals to study Spring Boot. Servlet stack.

# Description
The application is written using the Spring boot (3.4.5 version)
This application can be running docker-compose file


The application works using postgreSQL database and redis.
There are 2 services: `payment-service` and `online-store`.
All settings for it are in `application.yml` file.
For testing purposes, the application also working with an embedded H2 database. I im defined in resources - `application-test.yml` file.

# Build the project
```shell
./gradlew build
docker compose up --build -d
```

You can set up initial **balance** into [docker-compose.yml](docker-compose.yml) file as `INITIAL_BALANCE` environment variable in payment-service

Before you start working with the application, you must update `Client secret` in **keycloak** service.
for that, you should dgo to the browser and open Keycloak admin console:
```text
http://localhost:8080/
```
Then go to online-shop-realm -> Clients -> store-service -> Credentials tab
and regenerate the `Client secret` then put the new value into the [docker-compose.yml](docker-compose.yml) file
as `CLIENT_SECRET` environment variable in `store-service` service.
Then you can restart the application with the command:
```shell
docker compose up -d
```

# Open the link in browser to see the application:
```text
http://localhost:8081/
```

