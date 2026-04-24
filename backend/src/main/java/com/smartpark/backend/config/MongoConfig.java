package com.smartpark.backend.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public GridFSBucket gridFSBucket(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        return GridFSBuckets.create(database);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
