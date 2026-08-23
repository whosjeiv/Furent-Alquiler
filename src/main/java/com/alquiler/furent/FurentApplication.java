package com.alquiler.furent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
		DataRedisAutoConfiguration.class,
		DataRedisRepositoriesAutoConfiguration.class
})
@ConfigurationPropertiesScan
@EnableAsync
@EnableCaching
@EnableScheduling
public class FurentApplication {

	public static void main(String[] args) {
		SpringApplication.run(FurentApplication.class, args);
	}

}
