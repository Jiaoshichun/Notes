package com.example.demo.conver;

import fr.opensagres.poi.xwpf.converter.core.BasicURIResolver;
import fr.opensagres.poi.xwpf.converter.core.FileImageExtractor;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class POIReadWord {
    public static void main(String[] args) {
        try {
            String imagePathStr = "D:\\home\\image\\";
            String sourceFileName = "D:\\home\\Test.docx";
            String targetFileName = "D:\\home\\TestWord.html";
            wordToHtml(imagePathStr, sourceFileName, targetFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isWordFile(String file) {
        return file.endsWith(".doc") || file.endsWith(".docx");
    }

    public static boolean wordToHtml(String imagePathStr, String sourceFileName, String targetFileName) {
        try {
            return wordToHtml(imagePathStr, sourceFileName, new FileInputStream(sourceFileName), targetFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean wordToHtml(String imagePathStr, String fileName, InputStream inputStream, String targetFileName) {
        if (!isWordFile(fileName)) return false;
        File file = new File(imagePathStr);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        if (fileName.endsWith(".doc")) {
            try {
                docToHtml(imagePathStr, inputStream, targetFileName);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else if (fileName.endsWith(".docx")) {
            try {
                docxToHtml(imagePathStr, inputStream, targetFileName);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private static String docToHtml(String imagePathStr, String sourceFileName, String targetFileName) throws Exception {
        return docToHtml(imagePathStr, new FileInputStream(sourceFileName), targetFileName);
    }

    private static String docToHtml(String imagePathStr, InputStream inputStream, String targetFileName) throws Exception {
        HWPFDocument wordDocument = new HWPFDocument(inputStream);
        org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(document);
        //保存图片，并返回图片的相对路径
        wordToHtmlConverter.setPicturesManager((content, pictureType, name, width, height) -> {
            try (FileOutputStream out = new FileOutputStream(new File(imagePathStr, name))) {
                out.write(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "image/" + name;
        });
        wordToHtmlConverter.processDocument(wordDocument);
        org.w3c.dom.Document htmlDocument = wordToHtmlConverter.getDocument();
        NodeList headList = htmlDocument.getDocumentElement().getElementsByTagName("head");
        if (headList != null && headList.getLength() > 0) {
            Element meta = htmlDocument.createElement("meta");
            meta.setAttribute("http-equiv", "content-type");
            meta.setAttribute("content", "text/html;charset=utf-8");
            headList.item(0).appendChild(meta);
        }

        DOMSource domSource = new DOMSource(htmlDocument);
        StreamResult streamResult = new StreamResult(new File(targetFileName));
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "html");
        serializer.transform(domSource, streamResult);
        return targetFileName;
    }

    private static String docxToHtml(String imagePathStr, String sourceFileName, String targetFileName) throws Exception {
        return docxToHtml(imagePathStr, new FileInputStream(sourceFileName), targetFileName);
    }

    private static String docxToHtml(String imagePathStr, InputStream inputStream, String targetFileName) throws Exception {
        File file = new File(imagePathStr);
        if (!file.exists()) {
            file.mkdirs();
        }
        StringWriter stringWriter = null;
        FileWriter writer = null;
        try {
            XWPFDocument document = new XWPFDocument(inputStream);
            XHTMLOptions options = XHTMLOptions.create();
            // 存放图片的文件夹
            options.setExtractor(new FileImageExtractor(new File(imagePathStr)));
            // html中图片的路径
            options.URIResolver(new BasicURIResolver("image"));
            stringWriter = new StringWriter();
            XHTMLConverter xhtmlConverter = (XHTMLConverter) XHTMLConverter.getInstance();
            xhtmlConverter.convert(document, stringWriter, options);
            StringBuffer buffer = stringWriter.getBuffer();
            buffer.insert(buffer.indexOf("<head>") + 6, " <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">");
            writer = new FileWriter(targetFileName);
            writer.write(buffer.toString());
            writer.flush();
        } finally {
            if (stringWriter != null) {
                stringWriter.close();
            }
            if (writer != null) {
                writer.close();
            }
        }

        return targetFileName;
    }

}
