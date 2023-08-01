package cn.ichensw.partner.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class AliyunOSSConfig {
    /**
     * 地域节点
     */
    private String endPoint;
    private String accessKeyId;
    private String accessKeySecret;
    /**
     * OSS的Bucket名称
     */
    private String bucketName;
    /**
     * Bucket 域名
     */
    private String urlPrefix;
    /**
     * 目标文件夹
     */
    private String fileHost;

    /**
     * 将OSS 客户端交给Spring容器托管
     * @return OSS
     */
    @Bean
    public OSS OSSClient() {
        return new OSSClient(endPoint, accessKeyId, accessKeySecret);
    }
}
