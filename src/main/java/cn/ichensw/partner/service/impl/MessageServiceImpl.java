package cn.ichensw.partner.service.impl;

import cn.ichensw.partner.model.domain.Team;
import cn.ichensw.partner.model.domain.User;
import cn.ichensw.partner.model.domain.UserTeam;
import cn.ichensw.partner.model.vo.MessageVO;
import cn.ichensw.partner.service.TeamService;
import cn.ichensw.partner.service.UserService;
import cn.ichensw.partner.service.UserTeamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.ichensw.partner.model.domain.Message;
import cn.ichensw.partner.service.MessageService;
import cn.ichensw.partner.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
* @author zhx
* @description 针对表【message(消息表)】的数据库操作Service实现
* @createDate 2023-05-28 15:29:53
*/
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{

    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;
    /**
     * 获取当前用户的消息列表
     * @param request 当前会话
     * @return List<MessageVO>
     */
    @Override
    public List<MessageVO> listMessages(HttpServletRequest request) {
        List<MessageVO> result = new ArrayList<MessageVO>();
        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getUserId();
        // 查询该用户的消息列表
        // 根据发送人分组，并用发送人的时间进行降序
        List<Message> messageList = this.query()
                .eq("receive_user_id", userId)
                .groupBy("send_user_id")
                .orderByAsc("send_time")
                .list();
        // 查询该用户的所有有参与的房间
        List<UserTeam> userTeamList = userTeamService.query()
                .eq("user_id", userId)
                .list();
        // 遍历每个房间
        // 获取每个房间的ID，并通过id找到对应的每个房间的消息，并通过发送时间进行排序
        userTeamList.forEach(userTeam -> {
            Long teamId = userTeam.getTeamId();
            List<Message> roomMessageList = this.query()
                    .eq("receive_user_id", teamId)
                    .groupBy("receive_user_id")
                    .orderByAsc("send_time")
                    .list();
            // 最后将每个房间对应的消息加入到消息列表中
            messageList.addAll(roomMessageList);
        });
        messageList.forEach(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            // 判断接收方类型是私聊还是群聊，群聊则需要查询 聊天室信息（头像、名称等）
            Integer receiveType = message.getReceiveType();
            if (receiveType == 1) {
                Team team = teamService.getById(message.getReceiveUserId());
                messageVO.setAvatarUrl(team.getTeamImage());
                messageVO.setUserName(team.getName());
            } else {
                // 通过发送方id拿到头像和姓名并赋值
                User user = userService.getById(message.getSendUserId());
                messageVO.setAvatarUrl(user.getAvatarUrl());
                messageVO.setUserName(user.getUsername());
            }
            result.add(messageVO);
        });
        return result;
    }

    @Override
    public List<MessageVO> getUserHistoryMessage(Long fromUserId, Long toUserId) {
        List<MessageVO> result = new ArrayList<>();
        List<Message> messageList = this.query()
                // 要么是别人发的，要么是自己发的
                .and(i -> i.eq("receive_user_id", toUserId).eq("send_user_id", fromUserId))
                .or(i -> i.and(j -> j.eq("receive_user_id", fromUserId).eq("send_user_id", toUserId)))
                .orderByAsc("send_time")
                .list();
        messageList.forEach(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            // 通过发送方id查询头像和姓名
            User sendUserInfo = userService.getById(message.getSendUserId());
            messageVO.setUserName(sendUserInfo.getUsername());
            messageVO.setAvatarUrl(sendUserInfo.getAvatarUrl());
            result.add(messageVO);
        });
        return result;
    }


    @Override
    public List<MessageVO> getRoomHistoryMessage(Long fromUserId, Long toRoomId) {
        List<MessageVO> result = new ArrayList<>();
        // 查询房间号下的所有消息
        List<Message> messageList = this.query()
                .eq("receive_user_id", toRoomId)
                .orderByAsc("send_time")
                .list();

        messageList.forEach(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            // 通过发送方id查询头像和姓名
            User sendUserInfo = userService.getById(message.getSendUserId());
            messageVO.setUserName(sendUserInfo.getUsername());
            messageVO.setAvatarUrl(sendUserInfo.getAvatarUrl());
            result.add(messageVO);
        });
        return result;
    }
}