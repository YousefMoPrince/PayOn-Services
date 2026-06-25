package com.jo.payonservices;

import org.springframework.boot.SpringApplication;

public class TestPayOnServicesApplication {

    public static void main(String[] args) {
        SpringApplication.from(PayOnServicesApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
