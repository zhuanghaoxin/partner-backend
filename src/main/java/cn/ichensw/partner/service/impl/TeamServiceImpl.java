package cn.ichensw.partner.service.impl;

import cn.ichensw.partner.common.ErrorCode;
import cn.ichensw.partner.constant.RedisKeyConstant;
import cn.ichensw.partner.exception.BusinessException;
import cn.ichensw.partner.mapper.TeamMapper;
import cn.ichensw.partner.model.domain.Team;
import cn.ichensw.partner.model.domain.User;
import cn.ichensw.partner.model.domain.UserTeam;
import cn.ichensw.partner.model.dto.TeamQuery;
import cn.ichensw.partner.model.enums.TeamStatusEnum;
import cn.ichensw.partner.model.request.TeamJoinRequest;
import cn.ichensw.partner.model.vo.TeamUserVO;
import cn.ichensw.partner.model.vo.UserVO;
import cn.ichensw.partner.service.TeamService;
import cn.ichensw.partner.service.UserService;
import cn.ichensw.partner.service.UserTeamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zhx
 */
@Slf4j
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TeamMapper teamMapper;

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        boolean isAdmin = userService.isAdmin(request);
        if (teamQuery != null) {
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            // 不是管理员 && 查询私有队伍
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }
        // 根据请求参数查询队伍列表
        return getListTeams(teamQuery, request);
    }

    @Nullable
    private List<TeamUserVO> getListTeams(TeamQuery teamQuery, HttpServletRequest request) {
        List<Team> teamList = baseMapper.listByTeamQuery(teamQuery);
        List<TeamUserVO> teamUserVOList = setCreatorUserInfo(teamList);
        teamUserVOList = setTeamJoinStatus(teamUserVOList, request);
        return teamUserVOList;
    }

    /**
     * 设置队伍的加入状态
     *
     * @param teamList 队伍列表
     * @param request  会话状态
     * @return List<TeamUserVO>
     */
    private List<TeamUserVO> setTeamJoinStatus(List<TeamUserVO> teamList, HttpServletRequest request) {
        // final 为了固定ID列表
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getTeamId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(teamIdList)) {
            return null;
        }
        // 2. 判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        userTeamQueryWrapper.eq("user_id", loginUser.getUserId());
        userTeamQueryWrapper.in("team_id", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        // 收集已加入队伍的 id 集合
        Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        teamList.forEach(team -> {
            boolean hasJoin = hasJoinTeamIdSet.contains(team.getTeamId());
            team.setHasJoin(hasJoin);
        });
        return teamList;
    }

    /**
     * 队伍列表封装创建人信息
     *
     * @param teamList 队伍列表
     * @return List<TeamUserVO>
     */
    private List<TeamUserVO> setCreatorUserInfo(List<Team> teamList) {
        // 关联查询创建人的用户信息
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 用户信息脱敏
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 是否登陆
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum <= 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //   2. 队伍标题 <= 20
        String teamName = team.getName();
        if (StringUtils.isNotBlank(teamName) && teamName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        //   3. 队伍描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不满足要求");
        }
        //   4. status是否公开 不传默认为 0 (公开)
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //   5. 如果 status 是加密状态SECRET，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isNotBlank(password) && password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码不满足要求");
            }
        }
        //   6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime == null || new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合法的过期时间");
        }
        //   7. 用户最多创建 5 个队伍
        Long userId = loginUser.getUserId();
        long hasTeamNum = this.count(new QueryWrapper<Team>().eq("user_id", userId));
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        //   8. 插入队伍信息到队伍表
        team.setTeamId(null);
        team.setUserId(userId);
        // 创建队伍：创建人id和队长id一致
        team.setCreatorId(userId);
        boolean result = this.save(team);
        long teamId = Optional.ofNullable(team.getTeamId()).orElse(0L);
        if (!result || teamId <= 0) {
            throw new BusinessException(ErrorCode.INSERT_ERROR, "创建队伍失败");
        }
        //   9. 插入 用户=>队伍关系 到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.INSERT_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    /**
     * 加入队伍是一个极有可能出现并发的操作，所以必须加锁！！！
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        // 加入队伍校验
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(teamJoinRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 获取当前用户ID
        Long userId = loginUser.getUserId();
        // 存入到Redis中的key为：partner:join_team:teamId
        String keyName = RedisKeyConstant.USER_JOIN_TEAM + teamId;
        // 创建读写锁
        RLock lock = redissonClient.getLock(keyName);
        try {
            while (true) {
                // 如果当前线程要获取该锁，0表示 等待时间 -1表示不会自动解锁，单位为ms
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock：" + Thread.currentThread().getId());
//                    log.info("getLock：{}", Thread.currentThread().getId());
                    // 判断当前用户加入了多少队伍
                    long hasJoinTeam = userTeamService.count(new QueryWrapper<UserTeam>().eq("user_id", userId));
                    if (hasJoinTeam > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 是否已经加入了该队伍
                    long hasUserJoinTeam = userTeamService.count(new QueryWrapper<UserTeam>().eq("user_id", userId).eq("team_id", teamId));
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    // 已加入的队伍是否满员(加入数量 >= 最大数量)
                    if (team.getJoinNum() >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满员");
                    }
                    // 修改队伍信息，添加队员
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    userTeamService.save(userTeam);
                    // 增加队伍加入人数
                    return teamMapper.addTeamJoinNum(teamId);
                }
            }
        } catch (Exception e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 如果该锁是当前线程持有的，那就释放，防止误释放
            if (lock.isHeldByCurrentThread()) {
                log.info("unlock：{}", Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Override
    public boolean deleteTeam(long id, User loginUser) {
        // 1. 校验队伍是否存在
        Team team = getTeamById(id);
        Long teamId = team.getTeamId();
        // 2. 校验是否为队长
        if (!team.getUserId().equals(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 3. 移除所有加入队伍的关联信息
        boolean result = userTeamService.remove(new QueryWrapper<UserTeam>().eq("team_id", teamId));
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 4. 删除队伍
        return this.removeById(id);
    }

    @Override
    public boolean quitTeam(Long teamId, User loginUser) {
        Team team = getTeamById(teamId);
        Long userId = loginUser.getUserId();
        // 1. 校验用户是否加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("user_id", userId);
        userTeamQueryWrapper.eq("team_id", teamId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        // 队长调用该接口则说明非法请求，抛出异常。
        if (team.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法请求");
        }
        // 2. 删除关联表数据
        userTeamService.remove(userTeamQueryWrapper);
        // 3. 修改队伍人数
        team.setJoinNum(team.getJoinNum() - 1);
        return this.updateById(team);
    }

    /**
     * 用户加入的聊天室
     * @param teamQuery 查询参数
     * @param request 当前会话
     * @return
     */
    @Override
    public List<TeamUserVO> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        // 根据当前用户获取关联表数据
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getUserId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        if (userTeamList.isEmpty()) {
            return new ArrayList<>();
        }
        // 取出不重复的队伍ID （用队伍ID分组一下）
        List<Long> idList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        teamQuery.setIdList(idList);
        // 条件查询队伍列表，条件为：队伍ID列表
        List<TeamUserVO> teamUserVOList = this.getListTeams(teamQuery, request);
        if (CollectionUtils.isEmpty(teamUserVOList)) {
            return new ArrayList<>();
        }
        // 过滤自身创建的队伍
        teamUserVOList = teamUserVOList.stream().filter(item -> !item.getUserId().equals(loginUser.getUserId())).collect(Collectors.toList());
        return teamUserVOList;
    }

    @Override
    public List<TeamUserVO> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        teamQuery.setCreatorId(loginUser.getUserId());
        List<TeamUserVO> listTeams = this.getListTeams(teamQuery, request);
        if (listTeams == null) {
            return new ArrayList<>();
        }
        return listTeams;
    }


    /**
     * 根据
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }
}




