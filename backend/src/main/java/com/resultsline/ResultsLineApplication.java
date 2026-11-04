package com.resultsline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResultsLineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultsLineApplication.class, args);
    }
}
