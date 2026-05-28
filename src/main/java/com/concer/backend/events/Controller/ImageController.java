package com.concer.backend.events.Controller;

import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/image")
@Slf4j
public class ImageController {
    @Autowired
    private ImageService imageService;

    @GetMapping("/{imageName}")
    public ResponseEntity<Resource> select(@PathVariable String imageName) throws IOException{
        if (imageName == null){
            return ResponseEntity.notFound().build();
        }

        // 1. 毫無懸念，直接跟 Service 要 Resource
        Resource resource = imageService.getImage(imageName);

        // 2. 這一行算「組裝回覆」，不算複雜業務邏輯：利用 MediaTypeFactory 自動從 Resource 的檔名判斷型態
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                //30 天內不用重新下載,直接從瀏覽器快取拿
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .contentType(mediaType)
                .body(resource);

//      下方舊版 Byte[] 寫法
//        byte[] imageData = imageService.getImage(imageName);
//        String fileExtension = imageName.substring(imageName.lastIndexOf(".")+1);
//        MediaType mediaType;
//        switch (fileExtension.toLowerCase()){
//            case "jpeg":
//            case"jpg":
//                mediaType = MediaType.IMAGE_JPEG;
//                break;
//            case"png":
//                mediaType = MediaType.IMAGE_PNG;
//                break;
//            case"gif":
//                mediaType = MediaType.IMAGE_GIF;
//                break;
//            default:
//                mediaType = MediaType.APPLICATION_OCTET_STREAM;
//                break;
//        }
//      MediaType.APPLICATION_OCTET_STREAM是 MediaType的其中一個類型，設置為表示二進制數據流的媒體類型。
//        HttpHeaders headers =new HttpHeaders();
//        headers.setContentType(mediaType);
//        return new ResponseEntity<Resource>(imageData,headers, HttpStatus.OK);
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

        if (image == null){
            log.info("image == null)");
        }

        if (image != null) {
            RestfulResponse<String> response = new RestfulResponse<>("00000", "成功",
                    imageService.saveImage(image));
            log.info("圖片新增成功");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        }
        log.info("圖片新增失敗");
        return null;
    }
}


