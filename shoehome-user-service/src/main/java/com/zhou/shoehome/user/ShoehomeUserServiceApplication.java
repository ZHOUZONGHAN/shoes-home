package com.zhou.shoehome.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.zhou.shoehome.user.mapper")
public class ShoehomeUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoehomeUserServiceApplication.class, args);
    }

}
