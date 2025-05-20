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

# Open the link in browser:
```text
http://localhost:8081/
```
