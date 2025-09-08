FROM openjdk:17-alpine
ARG JAR_FILE=build/libs/windoorman-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Seoul","/app.jar"]