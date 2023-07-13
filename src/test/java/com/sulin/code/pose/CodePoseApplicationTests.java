package com.sulin.code.pose;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
class CodePoseApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisTemplate redisTemplate2;

    @Test
    void contextLoads() {

//        redisTemplate.opsForValue().set("demo", new Student("sulin", 26));
        ValueOperations<String,Student> valueOperations = redisTemplate2.opsForValue();
        Student demo = valueOperations.get("demo");
        System.out.println(demo);
    }

    @AllArgsConstructor
    @Data
    public static class Student {
        private String name;
        private Integer age;

    }

}
