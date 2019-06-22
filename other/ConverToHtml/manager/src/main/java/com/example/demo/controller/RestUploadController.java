package com.example.demo.controller;

import com.example.demo.conver.FormatConversionUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;

@RestController
public class RestUploadController {

    private final Logger logger = LoggerFactory.getLogger(RestUploadController.class);

    //Save the uploaded file to this folder
    private final String UPLOADED_FOLDER;

    {
        String path = "d:/temp";
        try {
            path = new File(ResourceUtils.getFile("classpath:"), "static").getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        UPLOADED_FOLDER = path;
    }

//    // 3.1.1 Single file upload
//    @PostMapping("/api/upload")
//    // If not @RestController, uncomment this
//    //@ResponseBody
//    public ResponseEntity<?> uploadFile(
//            @RequestParam("file") MultipartFile uploadfile) {
//
//        logger.debug("Single file upload!");
//
//        if (uploadfile.isEmpty()) {
//            return new ResponseEntity("please select a file!", HttpStatus.OK);
//        }
//
//        try {
//            saveUploadedFiles(Arrays.asList(uploadfile));
//
//        } catch (IOException e) {
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
//
//        return new ResponseEntity("Successfully uploaded - " +
//                uploadfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
//
//    }


    @PostMapping("/api/upload/multi")
    public ResponseEntity<?> uploadFileMulti(
            @RequestParam("file") MultipartFile[] uploadfiles) {
//        String uploadedFileName = Arrays.stream(uploadfiles).map(MultipartFile::getOriginalFilename)
//                .filter(x -> !StringUtils.isEmpty(x)).collect(Collectors.joining(" , "));

        if (uploadfiles == null || uploadfiles.length < 1 || uploadfiles[0] == null) {
            return new ResponseEntity<>("please select a file!", HttpStatus.BAD_REQUEST);
        }
        MultipartFile uploadfile = uploadfiles[0];

        if (!FormatConversionUtils.verifyType(uploadfile.getOriginalFilename())) {
            return new ResponseEntity<>("Only doc, ppt, xls, pdf files are supported ", HttpStatus.BAD_REQUEST);
        }
        String path = FormatConversionUtils.transformFile(uploadfile, UPLOADED_FOLDER);
        if (Strings.isBlank(path)) {
            return new ResponseEntity<>("please retry upload file!", HttpStatus.BAD_REQUEST);
        }
        path = path.replace(UPLOADED_FOLDER, "").replace("\\", "/");
        logger.info(path);
        if (path.endsWith(".pdf")) path = "/generic/web/viewer.html?file=" + path;
        return new ResponseEntity<>(path, HttpStatus.OK);

    }
//
//    // 3.1.3 maps html form to a Model
//    @PostMapping("/api/upload/multi/model")
//    public ResponseEntity<?> multiUploadFileModel(@ModelAttribute UploadModel model) {
//
//        try {
//
//            saveUploadedFiles(Arrays.asList(model.files));
//
//        } catch (IOException e) {
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
//
//        return new ResponseEntity<>("Successfully uploaded!", HttpStatus.OK);
//
//    }


}
