FROM eclipse-temurin:21-jdk

ARG ASYNC_PROFILER_VERSION=4.0
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && ARCH=$(dpkg --print-architecture | sed 's/amd64/x64/') \
 && curl -fsSL "https://github.com/async-profiler/async-profiler/releases/download/v${ASYNC_PROFILER_VERSION}/async-profiler-${ASYNC_PROFILER_VERSION}-linux-${ARCH}.tar.gz" \
    -o /tmp/ap.tgz \
 && tar -xzf /tmp/ap.tgz -C /opt \
 && mv "/opt/async-profiler-${ASYNC_PROFILER_VERSION}-linux-${ARCH}" /opt/async-profiler \
 && rm /tmp/ap.tgz \
 && rm -rf /var/lib/apt/lists/*

ENV ASYNC_PROFILER_LIB=/opt/async-profiler/lib/libasyncProfiler.so
WORKDIR /work
