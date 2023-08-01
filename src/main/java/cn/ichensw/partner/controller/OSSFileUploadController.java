package cn.ichensw.partner.controller;

import cn.ichensw.partner.common.BaseResponse;
import cn.ichensw.partner.service.FileUploadService;
import cn.ichensw.partner.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * OSS 文件上传和下载
 *
 * @author zhx
 * @date 2023/4/22
 */
@RestController
@RequestMapping("/oss")
@Slf4j
public class OSSFileUploadController {

    @Resource
    private FileUploadService fileUploadService;

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public BaseResponse<Map<String, Object>> uploadFile(@RequestBody MultipartFile file, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>(5);
        // 上传并返回新文件名称
        String imageUrl = fileUploadService.upload(file,request);
        result.put("url", imageUrl);
        return ResultUtils.success(result);
    }

    /**
     * 通用上传请求（多个）
     *//*
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception {
        try {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files) {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls" , StringUtils.join(urls, FILE_DELIMITER));
            ajax.put("fileNames" , StringUtils.join(fileNames, FILE_DELIMITER));
            ajax.put("newFileNames" , StringUtils.join(newFileNames, FILE_DELIMITER));
            ajax.put("originalFilenames" , StringUtils.join(originalFilenames, FILE_DELIMITER));
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }*/

    /**
     * 通用删除请求（单个）
     */
    /*
    @PostMapping("/delete")
    public AjaxResult uploadFile(@RequestBody Map<String, String> param) throws Exception {
        try {
            // 修改文件路径
            String filePath = param.get("filePath").replace("/profile/", "/");
            // 删除文件
            boolean flag = FileUtils.deleteFile(RuoYiConfig.getProfile() + filePath);
            if (flag) {
                return AjaxResult.success(true);
            }
            return AjaxResult.error("删除图片异常，请联系管理员");
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }*/
    /**
     * 本地资源通用下载
     */
    /*
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 " , resource));
            }
            // 本地资源路径
            String localPath = RuoYiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception e) {
            log.error("下载文件失败" , e);
        }
    }*/
}
