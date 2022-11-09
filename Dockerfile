#FROM amd64/gradle:7.5.1-jdk17 AS build
#COPY --chown=gradle:gradle . /home/gradle/src
#WORKDIR /home/gradle/src
#RUN gradle --no-daemon fatJar --info --stacktrace

FROM openjdk:17
RUN mkdir /app
COPY build/libs/homeless-1.0-SNAPSHOT-fat.jar /app/homeless.jar
ENTRYPOINT ["java","-jar","/app/homeless.jar"]