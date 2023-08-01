package cn.ichensw.partner.service.impl;

import cn.hutool.core.lang.Pair;
import cn.ichensw.partner.common.ErrorCode;
import cn.ichensw.partner.exception.BusinessException;
import cn.ichensw.partner.mapper.UserMapper;
import cn.ichensw.partner.model.domain.User;
import cn.ichensw.partner.model.request.UserTagAddRequest;
import cn.ichensw.partner.model.request.UserTagRemoveRequest;
import cn.ichensw.partner.service.UserService;
import cn.ichensw.partner.utils.AlgorithmUtils;
import cn.ichensw.partner.utils.RedisUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.ichensw.partner.constant.UserConstant.ADMIN_ROLE;
import static cn.ichensw.partner.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zhx
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {


    /**
     * 盐值，混淆密码用
     */
    private static final String SALT = "jucce";


    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 二次校验密码
     * @return 注册结果
     */
    @Override
    public Boolean userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号小于4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码小于8位");
        }
        // 账号不能包含特殊字符
        String validPattern = ".*[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；： ”“’。，、？\\\\]+.*";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        // 密码和校验密码不相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和重复密码不相同");
        }
        // 账户不能重复
        long count = userMapper.selectCount(new QueryWrapper<User>().eq("user_account", userAccount));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户不能重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setTags("[\"男\"]");
        // 默认用户名和账号相同
        user.setUsername(userAccount);
        // 默认头像
        user.setAvatarUrl("https://image-bed-ichensw.oss-cn-hangzhou.aliyuncs.com/006VVqOWgy1h3yp0mg9uwj30u00u0gpx.jpg");
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.INSERT_ERROR);
        }

        return saveResult;
    }

    /**
     * 用户登录
     *
     * @param userAccount  账户
     * @param userPassword 密码
     * @param request      请求处理器
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验账号和密码
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码小于8位");
        }
        // 账号不能包含特殊字符
        String validPattern = ".*[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；： ”“’。，、？\\\\]+.*";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 2. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Long start = System.currentTimeMillis();
        queryWrapper.eq("user_account", userAccount);
        User user = userMapper.selectOne(queryWrapper);
        Long end = System.currentTimeMillis();
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot find");
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }

        queryWrapper.eq("user_password", encryptPassword);
        user = userMapper.selectOne(queryWrapper);
        // 密码错误
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户密码错误");
        }

        // 3. 用户数据脱敏
        User safeUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safeUser);
        log.info("耗时：{}ms", (end - start));
        return safeUser;
    }

    /**
     * 用户数据脱敏
     *
     * @param originUser 完整的User
     * @return 脱敏后的User
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safeUser = new User();
        safeUser.setUserId(originUser.getUserId());
        safeUser.setUsername(originUser.getUsername());
        safeUser.setUserAccount(originUser.getUserAccount());
        safeUser.setAvatarUrl(originUser.getAvatarUrl());
        safeUser.setGender(originUser.getGender());
        safeUser.setPhone(originUser.getPhone());
        safeUser.setEmail(originUser.getEmail());
        safeUser.setProfile(originUser.getProfile());
        safeUser.setUserRole(originUser.getUserRole());
        safeUser.setUserStatus(originUser.getUserStatus());
        safeUser.setCreateTime(originUser.getCreateTime());
        safeUser.setTags(originUser.getTags());
        return safeUser;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户 (SQL分页查询版)
     *
     * @param tagNameList 用户所拥有的的标签
     * @param username
     * @return
     */
    @Override
    public Page<User> searchUsersByTags(List<String> tagNameList, String username, long pageSize, long pageNum) {
        if (StringUtils.isBlank(username) && CollectionUtils.isEmpty(tagNameList)) {
            return new Page<>();
        }
        // SQL查询方式
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(tagNameList)) {
            // 拼接 and 查询
            for (String tagName : tagNameList) {
                qw = qw.like("tags", tagName);
            }
        }
        if (!StringUtils.isBlank(username)) {
            qw.like("username", username);
        }
        Page<User> userPage = this.page(new Page<>(pageNum, pageSize), qw);
        List<User> collect = userPage.getRecords().stream().map(this::getSafetyUser).collect(Collectors.toList());
        userPage.setRecords(collect);
        return userPage;
    }

    /**
     * 根据标签搜索用户 (内存过滤版)
     *
     * @param tagNameList 用户所拥有的的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 根据一个或多个tag查询用户 (两种方式各有优劣，需要根据业务来选择)
        // 2. 内存查询方式
        List<User> userList = userMapper.selectList(null);
        userList = userList.stream().filter(user -> {
            String userTags = user.getTags();
            Set<String> tempTagNameList = JSON.parseObject(userTags, new TypeReference<Set<String>>() {
            });
            tempTagNameList = Optional.ofNullable(tempTagNameList).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameList.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

        return userList;
    }

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @param loginUser
     * @return 结果
     */
    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getUserId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 不是管理员/本人，则报错
        if (!isAdmin(loginUser) && userId != loginUser.getUserId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 管理员/本人，则更新
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request 会话信息
     * @return 用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        // 是否为管理员
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 首页用户推荐
     *
     * @param pageSize 分页大小
     * @param pageNum  页码
     * @param request  当前会话
     * @return
     */
    @Override
    public List<User> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
            /*
            select * from orders_history where type=2
            and id between 1000000 and 1000100 limit 100;
             */
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.between("user_id", pageSize * pageNum, (pageSize * pageNum) + pageSize);
        userQueryWrapper.last("limit " + pageSize);
        return this.list(userQueryWrapper);
    }

    /**
     * 用户匹配
     * @param num 匹配人数
     * @param loginUser 当前用户
     * @return
     * @throws IOException
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) throws IOException {
        // 从数据库中筛选出id、tags列，并且tags不为空
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list();
        // 获取该请求的用户的标签（String）
        String tags = loginUser.getTags();

        if (StringUtils.isBlank(tags)) {
            // 如果用户的标签为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户标签为空");
        }
        // 由于tags在数据库存储是JSON格式，因此使用json进行转换
        // 获取到请求用户的标签列表（List）
        List<String> tagList = JSON.parseObject(tags, new TypeReference<List<String>>(){});
        // 集合<集合<用户数据, 相似度>>

        List<Pair<User, Double>> list = new ArrayList<>();
        for (User user : userList) {
            String userTags = user.getTags();
            // 排除空标签 和 自己的标签
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getUserId(), loginUser.getUserId())) {
                continue;
            }
            // 同样提取出其他用户的标签
            List<String> userTagList = JSON.parseObject(userTags, new TypeReference<List<String>>() {});
            // 相似度匹配算法执行
            double distance = AlgorithmUtils.sorce(tagList, userTagList);
            // 将当前用户与其他用户的匹配结果记录到
            list.add(new Pair<>(user, distance));
        }
        // 按照计算出来的相似度 由大到小排序
        List<Pair<User, Double>> topUserPairList = list.stream().
                sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 取出用户 id
        List<Long> userIdList = topUserPairList.stream()
                .map(pair -> pair.getKey().getUserId())
                .collect(Collectors.toList());
        // 通过用户 id 查询详细数据
        queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIdList);
        // 查询前顺序 1 3 2
        // 查询后顺序 1 2 3
        // 通过Map<id, user>记录原本顺序，创建新集合返回即可
        Map<Long, List<User>> userIdListMap = this.list(queryWrapper).stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getUserId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdListMap.get(userId).get(0));
        }
        return finalUserList;
    }

    @Override
    public User getUserById(Integer userId) {
        return this.getById(userId);
    }

    @Override
    public Boolean removeTag(UserTagRemoveRequest request) {
        List<String> oldTags = request.getOldTags();
        List<String> tagsList = new ArrayList<>();
        // 遍历oldTags，如果有相同的tag，则不添加
        for (String oldTag : oldTags) {
            if (!oldTag.equals(request.getTag())) {
                tagsList.add(oldTag);
            }
        }
        String newTags = JSON.toJSONString(tagsList);

        User user = new User();
        user.setUserId(request.getUserId());
        user.setTags(newTags);
        return this.updateById(user);
    }

    @Override
    public Boolean addTag(UserTagAddRequest request) {
        User user = this.getById(request.getUserId());
        String tags = user.getTags();
        // 更新JSON格式的tag标签
        JSONArray tagsArr = JSON.parseArray(tags);
        tagsArr.add(request.getTag());
        user.setTags(tagsArr.toJSONString());
        return this.updateById(user);
    }

    /**
     * 首页用户推荐
     *
     * @param pageSize 分页大小
     * @param pageNum  页码
     * @param request  当前会话
     * @return
     *//*
    @Override
    public Page<User> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        // 获取当前用户
        User loginUser = this.getLoginUser(request);
        String redisKey = String.format("awei:user:recommond:%s", loginUser.getUserId());
        Page<User> userPage = (Page<User>) redisUtils.get(redisKey);
        // 如果有缓存，直接读缓存
        if (userPage != null) {
            return userPage;
        }
        // 无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        // 写缓存
        try {
            // 固定缓存到期时间，可能会造成缓存雪崩
            redisUtils.set(redisKey, userPage, 3000 + new Random().nextInt(600));
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return userPage;
    }*/


    /**
     * 根据标签搜索用户 (SQL查询版)
     *
     * @param tagNameList 用户所拥有的的标签
     * @return
     */
    @Deprecated
    private List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 根据一个或多个tag查询用户 (两种方式各有优劣，需要根据业务来选择)
        // 1. SQL查询方式
        QueryWrapper<User> qw = new QueryWrapper<>();
        // 拼接 and 查询
        for (String tagName : tagNameList) {
            qw = qw.like("tags", tagName);
        }
        return userMapper.selectList(qw).stream().map(this::getSafetyUser).collect(Collectors.toList());
    }
}




