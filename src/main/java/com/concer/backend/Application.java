package com.concer.backend;



import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan({
		"com.concer.backend.events.MyBatisPlus",
		"com.concer.backend.area.MyBatisPlus",
		"com.concer.backend.users.MyBatisPlus",
		"com.concer.backend.orders.MyBatisPlus"}) // 這裡填入你所有 Mapper 介面的資料夾路徑
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);


	}

}
