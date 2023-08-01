package cn.ichensw.partner.mapper;

import cn.ichensw.partner.model.domain.Team;
import cn.ichensw.partner.model.dto.TeamQuery;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @author zhx
 */
public interface TeamMapper extends BaseMapper<Team> {

    /**
     * 查询队伍列表
     *
     * @param teamQuery 查询关键字
     * @return List<Team>
     */
    List<Team> listByTeamQuery(TeamQuery teamQuery);

    /**
     * 增加队伍加入人数
     *
     * @param teamId 队伍ID
     * @return 是否成功
     */
    boolean addTeamJoinNum(Long teamId);
}




