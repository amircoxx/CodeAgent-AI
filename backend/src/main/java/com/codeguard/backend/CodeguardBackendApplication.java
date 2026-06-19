package com.codeguard.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeguardBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(CodeguardBackendApplication.class, args);
  }
}
