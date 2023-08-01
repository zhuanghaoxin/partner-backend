package cn.ichensw.partner.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍修改请求体
 *
 * @author zhx
 */
@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = 5936345271727135668L;
    /**
     * id
     */
    private Long teamId;

    /**
     * 队伍照片
     */
    private String teamImage;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;
}
