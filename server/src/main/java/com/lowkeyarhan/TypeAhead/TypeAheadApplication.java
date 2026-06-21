package com.lowkeyarhan.TypeAhead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TypeAheadApplication {

	public static void main(String[] args) {
		SpringApplication.run(TypeAheadApplication.class, args);
	}

}
