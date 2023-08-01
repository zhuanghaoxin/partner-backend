package cn.ichensw.partner.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

/**
 * @author zhx
 */
public interface FileUploadService {
    /**
     * 上传文件
     *
     * @param uploadFile 文件
     * @param request 当前会话
     * @return String
     */
    String upload(MultipartFile uploadFile, HttpServletRequest request);

    void download(String fileName, HttpServletResponse response) throws UnsupportedEncodingException;
    void delete(String fileName);
}