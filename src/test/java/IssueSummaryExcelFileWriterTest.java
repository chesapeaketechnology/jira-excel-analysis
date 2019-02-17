import com.chesapeake.technology.excel.AExcelFileWriter;
import com.chesapeake.technology.excel.IssueSummaryExcelFileWriter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.rcarz.jiraclient.Issue;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class IssueSummaryExcelFileWriterTest extends AExcelFileWriterTest
{
    private Config config = ConfigFactory.parseResources("issue.summary.test.conf")
            .withFallback(ConfigFactory.parseResources("default.test.conf"))
            .resolve();

    @Test
    public void testSheetConfigurations() throws Exception
    {
        excelFileWriter.createJIRAReport(config);

        XSSFWorkbook workbook = getWorkbook();

        Assert.assertEquals(workbook.getNumberOfSheets(), 1);
    }

    @Test(dependsOnMethods = {"testSheetConfigurations"})
    public void testGenerateReport() throws Exception
    {
        XSSFWorkbook workbook = getWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);

        int numStories = epicStoryMap.values().stream()
                .mapToInt(Collection::size)
                .sum();

        //Add 1 for the title row
        int expectedNumRowsWithEmptyGroups = initiativeEpicMap.keySet().size() + epicStoryMap.keySet().size() + numStories + 1;

        Assert.assertEquals(sheet.getPhysicalNumberOfRows(), expectedNumRowsWithEmptyGroups);
    }

    @Test(dependsOnMethods = {"testSheetConfigurations"})
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

    @Test(dependsOnMethods = {"testSheetConfigurations"})
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

    private XSSFWorkbook getWorkbook() throws IOException, InvalidFormatException
    {
        File reportDirectory = new File(reportDirectoryPath);
        File report = reportDirectory.listFiles()[0];

        return new XSSFWorkbook(report);
    }
}
