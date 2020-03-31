package com.atguigu.gmall;


import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.gmall.user.mapper")
public class GmallUserServiceApplication {



	public static void main(String[] args) {
		SpringApplication.run(GmallUserServiceApplication.class, args);
	}
	@Bean
	@ConfigurationProperties("spring.datasource.druid")
	public DruidDataSource dataSource() {
		return DruidDataSourceBuilder.create().build();
	}


}
