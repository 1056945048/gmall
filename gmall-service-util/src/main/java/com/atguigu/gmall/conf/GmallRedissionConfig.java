package com.atguigu.gmall.conf;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yangkun
 * @date 2020/2/29
 */
@Configuration
public class GmallRedissionConfig {
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private String port;
    @Value("${spring.redis.password}")
    private String auth;
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port).setPassword(auth);
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
