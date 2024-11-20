package com.spacepeople;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SpacepeopleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpacepeopleApplication.class, args);
	}

}
