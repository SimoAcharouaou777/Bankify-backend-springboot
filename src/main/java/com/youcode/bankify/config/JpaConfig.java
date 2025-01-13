package com.youcode.bankify.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.youcode.bankify.repository.jpa")
@EnableElasticsearchRepositories(basePackages = "com.youcode.bankify.repository.elasticsearch")
@EntityScan(basePackages = "com.youcode.bankify.entity")
public class JpaConfig {
}
