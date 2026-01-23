package com.dronefleet.statemanager.infrastructure.adapter.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

  private int valueTest = 21;

  @GetMapping("/sample")
  public String getSampleTest(@RequestParam(value = "input", defaultValue = "World") String inputText) {

    return String.format("It just works with %d.", inputText);
  }

  public int testingInput() {
    System.out.println(String.format("Current number : %n", valueTest));
    return valueTest;
  }

  public int getValueTest() {
    return this.valueTest;
  }

}
