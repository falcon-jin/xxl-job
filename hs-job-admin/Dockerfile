FROM openjdk:8-jdk-alpine

ARG JAR_FILE
ADD ${JAR_FILE} /app.jar
EXPOSE 8182
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/urandom -jar /app.jar"]
ENTRYPOINT ["java","-Xms256m", "-Xmx256m", "-Djava.security.egd=file:/dev/./urandom","-Duser.timezone=GMT+08","-jar","app.jar"]
