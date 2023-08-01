package cn.ichensw.partner.model.request;

import lombok.Data;

import java.io.Serializable;


/**
 * 用户登录请求体
 *
 * @author zhx
 * @date 2022/12/7
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 6121458871274540023L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;


}
