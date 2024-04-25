package com.atguigu.cloud;

import java.time.ZonedDateTime;

/**
 * @ClassName Main
 * @Description
 * @Author GateOfRust
 * @Date 2024/4/19 下午4:25
 * @Version 1.0
 */

public class Main {
    public static void main(String[] args) {
        ZonedDateTime zbj = ZonedDateTime.now(); // 默认时区
        System.out.println(zbj);
    }
}
