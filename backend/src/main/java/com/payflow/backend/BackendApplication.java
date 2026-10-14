package com.payflow.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        String rawDbUrl = System.getProperty("DATABASE_URL");
        if (rawDbUrl != null) {
            String cleanDbUrl = rawDbUrl.trim();
            if (cleanDbUrl.startsWith("jdbc:")) {
                cleanDbUrl = cleanDbUrl.substring(5);
            }
            try {
                java.net.URI uri = java.net.URI.create(cleanDbUrl.replace("postgresql:", "http:"));
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    System.setProperty("DATABASE_USERNAME", parts[0]);
                    System.setProperty("DATABASE_PASSWORD", parts[1]);
                }
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath() != null ? uri.getPath() : "";
                String query = uri.getQuery();
                if (query != null) {
                    query = query.replace("&channel_binding=require", "")
                                 .replace("channel_binding=require&", "")
                                 .replace("channel_binding=require", "");
                }
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null && !query.isEmpty() ? "?" + query : "");
                System.setProperty("DATABASE_URL", jdbcUrl);
            } catch (Exception e) {
                if (rawDbUrl.startsWith("postgresql://")) {
                    System.setProperty("DATABASE_URL", "jdbc:" + rawDbUrl);
                }
            }
        }

        String rawRedisUrl = System.getProperty("REDIS_URL");
        if (rawRedisUrl != null) {
            String redisUrl = rawRedisUrl.trim();
            if (redisUrl.startsWith("redis-cli")) {
                int uIdx = redisUrl.indexOf("-u ");
                if (uIdx != -1) {
                    redisUrl = redisUrl.substring(uIdx + 3).trim();
                }
            }
            if (redisUrl.startsWith("redis://") && (redisUrl.contains("upstash.io") || redisUrl.contains(".upstash."))) {
                redisUrl = "rediss://" + redisUrl.substring("redis://".length());
            }
            System.setProperty("REDIS_URL", redisUrl);
        }

        System.out.println("PayFlow backend starting... (Database & Redis configured)");
		SpringApplication.run(BackendApplication.class, args);
	}
}
