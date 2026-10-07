package com.optibrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OptibrainApplication {

	public static void main(String[] args) {
		SpringApplication.run(OptibrainApplication.class, args);
	}

}
