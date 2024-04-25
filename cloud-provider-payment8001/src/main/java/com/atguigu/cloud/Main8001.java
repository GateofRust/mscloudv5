package com.atguigu.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @ClassName Main8001
 * @Description
 * @Author GateOfRust
 * @Date 2024/4/14 下午4:14
 * @Version 1.0
 */

@SpringBootApplication
//相当于统一进行扫描，不需要额外添加@mapper
@MapperScan("com.atguigu.cloud.mapper")
@EnableDiscoveryClient
@RefreshScope //动态刷新
public class Main8001 {
    public static void main(String[] args) {
        SpringApplication.run(Main8001.class, args);
    }
}
