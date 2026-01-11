package com.alquds.edu.ArchiveSystem.service.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Service for converting Office documents to HTML format.
 * Supports Word (.doc, .docx), Excel (.xls, .xlsx), and PowerPoint (.ppt, .pptx) files.
 */
@Service
@Slf4j
public class OfficeDocumentConverter {
    
    /**
     * Convert an Office document to HTML.
     * 
     * @param filePath the path to the Office document
     * @param mimeType the MIME type of the document
     * @return HTML representation as byte array
     * @throws IOException if conversion fails
     */
    public byte[] convertToHtml(String filePath, String mimeType) throws IOException {
        log.info("Converting Office document to HTML: {} (type: {})", filePath, mimeType);
        
        String html;
        
        switch (mimeType) {
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                html = convertDocxToHtml(filePath);
                break;
            case "application/msword":
                html = convertDocToHtml(filePath);
                break;
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                html = convertXlsxToHtml(filePath);
                break;
            case "application/vnd.ms-excel":
                html = convertXlsToHtml(filePath);
                break;
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                html = convertPptxToHtml(filePath);
                break;
            case "application/vnd.ms-powerpoint":
                html = convertPptToHtml(filePath);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Office document type: " + mimeType);
        }
        
        return html.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Convert DOCX (Word 2007+) to HTML.
     */
    private String convertDocxToHtml(String filePath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; }");
        html.append("p { margin: 10px 0; }");
        html.append("</style></head><body>");
        
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    html.append("<p>").append(escapeHtml(text)).append("</p>");
                }
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Convert DOC (Word 97-2003) to HTML.
     */
    private String convertDocToHtml(String filePath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; }");
        html.append("p { margin: 10px 0; }");
        html.append("</style></head><body>");
        
        try (FileInputStream fis = new FileInputStream(filePath);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            
            String[] paragraphs = extractor.getParagraphText();
            for (String paragraph : paragraphs) {
                if (paragraph != null && !paragraph.trim().isEmpty()) {
                    html.append("<p>").append(escapeHtml(paragraph.trim())).append("</p>");
                }
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Convert XLSX (Excel 2007+) to HTML.
     */
    private String convertXlsxToHtml(String filePath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append("h2 { margin-top: 20px; }");
        html.append("</style></head><body>");
        
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                html.append("<h2>").append(escapeHtml(sheet.getSheetName())).append("</h2>");
                html.append(convertSheetToHtml(sheet));
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Convert XLS (Excel 97-2003) to HTML.
     */
    private String convertXlsToHtml(String filePath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append("h2 { margin-top: 20px; }");
        html.append("</style></head><body>");
        
        try (FileInputStream fis = new FileInputStream(filePath);
             HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                html.append("<h2>").append(escapeHtml(sheet.getSheetName())).append("</h2>");
                html.append(convertSheetToHtml(sheet));
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Convert a sheet to HTML table.
     */
    private String convertSheetToHtml(Sheet sheet) {
        StringBuilder html = new StringBuilder();
        html.append("<table>");
        
        Iterator<Row> rowIterator = sheet.iterator();
        boolean isFirstRow = true;
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            html.append("<tr>");
            
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String cellValue = getCellValue(cell);
                
                if (isFirstRow) {
                    html.append("<th>").append(escapeHtml(cellValue)).append("</th>");
                } else {
                    html.append("<td>").append(escapeHtml(cellValue)).append("</td>");
                }
            }
            
            html.append("</tr>");
            isFirstRow = false;
        }
        
        html.append("</table>");
        return html.toString();
    }
    
    /**
     * Get cell value as string.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    /**
     * Convert PPTX (PowerPoint 2007+) to HTML.
     */
    private String convertPptxToHtml(String filePath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; }");
        html.append(".slide { border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; background: #f9f9f9; }");
        html.append(".slide-number { font-weight: bold; color: #666; margin-bottom: 10px; }");
        html.append("p { margin: 5px 0; }");
        html.append("</style></head><body>");
        
        try (FileInputStream fis = new FileInputStream(filePath);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            
            int slideNumber = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                html.append("<div class='slide'>");
                html.append("<div class='slide-number'>Slide ").append(slideNumber).append("</div>");
                
                // Extract text from all shapes in the slide
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        org.apache.poi.xslf.usermodel.XSLFTextShape textShape = 
                            (org.apache.poi.xslf.usermodel.XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            html.append("<p>").append(escapeHtml(text)).append("</p>");
                        }
                    }
                });
                
                html.append("</div>");
                slideNumber++;
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Convert PPT (PowerPoint 97-2003) to HTML.
     */
    private String convertPptToHtml(String filePath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; }");
        html.append(".slide { border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; background: #f9f9f9; }");
        html.append(".slide-number { font-weight: bold; color: #666; margin-bottom: 10px; }");
        html.append("p { margin: 5px 0; }");
        html.append("</style></head><body>");
        
        try (FileInputStream fis = new FileInputStream(filePath);
             HSLFSlideShow ppt = new HSLFSlideShow(fis)) {
            
            html.append("<div class='slide'>");
            html.append("<p>PowerPoint 97-2003 format preview available. Download for full content.</p>");
            html.append("</div>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Escape HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
