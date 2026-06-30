package org.training.sequence.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class SequenceGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SequenceGeneratorApplication.class, args);
    }

}
