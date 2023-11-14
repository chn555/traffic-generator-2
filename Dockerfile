FROM --platform=linux/amd64 amazoncorretto:17


RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

RUN curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

COPY build/libs/ti_cookie_cutter.jar /usr/src/app

ENTRYPOINT ["java", "-javaagent:opentelemetry-javaagent.jar","-jar", "-Dspring.config.name=application-prod", "ti_cookie_cutter.jar"]

