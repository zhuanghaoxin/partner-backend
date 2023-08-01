package cn.ichensw.partner.service;

import cn.ichensw.partner.model.domain.User;
import cn.ichensw.partner.model.request.UserTagAddRequest;
import cn.ichensw.partner.model.request.UserTagRemoveRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * @author zhx
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2022-12-06 19:37:03
 */
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 二次校验密码
     * @return 注册结果
     */
    Boolean userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  账户
     * @param userPassword 密码
     * @param request      请求处理器
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户数据脱敏
     *
     * @param originUser 完整的User
     * @return 脱敏后的User
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request
     * @return 结果
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 标签列表
     * @param username
     * @return 用户列表
     */
    Page<User> searchUsersByTags(List<String> tagNameList, String username, long pageSize, long pageNum);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 标签列表
     * @return 用户列表
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 修改用户信息
     *
     * @param user      用户信息
     * @param loginUser
     * @return 结果
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户信息
     *
     * @param request 会话信息
     * @return 用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param loginUser 当前用户
     * @return boolean
     */
    boolean isAdmin(User loginUser);

    /**
     * 推荐相似用户（未实现）
     *
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @param request 当前会话
     * @return Page<User>
     */
    List<User> recommendUsers(long pageSize, long pageNum, HttpServletRequest request);

    /**
     * 匹配相似用户
     * @param num 匹配人数
     * @param loginUser 当前用户
     * @return List<User>
     */
    List<User> matchUsers(long num, User loginUser) throws IOException;

    /**
     * 根据id查询用户
     * @param userId 用户id
     * @return User
     */
    User getUserById(Integer userId);

    /**
     * 添加用户标签
     *
     * @param request 请求参数
     * @return Integer
     */
    Boolean addTag(UserTagAddRequest request);

    /**
     * 修改用户标签
     *
     * @param request 请求参数
     * @return Integer
     */
    Boolean removeTag(UserTagRemoveRequest request);
}
