package com.zhou.shoehome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.zhou.shoehome.payment.mapper")
public class ShoehomePaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoehomePaymentServiceApplication.class, args);
    }

}
