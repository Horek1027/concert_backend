package com.concer.backend.events.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageService {
    byte[] getImage(String imageName) throws IOException;
    String saveImage(MultipartFile image) throws IOException;

    String getFormatName (String fileExtension);
}
