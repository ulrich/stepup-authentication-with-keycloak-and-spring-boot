# Build Container
FROM maven:3.8.5-openjdk-17-slim as build

WORKDIR /app/

RUN apt-get update && \
	  apt-get install -y --no-install-recommends

COPY pom.xml .
COPY /src src

RUN mvn clean package -DskipTests

# Run Container
FROM amazoncorretto:17

COPY --from=build /app/target/**.jar app.jar
