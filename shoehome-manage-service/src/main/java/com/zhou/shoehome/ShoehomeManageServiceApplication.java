package com.zhou.shoehome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.zhou.shoehome.manage.mapper")
public class ShoehomeManageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoehomeManageServiceApplication.class, args);
    }

}
