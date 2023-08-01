package cn.ichensw.partner.service;

import cn.ichensw.partner.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("zhxString", "dog");
        valueOperations.set("zhxInt", 1);
        valueOperations.set("zhxDouble", 2.0);
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhx");
        valueOperations.set("zhxUser", user);
        // 查
        Object zhx = valueOperations.get("zhxString");
        Assertions.assertTrue("dog".equals((String) zhx));
        zhx = valueOperations.get("zhxInt");
        Assertions.assertTrue(1 == (Integer) zhx);
        zhx = valueOperations.get("zhxDouble");
        Assertions.assertTrue(2.0 == (Double) zhx);
        System.out.println(valueOperations.get("zhxUser"));
        valueOperations.set("zhxString", "dog");
        redisTemplate.delete("zhxString");
    }
}
