package com.webwar.webwar;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.webwar.webwar.modules.**.mapper")
public class WebWarApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebWarApplication.class, args);
    }

}