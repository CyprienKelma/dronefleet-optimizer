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

	@GetMapping("/test")
	public String getStateManager(@RequestParam(value = "input", defaultValue = "default_value") String inputText) {
		return String.format("It just works with %s.", inputText);
	}
}
