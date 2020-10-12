package com.zhou.shoehome.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author zhouzh6
 */
public class PmsUploadUtil {

    public static String uploadImage(MultipartFile multipartFile) {
        String imgUrl = "http://47.110.60.206";

        // 配置fdfs的全局链接地址
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();

        try {
            ClientGlobal.init(tracker);
            TrackerClient trackerClient = new TrackerClient();

            // 获得一个trackerServer的实例
            TrackerServer trackerServer = trackerClient.getTrackerServer();
            // 通过tracker获取一个storage链接客户端
            StorageClient storageClient = new StorageClient(trackerServer, null);

            byte[] fileBytes = multipartFile.getBytes();
            String filename = multipartFile.getOriginalFilename();

            // 获取文件后缀
            String suffixName = filename.substring(filename.lastIndexOf(".") + 1);

            String[] uploadInfos = storageClient.upload_appender_file(fileBytes, suffixName, null);

            for (String uploadInfo : uploadInfos) {
                imgUrl += "/" + uploadInfo;
            }
        } catch (IOException | MyException e) {
            e.printStackTrace();
        }
        return imgUrl;
    }
}
