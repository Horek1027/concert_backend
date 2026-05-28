package com.concer.backend.events.Service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageService {
    Resource getImage(String imageName) throws IOException;
    String saveImage(MultipartFile image) throws IOException;

    String getFormatName (String fileExtension);
}
