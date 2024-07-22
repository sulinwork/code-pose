package com.sulin.codepose.sample.service;

import com.sulin.codepose.sample.bean.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    @Cacheable(cacheManager = "customerCaffeineCacheManager", cacheNames = "USER", key = "#userId")
    public List<User> getUser(Long userId) {
        System.out.println("=====getUser======");
        return Arrays.asList(new User().setUserId(userId).setName("sulin"));
    }
}
