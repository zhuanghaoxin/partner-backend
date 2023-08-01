package cn.ichensw.partner.utils;

import java.util.UUID;
public class TokenUtil {
    public static String generateToken() {
        //生成唯一不重复的字符串
        return UUID.randomUUID().toString();
    }
}