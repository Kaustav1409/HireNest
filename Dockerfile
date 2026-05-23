FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/hirenest-backend-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /var/data
ENV PORT=9090
EXPOSE 9090
CMD ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
