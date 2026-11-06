# syntax=docker/dockerfile:1

# Must match the toolchain version in build.gradle.kts
ARG JAVA_VERSION=27

# ---------------------------------------------------------------------------
# Stage 1: build the executable WAR with the Gradle wrapper
# ---------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
WORKDIR /workspace

COPY --chmod=0755 gradlew ./
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./
COPY src/ src/

# The cache mount keeps the Gradle distribution and downloaded dependencies
# between builds. Tests are not run here; run them as their own CI step.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootWar --no-daemon

# Split the WAR into Spring Boot layers so the dependencies (rarely change) and
# the application code (changes every build) become separate image layers.
# --launcher is required for a WAR: the plain extract mode rejects the
# WEB-INF/lib-provided/ folder that holds the embedded Tomcat.
RUN java -Djarmode=tools -jar build/libs/*.war extract --layers --launcher --destination extracted

# ---------------------------------------------------------------------------
# Stage 2: runtime image (JRE only, runs as a non-root user)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre
WORKDIR /app

RUN groupadd --gid 1001 spring \
 && useradd --uid 1001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# Numeric so Kubernetes' runAsNonRoot check can verify it
USER 1001:1001
EXPOSE 8080

# Let the heap use up to 75% of the container's memory limit (JVM default: 25%)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.WarLauncher"]
