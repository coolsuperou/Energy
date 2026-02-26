package org.example.back;

import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;
import org.example.back.mapper.common.UserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class StreamPracticeTest {


    @Autowired
    private UserMapper userMapper;

    @Test
    void testStreamPractice() {
List<User> users = userMapper.selectList(null);
        System.out.println(users);



    }



}
