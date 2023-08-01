package cn.ichensw.partner.service.impl;

import cn.ichensw.partner.common.ErrorCode;
import cn.ichensw.partner.config.AliyunOSSConfig;
import cn.ichensw.partner.exception.BusinessException;
import cn.ichensw.partner.service.FileUploadService;
import cn.ichensw.partner.service.UserService;
import cn.ichensw.partner.utils.AliyunOSSUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @Author zhx
 * @date 2023/4/24
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    /**
     * 允许上传文件(图片)的格式
     */
    private static final String[] IMAGE_TYPE = new String[]{".bmp", ".jpg", ".jpeg", ".gif", ".png"};

    /**
     * 注入阿里云OSS基本配置类
     */
    @Resource
    private AliyunOSSConfig aliyunOSSConfig;

    @Resource
    private UserService userService;

    /**
     * 文件上传
     * 注：阿里云OSS文件上传官方文档链接：https://help.aliyun.com/document_detail/84781.html?spm=a2c4g.11186623.6.749.11987a7dRYVSzn
     */
    public String upload(MultipartFile uploadFile, HttpServletRequest request) {
        // 获取oss的Bucket名称
        String bucketName = aliyunOSSConfig.getBucketName();
        // 获取oss的地域节点
        String endpoint = aliyunOSSConfig.getEndPoint();
        // 获取oss的AccessKeySecret
        String accessKeySecret = aliyunOSSConfig.getAccessKeySecret();
        // 获取oss的AccessKeyId
        String accessKeyId = aliyunOSSConfig.getAccessKeyId();
        // 获取oss目标文件夹
        String fileHost = aliyunOSSConfig.getFileHost();
        // 返回图片上传后返回的url
        String imageUrl = "";
        // 获取阿里云OSS客户端
        OSS ossClient = AliyunOSSUtil.initOSS(endpoint, accessKeyId, accessKeySecret);

        // 校验图片格式
        boolean isLegal = false;
        for (String type : IMAGE_TYPE) {
            if (StringUtils.endsWithIgnoreCase(uploadFile.getOriginalFilename(), type)) {
                isLegal = true;
                break;
            }
        }
        // 如果图片格式不合法
        if (!isLegal) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式不合法");
        }
        // 获取文件原名称
        String originalFilename = uploadFile.getOriginalFilename();
        // 获取文件类型
        String fileType = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 新文件名称
        String newFileName = UUID.randomUUID() + fileType;
        // 构建日期路径, 例如：OSS目标文件夹/2020/10/31/文件名
        String filePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        // 文件上传的路径地址
        String uploadImageUrl = fileHost + "/" + filePath + "/" + newFileName;

        // 获取文件输入流
        InputStream inputStream = null;
        try {
            inputStream = uploadFile.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         下面两行代码是重点坑：
         现在阿里云OSS 默认图片上传ContentType是image/jpeg
         也就是说，获取图片链接后，图片是下载链接，而并非在线浏览链接，
         因此，这里在上传的时候要解决ContentType的问题，将其改为image/jpg
        */
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType("image/jpg");

        //文件上传至阿里云OSS
        ossClient.putObject(bucketName, uploadImageUrl, inputStream, meta);
        // 获取文件上传后的图片返回地址
        imageUrl = "http://" + bucketName + "." + endpoint + "/" + uploadImageUrl;
//        // 获取当前用户信息
//        User loginUser = userService.getLoginUser(request);
//        // 修改头像地址
//        loginUser.setAvatarUrl(imageUrl);
//        userService.updateById(loginUser);
        return imageUrl;
    }

    /*
     * 文件下载
     */
    public void download(String fileName, HttpServletResponse response) throws UnsupportedEncodingException {
//        // 设置响应头为下载
//        response.setContentType("application/x-download");
//        // 设置下载的文件名
//        response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
//        response.setCharacterEncoding("UTF-8");
        // 文件名以附件的形式下载
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

        // 获取oss的地域节点
        String endpoint = aliyunOSSConfig.getEndPoint();
        // 获取oss的AccessKeySecret
        String accessKeySecret = aliyunOSSConfig.getAccessKeySecret();
        // 获取oss的AccessKeyId
        String accessKeyId = aliyunOSSConfig.getAccessKeyId();
        // 获取oss的Bucket名称
        String bucketName = aliyunOSSConfig.getBucketName();
        // 获取oss目标文件夹
        String fileHost = aliyunOSSConfig.getFileHost();
        // 日期目录
        // 注意，这里虽然写成这种固定获取日期目录的形式，逻辑上确实存在问题，但是实际上，filePath的日期目录应该是从数据库查询的
        String filePath = new DateTime().toString("yyyy/MM/dd");

        // 获取阿里云OSS客户端
        OSS ossClient = AliyunOSSUtil.initOSS(endpoint, accessKeyId, accessKeySecret);

        String fileKey = fileHost + "/" + filePath + "/" + fileName;
        // ossObject包含文件所在的存储空间名称、文件名称、文件元信息以及一个输入流。
        OSSObject ossObject = ossClient.getObject(bucketName, fileKey);
        try {
            // 读取文件内容。
            InputStream inputStream = ossObject.getObjectContent();
            // 把输入流放入缓存流
            BufferedInputStream in = new BufferedInputStream(inputStream);
            ServletOutputStream outputStream = response.getOutputStream();
            // 把输出流放入缓存流
            BufferedOutputStream out = new BufferedOutputStream(outputStream);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 文件删除
     */
    public void delete(String fileName) {
        // 获取oss的Bucket名称
        String bucketName = aliyunOSSConfig.getBucketName();
        // 获取oss的地域节点
        String endpoint = aliyunOSSConfig.getEndPoint();
        // 获取oss的AccessKeySecret
        String accessKeySecret = aliyunOSSConfig.getAccessKeySecret();
        // 获取oss的AccessKeyId
        String accessKeyId = aliyunOSSConfig.getAccessKeyId();
        // 获取oss目标文件夹
        String fileHost = aliyunOSSConfig.getFileHost();
        // 日期目录
        // 注意，这里虽然写成这种固定获取日期目录的形式，逻辑上确实存在问题，但是实际上，filePath的日期目录应该是从数据库查询的
        String filePath = new DateTime().toString("yyyy/MM/dd");

        // 建议在方法中创建OSSClient 而不是使用@Bean注入，不然容易出现Connection pool shut down
        OSS ossClient = AliyunOSSUtil.initOSS(endpoint,
                accessKeyId, accessKeySecret);
        try {
            /*
             注意：在实际项目中，不需要删除OSS文件服务器中的文件，
             只需要删除数据库存储的文件路径即可！
             */
            // 根据BucketName,filetName删除文件
            // 删除目录中的文件，如果是最后一个文件fileoath目录会被删除。
            String fileKey = fileHost + "/" + filePath + "/" + fileName;
            ossClient.deleteObject(bucketName, fileKey);

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        } finally {
            ossClient.shutdown();
        }
    }
}
