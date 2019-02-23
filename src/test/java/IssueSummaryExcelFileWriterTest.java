import com.chesapeake.technology.excel.AExcelFileWriter;
import com.chesapeake.technology.excel.ExcelFileWriter;
import com.chesapeake.technology.excel.IssueSummaryExcelFileWriter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.rcarz.jiraclient.Issue;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class IssueSummaryExcelFileWriterTest extends AExcelFileWriterTest
{
    private Config config = ConfigFactory.parseResources("issue.summary.test.conf")
            .withFallback(ConfigFactory.parseResources("default.test.conf"));

    @Test(groups = "content")
    public void testSheetConfigurations() throws Exception
    {
        excelFileWriter.createJIRAReport(config);

        XSSFWorkbook workbook = getWorkbook();

        Assert.assertEquals(workbook.getNumberOfSheets(), 1);
    }

    @Test(dependsOnMethods = {"testSheetConfigurations"}, groups = "content")
    public void testGenerateReport() throws Exception
    {
        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        Assert.assertEquals(sheet.getPhysicalNumberOfRows(), getExpectedNumberOfRows(true, true));
    }

    @Test(dependsOnMethods = {"testSheetConfigurations"}, groups = "content")
    public void testNoEmptyCells() throws Exception
    {
        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        for (int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);

            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++)
            {
                Cell cell = row.getCell(columnIndex);

                Assert.assertNotNull(cell);
                Assert.assertFalse(AExcelFileWriter.isCellEmpty(cell));
            }
        }
    }

    @Test(dependsOnMethods = {"testSheetConfigurations"}, groups = "content")
    public void testTextWrap() throws Exception
    {
        testTextWrap(true);
    }

    @Test(dependsOnMethods = {"testSheetConfigurations"}, groups = "content")
    public void testAllIssuesIncluded() throws Exception
    {
        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        Set<String> issueKeys = initiativeEpicMap.keySet().stream()
                .map(Issue::getKey)
                .collect(Collectors.toSet());

        issueKeys.addAll(epicStoryMap.keySet().stream()
                .map(Issue::getKey)
                .collect(Collectors.toSet()));

        issueKeys.addAll(epicStoryMap.values().stream()
                .flatMap(Collection::stream)
                .map(Issue::getKey)
                .collect(Collectors.toSet()));

        //The excel row numbers start at 0 but the first row is headers so the first key begins at row 1. Because the
        //headers start at 1, the number of excel rows should match the issueKey's size exactly.
        Assert.assertEquals(sheet.getLastRowNum(), issueKeys.size());

        for (int rowIndex = 1; rowIndex < sheet.getLastRowNum(); rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            Cell issueKey = row.getCell(IssueSummaryExcelFileWriter.KEY_COLUMN);

            Assert.assertTrue(issueKeys.contains(issueKey.getStringCellValue()), "Unexpected key detected in excel file");
        }
    }

    @Test(dependsOnGroups = "content")
    public void testHideAllColumns() throws Exception
    {
        Map<String, Object> customizations = new HashMap<>();
        Row row = getWorkbook().getSheetAt(0).getRow(0);
        List<String> hiddenColumns = new ArrayList<>();

        for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++)
        {
            hiddenColumns.add(row.getCell(columnIndex).getStringCellValue());
        }

        customizations.put("jira.sheets.Issue Summary.columns.hidden", hiddenColumns);
        Config config = ConfigFactory.parseMap(customizations)
                .withFallback(ConfigFactory.parseResources("issue.summary.test.conf"))
                .withFallback(ConfigFactory.parseResources("default.test.conf"))
                .resolve();

        deleteOldReports();

        excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, customFieldMapping);

        initializeActiveData();
        excelFileWriter.createJIRAReport(config);

        Row hiddenRow = getWorkbook().getSheetAt(0).getRow(0);

        Sheet sheet = getWorkbook().getSheetAt(0);

        for (int columnIndex = 0; columnIndex < hiddenRow.getLastCellNum(); columnIndex++)
        {
            Assert.assertTrue(sheet.isColumnHidden(columnIndex), " Expected column "
                    + row.getCell(columnIndex).getStringCellValue() + " to be hidden ");
        }
    }

    @Test(dependsOnGroups = "content", groups = {"destructive"})
    public void testHideEmptyInitiatives() throws Exception
    {
        deleteOldReports();
        Map<String, Object> customizations = new HashMap<>();

        customizations.put("jira.filters.hideEmptyInitiatives", true);
        Config config = ConfigFactory.parseMap(customizations)
                .withFallback(ConfigFactory.parseResources("issue.summary.test.conf"))
                .withFallback(ConfigFactory.parseResources("default.test.conf"))
                .resolve();

        excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, customFieldMapping);

        initializeActiveData();
        excelFileWriter.createJIRAReport(config);

        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        Assert.assertEquals(sheet.getPhysicalNumberOfRows(), getExpectedNumberOfRows(false, true));
    }

    @Test(dependsOnGroups = {"content", "destructive"})
    public void testHideEmptyEpics() throws Exception
    {
        deleteOldReports();
        Map<String, Object> customizations = new HashMap<>();

        customizations.put("jira.filters.hideEmptyEpics", true);
        Config config = ConfigFactory.parseMap(customizations)
                .withFallback(ConfigFactory.parseResources("issue.summary.test.conf"))
                .withFallback(ConfigFactory.parseResources("default.test.conf"))
                .resolve();

        excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, customFieldMapping);

        initializeActiveData();
        excelFileWriter.createJIRAReport(config);

        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        Assert.assertEquals(sheet.getPhysicalNumberOfRows(), getExpectedNumberOfRows(true, false));
    }

    @Test(dependsOnGroups = {"content", "destructive"})
    public void testWithoutTextWrap() throws Exception
    {
        deleteOldReports();
        Map<String, Object> customizations = new HashMap<>();

        customizations.put("jira.sheets.Issue Summary.wrapText", false);
        Config config = ConfigFactory.parseMap(customizations)
                .withFallback(ConfigFactory.parseResources("issue.summary.test.conf"))
                .withFallback(ConfigFactory.parseResources("default.test.conf"))
                .resolve();

        excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, customFieldMapping);

        initializeActiveData();
        excelFileWriter.createJIRAReport(config);

        testTextWrap(false);
    }

    private void testTextWrap(boolean textWrap) throws Exception
    {
        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        for (int rowIndex = 1; rowIndex < sheet.getLastRowNum(); rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            Cell cell = row.getCell(IssueSummaryExcelFileWriter.DESCRIPTION_COLUMN);

            Assert.assertNotNull(cell);

            Assert.assertEquals(cell.getCellStyle().getWrapText(), textWrap);
        }
    }

    private XSSFWorkbook getWorkbook() throws IOException, InvalidFormatException
    {
        File reportDirectory = new File(reportDirectoryPath);
        File[] reports = reportDirectory.listFiles();

        return new XSSFWorkbook(reports[0]);
    }

    private int getExpectedNumberOfRows(boolean includeEmptyInitiatives, boolean includeEmptyEpics)
    {
        int numStories = epicStoryMap.values().stream()
                .mapToInt(Collection::size)
                .sum();

        //Add 1 for the title row
        int expectedNumberOfRows = initiativeEpicMap.keySet().size() + epicStoryMap.keySet().size() + numStories + 1;

        if (!includeEmptyInitiatives)
        {
            expectedNumberOfRows -= initiativeEpicMap.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty())
                    .count();
        }
        if (!includeEmptyEpics)
        {
            expectedNumberOfRows -= epicStoryMap.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty())
                    .count();
        }

        return expectedNumberOfRows;
    }
}
