package com.sulin.codepose.sample.service;

import com.sulin.codepose.sample.App;
import com.sulin.codepose.sample.bean.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest(classes = App.class)
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testCache(){
        List<User> user = userService.getUser(10L);
        System.out.println(user);
    }
}
