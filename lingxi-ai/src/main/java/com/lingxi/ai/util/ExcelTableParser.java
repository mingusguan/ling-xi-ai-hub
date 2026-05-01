package com.lingxi.ai.util;

import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel/CSV 表格解析工具。
 */
public final class ExcelTableParser
{
    private static final Logger log = LoggerFactory.getLogger(ExcelTableParser.class);
    private static final int MAX_SHEETS = 3;
    private static final int MAX_ROWS_PER_SHEET = 30;
    private static final int MAX_COLS = 10;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.CHINA);

    private ExcelTableParser()
    {
    }

    /**
     * 将表格文件解析为适合送给大模型的文本预览。
     *
     * @param file 上传文件
     * @return 预览文本
     */
    public static String parse(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请上传报表文件");
        }
        String fileName = StringUtils.defaultString(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".csv"))
        {
            return parseCsv(file);
        }
        return parseWorkbook(file);
    }

    private static String parseCsv(MultipartFile file)
    {
        try
        {
            String content = new String(file.getBytes());
            if (StringUtils.isBlank(content))
            {
                throw new ServiceException("上传的 CSV 内容为空");
            }
            return truncate(content, 12000);
        }
        catch (IOException e)
        {
            log.error("解析 CSV 报表失败", e);
            throw new ServiceException("解析 CSV 报表失败");
        }
    }

    private static String parseWorkbook(MultipartFile file)
    {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream))
        {
            StringBuilder builder = new StringBuilder(4096);
            int sheetLimit = Math.min(workbook.getNumberOfSheets(), MAX_SHEETS);
            for (int i = 0; i < sheetLimit; i++)
            {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null)
                {
                    continue;
                }
                builder.append("工作表: ").append(sheet.getSheetName()).append('\n');
                appendSheet(builder, workbook, sheet);
                builder.append('\n');
            }
            String preview = builder.toString().trim();
            if (StringUtils.isBlank(preview))
            {
                throw new ServiceException("上传的报表内容为空");
            }
            return truncate(preview, 12000);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("解析 Excel 报表失败", e);
            throw new ServiceException("解析 Excel 报表失败，请确认文件格式正确");
        }
    }

    private static void appendSheet(StringBuilder builder, Workbook workbook, Sheet sheet)
    {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        int maxRowNum = Math.min(sheet.getLastRowNum(), MAX_ROWS_PER_SHEET - 1);
        for (int rowIndex = 0; rowIndex <= maxRowNum; rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            if (row == null)
            {
                continue;
            }
            List<String> cells = new ArrayList<>();
            int maxCellNum = Math.min(Math.max(row.getLastCellNum(), 0), MAX_COLS);
            for (int cellIndex = 0; cellIndex < maxCellNum; cellIndex++)
            {
                Cell cell = row.getCell(cellIndex);
                cells.add(readCell(cell, evaluator));
            }
            if (cells.stream().allMatch(StringUtils::isBlank))
            {
                continue;
            }
            builder.append(String.join(" | ", cells)).append('\n');
        }
    }

    private static String readCell(Cell cell, FormulaEvaluator evaluator)
    {
        if (cell == null)
        {
            return "";
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA)
        {
            cellType = evaluator.evaluateFormulaCell(cell);
        }
        if (cellType == CellType.NUMERIC)
        {
            try
            {
                BigDecimal number = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
                return number.toPlainString();
            }
            catch (Exception ignored)
            {
                return DATA_FORMATTER.formatCellValue(cell, evaluator);
            }
        }
        return DATA_FORMATTER.formatCellValue(cell, evaluator);
    }

    private static String truncate(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
