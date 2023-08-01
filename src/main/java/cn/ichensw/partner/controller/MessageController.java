package cn.ichensw.partner.controller;

import cn.ichensw.partner.common.BaseResponse;
import cn.ichensw.partner.model.vo.MessageVO;
import cn.ichensw.partner.service.MessageService;
import cn.ichensw.partner.service.UserService;
import cn.ichensw.partner.utils.ResultUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 消息接口
 *
 * @author zhx
 */
@RestController
@RequestMapping("/message")
@Slf4j
@Api(tags = "消息接口")
public class MessageController {

    @Resource
    private MessageService messageService;
    @Resource
    private UserService userService;
    /**
     * 查询全部消息列表
     *
     * @return List<TeamUserVO>
     */
    @GetMapping("/list")
    public BaseResponse<List<MessageVO>> listMessages(HttpServletRequest request) {
        // 1. 查询消息列表
        List<MessageVO> teamList = messageService.listMessages(request);
        return ResultUtils.success(teamList);
    }

    /**
     * 查询用户的消息记录
     *
     * @return List<MessageVO>
     */
    @GetMapping("/getUserHistoryMessage")
    public BaseResponse<List<MessageVO>> getUserHistoryMessage(@RequestParam("fromUserId") Long fromUserId,
                                                               @RequestParam("toUserId") Long toUserId, HttpServletRequest request) {
        List<MessageVO> result = messageService.getUserHistoryMessage(fromUserId, toUserId);
        return ResultUtils.success(result);
    }


    /**
     * 查询聊天室的消息列表
     *
     * @return List<MessageVO>
     */
    @GetMapping("/getRoomHistoryMessage")
    public BaseResponse<List<MessageVO>> getRoomHistoryMessage(@RequestParam("fromUserId") Long fromUserId,
                                                               @RequestParam("toRoomId") Long toRoomId, HttpServletRequest request) {
        List<MessageVO> result = messageService.getRoomHistoryMessage(fromUserId, toRoomId);
        return ResultUtils.success(result);
    }
}
