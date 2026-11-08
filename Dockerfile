# Production image for Back4app Containers, which deploys every commit to this branch.
# Don't edit by hand: the CD workflow (.github/workflows/cd.yml on master) rewrites the FROM
# line with each version that already passed staging.
FROM ghcr.io/se-mahmoudabdelaal/ci-cd-demo:latest

# Fit Back4app's free instance (256 MB RAM, 0.25 CPU): a smaller heap and code cache, the
# serial garbage collector, the quick C1 compiler only, and fewer Tomcat threads.
# Tested locally with those limits: ready in about 25 seconds, using about 150 MB.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=128m -XX:TieredStopAtLevel=1" \
    SERVER_TOMCAT_THREADS_MAX=20

EXPOSE 8080
