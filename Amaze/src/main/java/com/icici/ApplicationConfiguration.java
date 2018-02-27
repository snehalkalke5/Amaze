package com.icici;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.mongodb.MongoClient;
@Configuration
@PropertySource("classpath:config.properties")
@EnableWebMvc
@ComponentScan(basePackages = "com.icici")
@EnableMongoRepositories(basePackages = "com.icici")
public class ApplicationConfiguration {
	
	@Autowired
	private Environment env;
	
	@Bean
    public MongoDbFactory mongoDbFactory() throws Exception {
        MongoClient mongoClient = new MongoClient(env.getProperty("host"), Integer.parseInt(env.getProperty("port")));
        return new SimpleMongoDbFactory(mongoClient, env.getProperty("database"));
    }
 
    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory());
        return mongoTemplate;
    }
 
}
