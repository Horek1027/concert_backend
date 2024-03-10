package com.concer.backend.events.Controller;

import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Service.ImageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/image")
public class ImageController {
    @Autowired
    private ImageService imageService;
    @GetMapping("/{imageName}")
    public ResponseEntity<byte[]> select(@PathVariable String imageName) throws IOException{
        if (imageName == null){
            return ResponseEntity.notFound().build();
        }

        byte[] imageData = imageService.getImage(imageName);

        String fileExtension = imageName.substring(imageName.lastIndexOf(".")+1);

        MediaType mediaType;

        switch (fileExtension.toLowerCase()){
            case "jpeg":
            case"jpg":
                mediaType = MediaType.IMAGE_JPEG;
                break;
            case"png":
                mediaType = MediaType.IMAGE_PNG;
                break;
            case"gif":
                mediaType = MediaType.IMAGE_GIF;
                break;
            default:
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                break;
        }
//MediaType.APPLICATION_OCTET_STREAM是 MediaType的其中一個類型，設置為表示二進制數據流的媒體類型。
        HttpHeaders headers =new HttpHeaders();
        headers.setContentType(mediaType);
        return new ResponseEntity<byte[]>(imageData,headers, HttpStatus.OK);
    }

//    @PostMapping
//    public ResponseEntity<?> insert(MultipartFile image) throws IOException {
//        if (image != null) {
//            //刪減一些東西了
//            return ResponseEntity.status(HttpStatus.OK).body(imageService.saveImage(image));
//        }
//        return null;
//    }
    @PostMapping
    public ResponseEntity<RestfulResponse<?>> insert(MultipartFile image) throws IOException {
        if (image != null) {
            RestfulResponse<String> response = new RestfulResponse<>("00000", "成功",
                    imageService.saveImage(image));
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        return null;
    }
}


