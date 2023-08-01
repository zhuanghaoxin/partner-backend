package cn.ichensw.partner.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户退出队伍请求体
 *
 * @author zhx
 * @date 2023/4/7 13:09
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = 3436788570307585362L;

    /**
     * 队伍 id
     */
    private Long teamId;
}
