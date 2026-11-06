package com.mjrenew.mjrenew_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MjrenewBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MjrenewBackendApplication.class, args);
	}

}