# doc,xls,ppt,pdf在线预览  
1. 针对doc,xls文件，通过[poi](https://poi.apache.org/)将doc,xls文件转为html后进行展示  
2. 针对pdf文件，通过[pdf.js](https://github.com/mozilla/pdf.js)直接在浏览器预览   
3. 针对ppt文件有两种方式。
    1. 通过python脚本通过将ppt转为pdf后，通过pdf.js进行展示
    2. 通过poi将每页输出为图片后，拼接为html进行展示   
# 各个工具类
[doc转为html工具类](manager/src/main/java/com/example/demo/conver/POIReadWord.java)  
[xls转为html工具类](manager/src/main/java/com/example/demo/conver/POIReadExcel.java)  
[ppt转为html工具类](manager/src/main/java/com/example/demo/conver/POIReadPowerPoint.java)  
[ppt转pdf脚本](py/pptToPdf.py)  
[pdf.js下载](http://mozilla.github.io/pdf.js/)
