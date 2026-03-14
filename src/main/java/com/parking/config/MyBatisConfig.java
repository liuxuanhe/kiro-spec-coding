package com.parking.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.parking.mapper")
public class MyBatisConfig {
}
