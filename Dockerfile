FROM maven:3.8.3-openjdk-17

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .

CMD ["mkdir", "/app/storage"]

ENV PHOTO_STORAGE=/app/storage

CMD ["java", "-jar", "target/BLPS_Lab4_Standalone-0.0.1-SNAPSHOT.jar"]