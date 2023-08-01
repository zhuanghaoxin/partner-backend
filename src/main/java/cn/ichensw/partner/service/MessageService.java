package cn.ichensw.partner.service;

import cn.ichensw.partner.model.domain.Message;
import cn.ichensw.partner.model.vo.MessageVO;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zhx
* @description 针对表【message(消息表)】的数据库操作Service
* @createDate 2023-05-28 15:29:53
*/

public interface MessageService extends IService<Message> {

    /**
     * 获取当前用户的消息列表
     * @param request 当前会话
     * @return List<MessageVO>
     */
    List<MessageVO> listMessages(HttpServletRequest request);

    /**
     * 根据 发送方id 和 接收方id 获取历史记录
     * @param fromUserId 发送方id
     * @param toUserId 接收方id
     * @return List<MessageVO>
     */
    List<MessageVO> getUserHistoryMessage(Long fromUserId, Long toUserId);

    /**
     * 根据 发送方id 和 房间id 获取历史记录
     * @param fromUserId 发送方id
     * @param toRoomId 房间id
     * @return List<MessageVO>
     */
    List<MessageVO> getRoomHistoryMessage(Long fromUserId, Long toRoomId);
}
