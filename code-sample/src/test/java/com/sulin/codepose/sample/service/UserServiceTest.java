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
    public void testCache() {
        for (int i = 0; i < 10; i++) {
            List<User> user = userService.getUser(10L);
            System.out.println(user);
            if (i == 0) {
                user.get(0).setName("242423432");
                System.out.println("update.....");
            }
        }

    }
}
