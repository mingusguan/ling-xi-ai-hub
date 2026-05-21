FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -pl lingxi-admin -am clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 lingxi \
    && useradd --system --uid 10001 --gid lingxi --home-dir /app lingxi \
    && mkdir -p /app/logs /app/uploadPath \
    && chown -R lingxi:lingxi /app

COPY --from=build /workspace/lingxi-admin/target/lingxi-admin.jar /app/lingxi-admin.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/lingxi-admin.jar"]
