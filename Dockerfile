FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/pedestal.sqs-0.0.1-SNAPSHOT-standalone.jar /pedestal.sqs/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/pedestal.sqs/app.jar"]
