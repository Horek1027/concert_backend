package com.concer.backend.events.Service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {
    @Value("${product.image.directory}")
    private String imageDirectory;

    @Override
    public byte[] getImage(String imageName) throws IOException {
        byte[] imageBytes = null;

        File imageFile = new File(imageDirectory + imageName);
        System.out.println("這是imageFile的路徑:" + imageFile);
        BufferedImage image = ImageIO.read(imageFile);
//取得文件副檔名
        String fileExtension = imageName.substring(imageName.lastIndexOf(".") + 1);
        String formatName = getFormatName(fileExtension);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, baos);
        imageBytes = baos.toByteArray();

        return imageBytes;
    }

    @Override
    public String saveImage(MultipartFile image) throws IOException {
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
