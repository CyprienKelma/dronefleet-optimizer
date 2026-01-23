package com.dronefleet.statemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@RestController
public class StateManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StateManagerApplication.class, args);
	}
}
