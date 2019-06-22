package com.example.demo.conver;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.xslf.usermodel.*;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

public class POIReadPowerPoint {
    public static void main(String[] args) {
        String imagePathStr = "D:\\home\\image\\";
        String sourceFileName = "D:\\home\\Test.pptx";
        String targetFileName = "D:\\home\\TestPPt.html";
        powerPointToHtml(imagePathStr, sourceFileName, targetFileName);
    }

    public static boolean isPowerPointFile(String file) {
        return file.endsWith(".ppt") || file.endsWith(".pptx");
    }

    /**
     * 通过Python脚本将ppt文件转为pdf
     * @param sourceFileName  源文件路径
     * @param targetDirName  目标文件路径
     * @return
     */
    public static String powerPointToPdf(String sourceFileName, String targetDirName) {
        if (!isPowerPointFile(sourceFileName)) return null;
        String exe = "python";
        String command = System.getProperty("user.dir")+"\\py\\pptToPdf.py";
        String name = new File(sourceFileName).getName();
        String[] cmdArr = new String[]{exe, command, "-s", sourceFileName, "-t", targetDirName};
        System.out.println("py目录:"+command+"  源目录:"+sourceFileName+"  目标目录:"+targetDirName);
        try {
            Process process = Runtime.getRuntime().exec(cmdArr);           process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return targetDirName;
    }

    /**
     * 将 ppt文件每页转为图片，最后封装成html文件并保存
     * @param imagePathStr 图片路径
     * @param sourceFileName  源文件路径
     * @param targetFileName   目标文件路径
     * @return
     */
    public static boolean powerPointToHtml(String imagePathStr, String sourceFileName, String targetFileName) {
        if (!isPowerPointFile(sourceFileName)) return false;
        File imageFileDir = new File(imagePathStr);
        if (!imageFileDir.exists() || !imageFileDir.isDirectory()) imageFileDir.mkdirs();
        if (sourceFileName.endsWith(".ppt")) {
            try {
                pptTohtml(imagePathStr, sourceFileName, targetFileName);
            } catch (IOException | TransformerException e) {
                e.printStackTrace();
                return false;
            }
        } else if (sourceFileName.endsWith(".pptx")) {
            try {
                pptxTohtml(imagePathStr, sourceFileName, targetFileName);
            } catch (IOException | TransformerException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 将ppt文件转为html文件
     * @param imagePathStr  图片路径
     * @param sourceFileName 源文件路径
     * @param targetFileName  目标文件路径
     * @throws IOException
     * @throws TransformerException
     */
    private static void pptTohtml(String imagePathStr, String sourceFileName, String targetFileName) throws IOException, TransformerException {
        // 读入PPT文件
        File file = new File(sourceFileName);
        FileInputStream is = new FileInputStream(file);
        HSLFSlideShow ppt = new HSLFSlideShow(is);
        is.close();

        Dimension pgsize = ppt.getPageSize();
        List<HSLFSlide> slides = ppt.getSlides();
        FileOutputStream out = null;
        String imghtml = "";
        for (int i = 0; i < slides.size(); i++) {
            System.out.print("第" + i + "页。");

            List<List<HSLFTextParagraph>> textParagraphs = slides.get(i).getTextParagraphs();
            for (List<HSLFTextParagraph> list : textParagraphs) {
                for (HSLFTextParagraph hSLFTextParagraph : list) {
                    hSLFTextParagraph.setBulletFont("simsun");
                }
            }

            BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);

            Graphics2D graphics = img.createGraphics();
            graphics.setPaint(Color.BLUE);
            graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            slides.get(i).draw(graphics);

            // 这里设置图片的存放路径和图片的格式(jpeg,png,bmp等等),注意生成文件路径
            out = new FileOutputStream(imagePathStr + "/" + (i + 1) + ".jpeg");
            javax.imageio.ImageIO.write(img, "jpeg", out);
            //图片在html加载路径
            String imgs = imagePathStr + "/" + (i + 1) + ".jpeg";
            imghtml += "<img src=\'" + imgs + "\' style=\'width:1200px;height:830px;vertical-align:text-bottom;\'><br><br><br><br>";

        }
        createHtml(targetFileName,  imghtml);

    }

    /**
     *
     * @param imagePathStr
     * @param sourceFileName
     * @param targetFileName
     * @throws IOException
     * @throws TransformerException
     */
    private static void pptxTohtml(String imagePathStr, String sourceFileName, String targetFileName) throws IOException, TransformerException {
        File file = new File(sourceFileName);
        FileInputStream is = new FileInputStream(file);
        XMLSlideShow ppt = new XMLSlideShow(is);
        is.close();

        Dimension pgsize = ppt.getPageSize();
        System.out.println(pgsize.width + "--" + pgsize.height);

        List<XSLFSlide> pptPageXSLFSLiseList = ppt.getSlides();
        FileOutputStream out = null;
        String imghtml = "";
        for (int i = 0; i < pptPageXSLFSLiseList.size(); i++) {
            try {
                for (XSLFShape shape : pptPageXSLFSLiseList.get(i).getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape tsh = (XSLFTextShape) shape;
                        for (XSLFTextParagraph p : tsh.getTextParagraphs()) {
                            for (XSLFTextRun r : p.getTextRuns()) {
                                r.setFontFamily("simsun");
                            }
                        }
                    }
                }
                BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();
                // clear the drawing area
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
                // render
                pptPageXSLFSLiseList.get(i).draw(graphics);


                String Imgname = imagePathStr + "/" + (i + 1) + ".jpeg";
                out = new FileOutputStream(Imgname);
                javax.imageio.ImageIO.write(img, "jpeg", out);
                //图片在html加载路径
                String imgs = imagePathStr + "/" + (i + 1) + ".jpeg";
                imghtml += "<img src=\'" + imgs + "\' style=\'width:1200px;height:830px;vertical-align:text-bottom;\'><br><br><br><br>";
            } catch (Exception e) {
                System.out.println(e);
                System.out.println("第" + i + "张ppt转换出错");
            }
        }

        System.out.println("7success");
        createHtml(targetFileName,  imghtml);
    }

    /**
     * 生成html文件
     * @param targetFileName  源文件
     * @param imghtml 组装的图片html字符串
     * @throws IOException
     */
    private static void createHtml(String targetFileName, String imghtml) throws  IOException {
        String ppthtml = "<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body>" + imghtml + "</body></html>";

        FileOutputStream fileOutputStream = new FileOutputStream(new File(targetFileName));
        fileOutputStream.write(ppthtml.getBytes("utf-8"));
        fileOutputStream.close();
    }

}
