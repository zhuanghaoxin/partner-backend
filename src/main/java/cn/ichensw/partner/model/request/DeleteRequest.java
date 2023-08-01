package cn.ichensw.partner.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求
 *
 * @author zhx
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -5850245444195057623L;

    private long id;
}
