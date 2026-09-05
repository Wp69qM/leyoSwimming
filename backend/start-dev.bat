@echo off
set SPRING_PROFILES_ACTIVE=local
.\mvnw spring-boot:run > dev.log 2>&1
