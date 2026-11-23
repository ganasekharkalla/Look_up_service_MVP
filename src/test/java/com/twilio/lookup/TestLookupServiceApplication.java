package com.twilio.lookup;

import org.springframework.boot.SpringApplication;

public class TestLookupServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(LookupServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
