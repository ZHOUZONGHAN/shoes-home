package com.zhou.shoehome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.zhou.shoehome.order.mapper")
public class ShoehomeOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoehomeOrderServiceApplication.class, args);
    }

}
