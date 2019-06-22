package com.example.demo.conver;


import javafx.util.Pair;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将Excel转为Html
 */
public class POIReadExcel {
    public static void main(String[] args) {
        try {
            String imagePathStr = "D:\\home\\image\\";
            String sourceFileName = "D:\\home\\Test.xls";
            String targetFileName = "D:\\home\\TestXls.html";
            excelToHtml(imagePathStr, sourceFileName, targetFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isExcelFile(String file) {
        return file.endsWith(".xls") || file.endsWith(".xlsx");
    }


    public static boolean excelToHtml(String imagePathStr, String sourceFileName, String targetFileName) {
        try {
            return excelToHtml(imagePathStr, sourceFileName, new FileInputStream(sourceFileName), targetFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean excelToHtml(String imagePathStr, String sourceFileName, InputStream inputStream, String targetFileName) {
        if (!isExcelFile(sourceFileName)) return false;
        File imageFileDir = new File(imagePathStr);
        if (!imageFileDir.exists() || !imageFileDir.isDirectory()) imageFileDir.mkdirs();
        if (!imagePathStr.endsWith("\\") || !imagePathStr.endsWith("/")) {
            imagePathStr = imagePathStr + File.separator;
        }
        String html = POIReadExcel.readExcelToHtml(inputStream, true, imagePathStr);
        if (html == null || html.trim().equals("")) return false;
        File file = new File(targetFileName);
        html = "<html><head>\n" +
                "    <meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">\n" +
                "</head><body>" + html + "</body></html>";
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(html.getBytes("utf-8"));
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static String readExcelToHtml(InputStream is, boolean isWithStyle, String imagePathDir) {
        String htmlExcel = null;
        try {
            Workbook wb = WorkbookFactory.create(is);
            htmlExcel =getExcelInfo(wb, 0,isWithStyle, imagePathDir);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return htmlExcel;
    }

    /**
     * 将excel表格转为html字符串
     * @param wb excel工作簿 对象
     * @param sheetNum  工作单角标 默认只输出第一个
     * @param isWithStyle 是否保留样式
     * @param imagePathDir  图片的保存路径
     * @return
     */
    private static String getExcelInfo(Workbook wb,int sheetNum, boolean isWithStyle, String imagePathDir) {

        StringBuffer sb = new StringBuffer();
        Sheet sheet = wb.getSheetAt(sheetNum);//获取第一个Sheet的内容
        // map等待存储excel图片
        Map<String, Pair<String, double[]>> imageDates = processSheetPictures(sheetNum, sheet, wb, imagePathDir);

        //读取excel拼装html
        int lastRowNum = sheet.getLastRowNum();
        Map<String, Object>[] map = getRowSpanColSpanMap(sheet);
        sb.append("<table style='border-collapse:collapse;width:100%;'>");
        Row row = null;        //兼容
        Cell cell = null;    //兼容

        for (int rowNum = sheet.getFirstRowNum(); rowNum <= lastRowNum; rowNum++) {
            if (rowNum > 1000) break;
            row = sheet.getRow(rowNum);

            int lastColNum = POIReadExcel.getColsOfTable(sheet)[0];
            int rowHeight = POIReadExcel.getColsOfTable(sheet)[1];

            if (null != row) {
                lastColNum = row.getLastCellNum();
                rowHeight = row.getHeight();
            }

            if (null == row) {
                sb.append("<tr><td >  </td></tr>");
                continue;
            } else if (row.getZeroHeight()) {
                continue;
            } else if (0 == rowHeight) {
                continue;     //针对jxl的隐藏行（此类隐藏行只是把高度设置为0，单getZeroHeight无法识别）
            }
            sb.append("<tr>");

            for (int colNum = 0; colNum < lastColNum; colNum++) {
                if (sheet.isColumnHidden(colNum)) continue;
                String imageRowNum = "0_" + rowNum + "_" + colNum;
                String imageHtml = "";
                cell = row.getCell(colNum);
                if ((imageDates == null || !imageDates.containsKey(imageRowNum)) && cell == null) {    //特殊情况 空白的单元格会返回null+//判断该单元格是否包含图片，为空时也可能包含图片
                    sb.append("<td>  </td>");
                    continue;
                }
                if (imageDates != null && imageDates.containsKey(imageRowNum)) {
                    //待修改路径
                    Pair<String, double[]> pair = imageDates.get(imageRowNum);
                    imageHtml = "<img src='" + pair.getKey() + "' style='height:" + pair.getValue()[1] + "px;" + "width:" + pair.getValue()[0] + "px'>";
                }
                String stringValue = getCellValue(cell);
                if (map[0].containsKey(rowNum + "," + colNum)) {
                    String pointString = (String) map[0].get(rowNum + "," + colNum);
                    int bottomeRow = Integer.valueOf(pointString.split(",")[0]);
                    int bottomeCol = Integer.valueOf(pointString.split(",")[1]);
                    int rowSpan = bottomeRow - rowNum + 1;
                    int colSpan = bottomeCol - colNum + 1;
                    if (map[2].containsKey(rowNum + "," + colNum)) {
                        rowSpan = rowSpan - (Integer) map[2].get(rowNum + "," + colNum);
                    }
                    sb.append("<td rowspan= '" + rowSpan + "' colspan= '" + colSpan + "' ");
                    if (map.length > 3 && map[3].containsKey(rowNum + "," + colNum)) {
                        //此类数据首行被隐藏，value为空，需使用其他方式获取值
                        stringValue = getMergedRegionValue(sheet, rowNum, colNum);
                    }
                } else if (map[1].containsKey(rowNum + "," + colNum)) {
                    map[1].remove(rowNum + "," + colNum);
                    continue;
                } else {
                    sb.append("<td ");
                }

                //判断是否需要样式
                if (isWithStyle) {
                    dealExcelStyle(wb, sheet, cell, sb, map);//处理单元格样式
                }

                sb.append(">");
                if (imageDates != null && imageDates.containsKey(imageRowNum)) sb.append(imageHtml);
                if (stringValue == null || "".equals(stringValue.trim())) {
                    sb.append("   ");
                } else {
                    // 将ascii码为160的空格转换为html下的空格（ ）
                    sb.append(stringValue.replace(String.valueOf((char) 160), " "));
                }
                sb.append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * 处理表格的 图片
     * @param sheetNum 工作单位置
     * @param sheet  工作单
     * @param workbook  工作簿
     * @param imagePathDir 图片存储位置
     * @return
     */
    private static Map<String, Pair<String, double[]>> processSheetPictures(int sheetNum, Sheet sheet, Workbook workbook, String imagePathDir) {
        Map<String, Picture> pictureMap = null;
        if (workbook instanceof HSSFWorkbook) {
            pictureMap = getSheetPictures03(sheetNum, (HSSFSheet) sheet, (HSSFWorkbook) workbook);
        } else if (workbook instanceof XSSFWorkbook) {
            pictureMap = getSheetPictures07(sheetNum, (XSSFSheet) sheet, (XSSFWorkbook) workbook);
        }
        try {
            return printImg(pictureMap, imagePathDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 分析excel表格，记录合并单元格相关的参数，用于之后html页面元素的合并操作
     *
     * @param sheet
     * @return
     */
    private static Map<String, Object>[] getRowSpanColSpanMap(Sheet sheet) {
        Map<String, String> map0 = new HashMap<String, String>();    //保存合并单元格的对应起始和截止单元格
        Map<String, String> map1 = new HashMap<String, String>();    //保存被合并的那些单元格
        Map<String, Integer> map2 = new HashMap<String, Integer>();    //记录被隐藏的单元格个数
        Map<String, String> map3 = new HashMap<String, String>();    //记录合并了单元格，但是合并的首行被隐藏的情况
        int mergedNum = sheet.getNumMergedRegions();
        CellRangeAddress range = null;
        Row row = null;
        for (int i = 0; i < mergedNum; i++) {
            range = sheet.getMergedRegion(i);
            int topRow = range.getFirstRow();
            int topCol = range.getFirstColumn();
            int bottomRow = range.getLastRow();
            int bottomCol = range.getLastColumn();
            /**
             * 此类数据为合并了单元格的数据
             * 1.处理隐藏（只处理行隐藏，列隐藏poi已经处理）
             */
            if (topRow != bottomRow) {
                int zeroRoleNum = 0;
                int tempRow = topRow;
                for (int j = topRow; j <= bottomRow; j++) {
                    row = sheet.getRow(j);
                    if (row.getZeroHeight() || row.getHeight() == 0) {
                        if (j == tempRow) {
                            //首行就进行隐藏，将rowTop向后移
                            tempRow++;
                            continue;//由于top下移，后面计算rowSpan时会扣除移走的列，所以不必增加zeroRoleNum;
                        }
                        zeroRoleNum++;
                    }
                }
                if (tempRow != topRow) {
                    map3.put(tempRow + "," + topCol, topRow + "," + topCol);
                    topRow = tempRow;
                }
                if (zeroRoleNum != 0) map2.put(topRow + "," + topCol, zeroRoleNum);
            }
            map0.put(topRow + "," + topCol, bottomRow + "," + bottomCol);
            int tempRow = topRow;
            while (tempRow <= bottomRow) {
                int tempCol = topCol;
                while (tempCol <= bottomCol) {
                    map1.put(tempRow + "," + tempCol, topRow + "," + topCol);
                    tempCol++;
                }
                tempRow++;
            }
            map1.remove(topRow + "," + topCol);
        }
        Map[] map = {map0, map1, map2, map3};
        System.err.println(map0);
        return map;
    }


    /**
     * 获取合并单元格的值
     *
     * @param sheet
     * @param row
     * @param column
     * @return
     */
    private static String getMergedRegionValue(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();

            if (row >= firstRow && row <= lastRow) {

                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);

                    return getCellValue(fCell);
                }
            }
        }
        return null;
    }

    /**
     * 获取表格单元格Cell内容
     *
     * @param cell
     * @return
     */
    private static String getCellValue(Cell cell) {
        String result = new String();
        switch (cell.getCellType()) {
            case NUMERIC:// 数字类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {// 处理日期格式、时间格式
                    SimpleDateFormat sdf = null;
                    if (cell.getCellStyle().getDataFormat() == HSSFDataFormat.getBuiltinFormat("h:mm")) {
                        sdf = new SimpleDateFormat("HH:mm");
                    } else {// 日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                    }
                    Date date = cell.getDateCellValue();
                    result = sdf.format(date);
                } else if (cell.getCellStyle().getDataFormat() == 58) {
                    // 处理自定义日期格式：m月d日(通过判断单元格的格式id解决，id的值是58)
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    double value = cell.getNumericCellValue();
                    Date date = DateUtil
                            .getJavaDate(value);
                    result = sdf.format(date);
                } else {
                    double value = cell.getNumericCellValue();
                    CellStyle style = cell.getCellStyle();
                    DecimalFormat format = new DecimalFormat();
                    String temp = style.getDataFormatString();
                    // 单元格设置成常规
                    if (temp.equals("General")) {
                        format.applyPattern("#");
                    }
                    result = format.format(value);
                }
                break;
            case STRING:// String类型
                result = cell.getRichStringCellValue().toString();
                break;
            case BLANK:
                result = "";
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

    /**
     * 处理表格样式
     *
     * @param wb
     * @param sheet
     * @param cell
     * @param sb
     */
    private static void dealExcelStyle(Workbook wb, Sheet sheet, Cell cell, StringBuffer sb, Map<String, Object>[] map) {
        CellStyle cellStyle = cell.getCellStyle();
        if (cellStyle != null) {
            HorizontalAlignment alignment = cellStyle.getAlignment();
            sb.append("align='" + convertAlignToHtml(alignment.getCode()) + "' ");//单元格内容的水平对齐方式
            VerticalAlignment verticalAlignment = cellStyle.getVerticalAlignment();
            sb.append("valign='" + convertVerticalAlignToHtml(verticalAlignment.getCode()) + "' ");//单元格中内容的垂直排列方式

            if (wb instanceof XSSFWorkbook) {

                XSSFFont xf = ((XSSFCellStyle) cellStyle).getFont();
                boolean boldWeight = xf.getBold();
                sb.append("style='");
                sb.append("font-weight:" + boldWeight + ";"); // 字体加粗
                sb.append("font-size: " + xf.getFontHeight() / 2 + "%;"); // 字体大小

                int topRow = cell.getRowIndex(), topColumn = cell.getColumnIndex();
                if (map[0].containsKey(topRow + "," + topColumn)) {//该单元格为合并单元格，宽度需要获取所有单元格宽度后合并
                    String value = (String) map[0].get(topRow + "," + topColumn);
                    String[] ary = value.split(",");
                    int bottomColumn = Integer.parseInt(ary[1]);
                    if (topColumn != bottomColumn) {//合并列，需要计算相应宽度
                        int columnWidth = 0;
                        for (int i = topColumn; i <= bottomColumn; i++) {
                            columnWidth += sheet.getColumnWidth(i);
                        }
                        sb.append("width:" + columnWidth / 256 * xf.getFontHeight() / 20 + "pt;");
                    } else {
                        int columnWidth = sheet.getColumnWidth(cell.getColumnIndex());
                        sb.append("width:" + columnWidth / 256 * xf.getFontHeight() / 20 + "pt;");
                    }
                } else {
                    int columnWidth = sheet.getColumnWidth(cell.getColumnIndex());
                    sb.append("width:" + columnWidth / 256 * xf.getFontHeight() / 20 + "pt;");
                }

                XSSFColor xc = xf.getXSSFColor();
                if (xc != null && !"".equals(xc.toString())) {
                    sb.append("color:#" + xc.getARGBHex().substring(2) + ";"); // 字体颜色
                }

                XSSFColor bgColor = (XSSFColor) cellStyle.getFillForegroundColorColor();
                if (bgColor != null && !"".equals(bgColor.toString())) {
                    sb.append("background-color:#" + bgColor.getARGBHex().substring(2) + ";"); // 背景颜色
                }
                sb.append("border:solid #000000 1px;");
                //                sb.append(getBorderStyle(0,cellStyle.getBorderTop(), ((XSSFCellStyle) cellStyle).getTopBorderXSSFColor()));
                //                sb.append(getBorderStyle(1,cellStyle.getBorderRight(), ((XSSFCellStyle) cellStyle).getRightBorderXSSFColor()));
                //                sb.append(getBorderStyle(2,cellStyle.getBorderBottom(), ((XSSFCellStyle) cellStyle).getBottomBorderXSSFColor()));
                //                sb.append(getBorderStyle(3,cellStyle.getBorderLeft(), ((XSSFCellStyle) cellStyle).getLeftBorderXSSFColor()));
            } else if (wb instanceof HSSFWorkbook) {
                HSSFFont hf = ((HSSFCellStyle) cellStyle).getFont(wb);
                boolean boldWeight = hf.getBold();
                short fontColor = hf.getColor();
                sb.append("style='");

                HSSFPalette palette = ((HSSFWorkbook) wb).getCustomPalette(); // 类HSSFPalette用于求的颜色的国际标准形式
                HSSFColor hc = palette.getColor(fontColor);
                sb.append("font-weight:" + boldWeight + ";"); // 字体加粗
                sb.append("font-size: " + hf.getFontHeight() / 2 + "%;"); // 字体大小
                String fontColorStr = convertToStardColor(hc);
                if (fontColorStr != null && !"".equals(fontColorStr.trim())) {
                    sb.append("color:" + fontColorStr + ";"); // 字体颜色
                }

                int topRow = cell.getRowIndex(), topColumn = cell.getColumnIndex();
                if (map[0].containsKey(topRow + "," + topColumn)) {//该单元格为合并单元格，宽度需要获取所有单元格宽度后合并
                    String value = (String) map[0].get(topRow + "," + topColumn);
                    String[] ary = value.split(",");
                    int bottomColumn = Integer.parseInt(ary[1]);
                    if (topColumn != bottomColumn) {//合并列，需要计算相应宽度
                        int columnWidth = 0;
                        for (int i = topColumn; i <= bottomColumn; i++) {
                            columnWidth += sheet.getColumnWidth(i);
                        }
                        sb.append("width:" + columnWidth / 256 * hf.getFontHeight() / 20 + "pt;");
                    } else {
                        int columnWidth = sheet.getColumnWidth(cell.getColumnIndex());
                        sb.append("width:" + columnWidth / 256 * hf.getFontHeight() / 20 + "pt;");
                    }
                } else {
                    int columnWidth = sheet.getColumnWidth(cell.getColumnIndex());
                    sb.append("width:" + columnWidth / 256 * hf.getFontHeight() / 20 + "pt;");
                }

                short bgColor = cellStyle.getFillForegroundColor();
                hc = palette.getColor(bgColor);
                String bgColorStr = convertToStardColor(hc);
                if (bgColorStr != null && !"".equals(bgColorStr.trim())) {
                    sb.append("background-color:" + bgColorStr + ";");        // 背景颜色
                }
                sb.append("border:solid #000000 1px;");
            }
            sb.append("' ");
        }
    }

    /**
     * 单元格内容的水平对齐方式
     *
     * @param alignment
     * @return
     */
    private static String convertAlignToHtml(short alignment) {
        String align = "left";
        if (HorizontalAlignment.LEFT.getCode() == alignment) {
            align = "left";
        } else if (HorizontalAlignment.CENTER.getCode() == alignment) {
            align = "center";
        } else if (HorizontalAlignment.RIGHT.getCode() == alignment) {
            align = "right";
        }
        return align;
    }

    /**
     * 单元格中内容的垂直排列方式
     *
     * @param verticalAlignment
     * @return
     */
    private static String convertVerticalAlignToHtml(short verticalAlignment) {
        String valign = "middle";
        if (verticalAlignment == VerticalAlignment.BOTTOM.getCode()) {
            valign = "bottom";
        } else if (verticalAlignment == VerticalAlignment.CENTER.getCode()) {
            valign = "center";
        } else if (verticalAlignment == VerticalAlignment.TOP.getCode()) {
            valign = "top";
        }
        return valign;
    }

    private static String convertToStardColor(HSSFColor hc) {
        StringBuffer sb = new StringBuffer("");
        if (hc != null) {
            if (HSSFColor.HSSFColorPredefined.AUTOMATIC.getIndex() == hc.getIndex()) {
                return null;
            }
            sb.append("#");
            for (int i = 0; i < hc.getTriplet().length; i++) {
                sb.append(fillWithZero(Integer.toHexString(hc.getTriplet()[i])));
            }
        }
        return sb.toString();
    }

    private static String fillWithZero(String str) {
        if (str != null && str.length() < 2) {
            return "0" + str;
        }
        return str;
    }

    private static String[] bordesr = {"border-top:", "border-right:", "border-bottom:", "border-left:"};
    private static String[] borderStyles = {"solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid", "solid", "solid", "solid", "solid"};

    @SuppressWarnings("unused")
    private static String getBorderStyle(HSSFPalette palette, int b, short s, short t) {
        if (s == 0) return bordesr[b] + borderStyles[s] + "#d0d7e5 1px;";
        String borderColorStr = convertToStardColor(palette.getColor(t));
        borderColorStr = borderColorStr == null || borderColorStr.length() < 1 ? "#000000" : borderColorStr;
        return bordesr[b] + borderStyles[s] + borderColorStr + " 1px;";
    }

    @SuppressWarnings("unused")
    private static String getBorderStyle(int b, short s, XSSFColor xc) {
        if (s == 0) return bordesr[b] + borderStyles[s] + "#d0d7e5 1px;";
        if (xc != null && !"".equals(xc)) {
            String borderColorStr = xc.getARGBHex();//t.getARGBHex();
            borderColorStr = borderColorStr == null || borderColorStr.length() < 1 ? "#000000" : borderColorStr.substring(2);
            return bordesr[b] + borderStyles[s] + borderColorStr + " 1px;";
        }
        return "";
    }

    /**
     * 获取Excel2003图片
     *
     * @param sheetNum 当前sheet编号
     * @param sheet    当前sheet对象
     * @param workbook 工作簿对象
     * @return Map key:图片单元格索引（0_1_1）String，value:图片流PictureData
     * @throws IOException
     */
    private static Map<String, Picture> getSheetPictures03(int sheetNum,
                                                           HSSFSheet sheet, HSSFWorkbook workbook) {

        Map<String, Picture> sheetIndexPicMap = new HashMap<String, Picture>();
        List<HSSFPictureData> pictures = workbook.getAllPictures();
        if (pictures.size() != 0) {
            for (HSSFShape shape : sheet.getDrawingPatriarch().getChildren()) {
                HSSFClientAnchor anchor = (HSSFClientAnchor) shape.getAnchor();
                shape.getLineWidth();
                if (shape instanceof HSSFPicture) {
                    HSSFPicture pic = (HSSFPicture) shape;
                    String picIndex = sheetNum + "_"
                            + anchor.getRow1() + "_"
                            + anchor.getCol1();
                    sheetIndexPicMap.put(picIndex, pic);
                }
            }
            return sheetIndexPicMap;
        } else {
            return null;
        }
    }

    /**
     * 获取Excel2007图片
     *
     * @param sheetNum 当前sheet编号
     * @param sheet    当前sheet对象
     * @return Map key:图片单元格索引（0_1_1）String，value:图片流PictureData
     */
    private static Map<String, Picture> getSheetPictures07(int sheetNum,
                                                           XSSFSheet sheet, XSSFWorkbook workbook) {
        Map<String, Picture> sheetIndexPicMap = new HashMap<String, Picture>();

        for (POIXMLDocumentPart dr : sheet.getRelations()) {
            if (dr instanceof XSSFDrawing) {
                XSSFDrawing drawing = (XSSFDrawing) dr;
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes) {
                    XSSFPicture pic = (XSSFPicture) shape;
                    XSSFClientAnchor anchor = pic.getPreferredSize();
                    CTMarker ctMarker = anchor.getFrom();
                    String picIndex = sheetNum + "_"
                            + ctMarker.getRow() + "_" + ctMarker.getCol();
                    sheetIndexPicMap.put(picIndex, pic);
                }
            }
        }

        return sheetIndexPicMap;
    }

    /**
     * 保存图片并输出保存后的信息
     *
     * @param map          读取到的图片信息
     * @param imagePathDir 图片存储路径
     * @return 图片的存储路径以及图片的宽高
     * @throws IOException
     */
    private static Map<String, Pair<String, double[]>> printImg(Map<String, Picture> map, String imagePathDir) throws IOException {
        if (map == null) return null;
        HashMap<String, Pair<String, double[]>> result = new HashMap<>();
        for (String key : map.keySet()) {
            // 获取图片流
            Picture pic = map.get(key);

            // 获取图片格式
            String ext = pic.getPictureData().suggestFileExtension();

            byte[] data = pic.getPictureData().getData();
            double[] wAndH = new double[]{pic.getImageDimension().getWidth(), pic.getImageDimension().getHeight()};
            String path = imagePathDir + "pic" + key + "." + ext;
            FileOutputStream out = new FileOutputStream(path);
            out.write(data);
            out.flush();
            out.close();
            result.put(key, new Pair<>(path, wAndH));
        }
        return result;
    }

    private static int[] getColsOfTable(Sheet sheet) {

        int[] data = {0, 0};
        for (int i = sheet.getFirstRowNum(); i < sheet.getLastRowNum(); i++) {
            if (null != sheet.getRow(i)) {
                data[0] = sheet.getRow(i).getLastCellNum();
                data[1] = sheet.getRow(i).getHeight();
            } else
                continue;
        }
        return data;
    }
}
