package com.wordpress.kkaravitis.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TransferServiceApplication {

   public static void main(String[] args) {
       SpringApplication.run(TransferServiceApplication.class);
   }

}
