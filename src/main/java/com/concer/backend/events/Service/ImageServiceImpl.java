package com.concer.backend.events.Service;

import com.concer.backend.Response.RestfulResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ImageServiceImpl implements ImageService {
    @Value("${product.image.directory}")
    private String imageDirectory;

    @Override
    public Resource getImage(String imageName) throws IOException {
        File imageFile = new File(imageDirectory + imageName);
        System.out.println("這是imageFile的路徑:" + imageFile);
        return new FileSystemResource(imageFile);



//      下方舊版 Byte[] 寫法
//        byte[] imageBytes = null;
//        File imageFile = new File(imageDirectory + imageName);
//
//        System.out.println("這是imageFile的路徑:" + imageFile);
//        BufferedImage image = ImageIO.read(imageFile);
//        //取得文件副檔名
//        String fileExtension = imageName.substring(imageName.lastIndexOf(".") + 1);
//        String formatName = getFormatName(fileExtension);
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(image, formatName, baos);
//        imageBytes = baos.toByteArray();
//
//        return imageBytes;
    }

    @Override
    public String saveImage(MultipartFile image) throws IOException {
        //實際專案中 Service 也要做二次檢查
        long maxSize = 2 * 1024 * 1024; // 2MB
        if (image.getSize() > maxSize) {
            log.error("檔案超過2MB");
            throw new IllegalArgumentException("檔案超過2MB");
        }

        String originName = image.getOriginalFilename();
        String suffix = originName.substring(originName.lastIndexOf("."));
        String imageName = UUID.randomUUID().toString() + suffix;

        File dir = new File(imageDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        image.transferTo(new File(dir, imageName));
        return imageName;
    }

    @Override
    public String getFormatName(String fileExtension) {

        switch (fileExtension.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return "jpeg";
            case "png":
                return "png";
            case "gif":
                return "gif";
            default:
                return "jpeg"; // 或者根據需求返回預設格式
        }
    }
}
