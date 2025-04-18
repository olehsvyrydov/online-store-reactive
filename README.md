# Online-store
This is a simple site. The project if for educational goals to study Spring Boot. Servlet stack.

# Description
The application is written using the Spring boot (3.4.4 version)
This application can be running included in web starter servlet container. I used the Jetty.


The application works using H2 in memory database.
All settings for it are in `application.yml` file.
For testing purposes, the application also working with an embedded H2 database. I is defined in resources - `application-test.yml` file.

# Build the project
```shell
./gradlew build
```

Then find an executable file in ./build/lib folder

# Run application
```shell
./build/libs/online-store
```