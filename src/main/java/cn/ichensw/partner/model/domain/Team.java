package cn.ichensw.partner.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍表
 *
 * @author zhx
 * @TableName team
 */
@TableName(value ="team")
@Data
public class Team implements Serializable {
    /**
     * 队伍ID
     */
    @TableId(type = IdType.AUTO)
    private Long teamId;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍照片
     */
    private String teamImage;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 加入人数
     */
    private Integer joinNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 房间密码
     */
    private String password;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}