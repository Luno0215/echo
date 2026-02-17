package com.luno.echo.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.luno.echo.config.AliOssProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.UUID;

@Component
public class AliOssUtil {

    @Resource
    private AliOssProperties aliOssProperties;

    /**
     * 上传文件
     * @param file 前端传来的文件
     * @return 文件的访问 URL
     */
    public String upload(MultipartFile file) throws Exception {
        // 1. 获取配置信息
        String endpoint = aliOssProperties.getEndpoint();
        String accessKeyId = aliOssProperties.getAccessKeyId();
        String accessKeySecret = aliOssProperties.getAccessKeySecret();
        String bucketName = aliOssProperties.getBucketName();

        // 2. 创建 OSSClient 实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 3. 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 4. 生成文件名 (UUID + 后缀)，防止覆盖
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + suffix;

            // 5. 上传文件到 OSS
            // 参数1: bucket名称, 参数2: 文件名, 参数3: 输入流
            ossClient.putObject(bucketName, fileName, inputStream);

            // 6. 拼接访问路径
            // 格式: https://bucket-name.endpoint/filename
            // 注意：endpoint 可能带 http:// 也可能不带，这里做一个简单的处理
            String urlEndpoint = endpoint;
            if (!endpoint.startsWith("http")) {
                urlEndpoint = "https://" + endpoint;
            }
            // 拼接 URL: https://echo-bucket.oss-cn-hangzhou.aliyuncs.com/abc.png
            String url = urlEndpoint.replace("://", "://" + bucketName + ".") + "/" + fileName;

            return url;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // 7. 关闭 OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}