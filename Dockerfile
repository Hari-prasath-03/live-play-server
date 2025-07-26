# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper files first
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml .

# Make wrapper executable
RUN chmod +x mvnw

# Now copy the rest (source code)
COPY src src

# Package the application
RUN ./mvnw -B -DskipTests clean package

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=builder /app/target/LivePlay-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
