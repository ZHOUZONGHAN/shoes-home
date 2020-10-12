package com.zhou.shoehome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.zhou.shoehome.cart.mapper")
public class ShoehomeCartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoehomeCartServiceApplication.class, args);
    }

}
