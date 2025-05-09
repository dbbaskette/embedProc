# Quick Reference

## Required Dependencies
- `spring-boot-starter-web`: Required for embedding with Spring AI Ollama integration (provides `RestClient.Builder` bean)

## Common Startup Errors
- **Missing RestClient.Builder**: Add `spring-boot-starter-web` to your dependencies if you see UnsatisfiedDependencyException for `RestClient.Builder`.

## Typical pom.xml snippet
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
