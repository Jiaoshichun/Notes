package com.example.demo.conver;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FormatConversionUtils {

    public static boolean verifyType(String fileName) {
        return POIReadExcel.isExcelFile(fileName)
                || POIReadWord.isWordFile(fileName)
                || POIReadPowerPoint.isPowerPointFile(fileName)
                || fileName.endsWith(".pdf");
    }

    /**
     * 文件转换
     * 针对doc xls 将直接转换为html文件 返回html文件路径
     * 针对ppt，将ppt转为pdf 返回pdf文件路径
     * 针对 pdf  直接保存后，返回pdf文件路径
     * * @param uploadFile  上传的文件对象
     * @param savePath  文件 的保存路径
     * @return 转换后的文件路径
     */
    public static String transformFile(MultipartFile uploadFile, String savePath) {
        String targetFileName = savePath + "\\Preview.html";
        String pdfFileName = savePath + "\\Preview.pdf";
        String pptFileName = savePath + "\\upload.ppt";
        String imagePathStr = savePath + File.separator + "image";
        String filename = uploadFile.getOriginalFilename();
        if (POIReadWord.isWordFile(filename)) {
            try {
                POIReadWord.wordToHtml(imagePathStr, filename, uploadFile.getInputStream(), targetFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (POIReadExcel.isExcelFile(filename)) {
            try {
                POIReadExcel.excelToHtml(imagePathStr, filename, uploadFile.getInputStream(), targetFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (POIReadPowerPoint.isPowerPointFile(filename)) {
            try {
                Files.write(Paths.get(pptFileName), uploadFile.getBytes());
                POIReadPowerPoint.powerPointToPdf(pptFileName, pdfFileName);
                targetFileName = pdfFileName;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (filename.endsWith("pdf")) {
            try {
                Files.write(Paths.get(pdfFileName), uploadFile.getBytes());
                targetFileName = pdfFileName;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return targetFileName;
    }
}
