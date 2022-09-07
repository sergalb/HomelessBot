FROM gradle:7.5.1-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle fatJar --no-daemon

FROM openjdk:17
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/homeless.jar
ENTRYPOINT ["java","-jar","/app/homeless.jar"]