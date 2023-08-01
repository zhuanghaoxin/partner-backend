package cn.ichensw.partner;

import cn.ichensw.partner.ws.ChatRoomWebSocket;
import cn.ichensw.partner.ws.UserChatWebSocket;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author zhx
 */
@SpringBootApplication
@MapperScan("cn.ichensw.partner.mapper")
@EnableScheduling
public class PartnerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PartnerApplication.class, args);
        UserChatWebSocket.setApplicationContext(context);
        ChatRoomWebSocket.setApplicationContext(context);
    }

}
