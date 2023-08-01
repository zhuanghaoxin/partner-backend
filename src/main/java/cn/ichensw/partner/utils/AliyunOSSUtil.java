package cn.ichensw.partner.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

/**
 * 阿里云OSS工具类
 *
 * @author ruoyi
 */
public class AliyunOSSUtil {

    public static OSS initOSS(String endpoint, String accessKeyId, String accessKeySecret) {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

}
