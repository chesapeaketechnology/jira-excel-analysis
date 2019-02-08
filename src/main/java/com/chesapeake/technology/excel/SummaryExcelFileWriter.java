package com.chesapeake.technology.excel;

import net.rcarz.jiraclient.Issue;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLegend;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.STAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.STBarDir;
import org.openxmlformats.schemas.drawingml.x2006.chart.STLegendPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.STOrientation;
import org.openxmlformats.schemas.drawingml.x2006.chart.STTickLblPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Generates an excel sheet that decomposes initiatives into epics and further into user stories.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
class SummaryExcelFileWriter extends AExcelFileWriter
{
    private XSSFSheet excelSheet;
    private Map<Issue, Double> initiativeCompletions = new HashMap<>();
    private Map<Issue, Double> epicCompletions = new HashMap<>();

    private static final int INITIATIVE_NAME_COLUMN = 0;
    private static final int INITIATIVE_VALUE_COLUMN = 1;
    private static final int EPIC_NAME_COLUMN = 2;
    private static final int EPIC_VALUE_COLUMN = 3;

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Creates an excel sheet and initializes fonts and styles.
     *
     * @param workbook          A excel sheet wrapper.
     * @param initiativeEpicMap A decomposition of initiatives into epics.
     * @param epicStoryMap      A decomposition of epics into initiatives.
     */
    SummaryExcelFileWriter(XSSFWorkbook workbook, Map<Issue, List<Issue>> initiativeEpicMap,
                           Map<Issue, List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap)
    {
        super(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

        excelSheet = workbook.createSheet("Issue Completion Rates");
    }

    void generateReport()
    {
        updateCompletionRates();
        writeAnaltyicsToExcel();
        excelSheet.setDefaultColumnWidth(30);
        excelSheet.autoSizeColumn(1);
        excelSheet.autoSizeColumn(3);
    }

    /**
     * Adds a summary of initatives, epics, and user stories to an excel sheet.
     */
    private void updateCompletionRates()
    {
        logger.info("Start calculation of completion rates");

        if (activeInitiatives.isEmpty())
        {
            for (Map.Entry<Issue, List<Issue>> initiativeEntry : getInitativeEntriesMap())
            {
                if (activeEpics.isEmpty())
                {
                    for (Issue epicIssue : initiativeEntry.getValue())
                    {
                        if (activeEpics.isEmpty() || activeEpics.contains(epicIssue))
                        {
                            epicCompletions.put(epicIssue, getPercentComplete(epicIssue));
                        }
                    }
                }

                initiativeCompletions.put(initiativeEntry.getKey(), getPercentComplete(initiativeEntry.getKey()));
            }
        }

        for (Issue initiativeIssue : activeInitiatives)
        {
            initiativeCompletions.put(initiativeIssue, getPercentComplete(initiativeIssue));
        }
        for (Issue epicIssue : activeEpics)
        {
            if (activeEpics.isEmpty() || activeEpics.contains(epicIssue))
            {
                epicCompletions.put(epicIssue, getPercentComplete(epicIssue));
            }
        }

        logger.info("End calculation of completion rates");
    }

    /**
     * Get's the progress stage that a ticket is in.
     *
     * @param issue The issue to retrieve information from.
     * @return The progress stage that a ticket is in.
     */
    private Double getPercentComplete(Issue issue)
    {
        Set<Issue> allNestedStories = getAllNestedIssues(issue);

        double completedNestedStories = allNestedStories.stream().filter(completedIssues::contains).count();

        return completedNestedStories / allNestedStories.size();
    }

    /**
     * Writes the developer metrics to the excel sheet and appends charts to visualize the data.
     */
    private void writeAnaltyicsToExcel()
    {
        //put some data in the sheet
        Row row;

        row = excelSheet.createRow(0);
        row.createCell(INITIATIVE_NAME_COLUMN);
        row.createCell(INITIATIVE_VALUE_COLUMN).setCellValue("Initiative");
        row.createCell(EPIC_NAME_COLUMN); //Epic Name Column
        row.createCell(EPIC_VALUE_COLUMN).setCellValue("Epic");

        initiativeCompletions.forEach(new BiConsumer<Issue, Double>()
        {
            int rowIndex = 1;

            @Override
            public void accept(Issue issue, Double percentComplete)
            {
                Row row = excelSheet.createRow(rowIndex++);
                Cell nameCell = row.createCell(0);
                Cell valueCell = row.createCell(1);

                nameCell.setCellValue(issue.getSummary());
                valueCell.setCellValue(percentComplete);
                valueCell.setCellStyle(wrapStyle);
            }
        });

        epicCompletions.forEach(new BiConsumer<Issue, Double>()
        {
            int rowIndex = 1;

            @Override
            public void accept(Issue issue, Double percentComplete)
            {
                Row row = excelSheet.getRow(rowIndex++);

                if (row == null)
                {
                    row = excelSheet.createRow(rowIndex - 1);
                }
                Cell nameCell = row.createCell(2);
                Cell valueCell = row.createCell(3);

                nameCell.setCellValue(issue.getSummary());
                valueCell.setCellValue(percentComplete);
                valueCell.setCellStyle(wrapStyle);
            }
        });

        //create empty chart in the sheet
        int numInitiativeAnchorColumns = (getNextEmptyInitiativeRow() - 1) / 2 + 4;
        int numEpicAnchorColumns = (getNextEmptyEpicRow() - 1) / 2 + 4;

        Drawing drawing = excelSheet.createDrawingPatriarch();
        ClientAnchor initiativeAnchor = drawing.createAnchor(0, 0, 0, 0, 4, 2, numInitiativeAnchorColumns, 20);
        ClientAnchor epicAnchor = drawing.createAnchor(0, 0, 0, 0, 4, 22, numEpicAnchorColumns, 42);
        XSSFChart initiativeChart = ((XSSFDrawing) drawing).createChart(initiativeAnchor);
        XSSFChart epicChart = ((XSSFDrawing) drawing).createChart(epicAnchor);

        //create the references to the chart data
        CellReference firstInitiativeDataCell = new CellReference(excelSheet.getSheetName(), 0, 0, true, true);
        CellReference lastInitativeDataCell = new CellReference(excelSheet.getSheetName(), getNextEmptyInitiativeRow() - 1, 1, true, true);

        //create the references to the chart data
        CellReference firstEpicDataCell = new CellReference(excelSheet.getSheetName(), 0, 2, true, true);
        CellReference lastEpicDataCell = new CellReference(excelSheet.getSheetName(), getNextEmptyEpicRow() - 1, 3, true, true);

        addChart(initiativeChart, "Initiatives", firstInitiativeDataCell, lastInitativeDataCell);
        addChart(epicChart, "Epics", firstEpicDataCell, lastEpicDataCell);
    }

    /**
     * Gets the first empty row index.
     *
     * @return The first empty row index.
     */
    private int getNextEmptyInitiativeRow()
    {
        for (int i = 0; i < Integer.MAX_VALUE; i++)
        {
            Row row = excelSheet.getRow(i);

            if (row == null || row.getCell(0) == null)
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * Get the first row with an empty
     *
     * @return
     */
    private int getNextEmptyEpicRow()
    {
        for (int i = 0; i < Integer.MAX_VALUE; i++)
        {
            Row row = excelSheet.getRow(i);

            if (row == null || row.getCell(2) == null)
            {
                return i;
            }
        }

        return -1;
    }

    private void addChart(XSSFChart chart, String title, CellReference firstDataCell, CellReference lastDataCell)
    {
        //create a default bar chart from the data
        CTChart ctBarChart = createDefaultBarChart(chart, firstDataCell, lastDataCell, true);

        //now we can customizing the chart

        //data labels:
        CTBoolean ctboolean = CTBoolean.Factory.newInstance();
        ctboolean.setVal(true);
        ctBarChart.getPlotArea().getBarChartArray(0).addNewDLbls().setShowVal(ctboolean);
        ctboolean.setVal(false);
        ctBarChart.getPlotArea().getBarChartArray(0).getDLbls().setShowSerName(ctboolean);
        ctBarChart.getPlotArea().getBarChartArray(0).getDLbls().setShowPercent(ctboolean);
        ctBarChart.getPlotArea().getBarChartArray(0).getDLbls().setShowLegendKey(ctboolean);
        ctBarChart.getPlotArea().getBarChartArray(0).getDLbls().setShowCatName(ctboolean);
        ctBarChart.getPlotArea().getBarChartArray(0).getDLbls().setShowLeaderLines(ctboolean);
        ctBarChart.getPlotArea().getBarChartArray(0).getDLbls().setShowBubbleSize(ctboolean);

        //val axis maximum:
        ctBarChart.getPlotArea().getValAxArray(0).getScaling().addNewMax().setVal(1);

        //cat axis title:
        ctBarChart.getPlotArea().getCatAxArray(0).addNewTitle().addNewOverlay().setVal(false);
        ctBarChart.getPlotArea().getCatAxArray(0).getTitle().addNewTx().addNewRich().addNewBodyPr();
        ctBarChart.getPlotArea().getCatAxArray(0).getTitle().getTx().getRich().addNewP().addNewR().setT(title);
        chart.deleteLegend();
    }

    private static CTChart createDefaultBarChart(XSSFChart chart, CellReference firstDataCell, CellReference lastDataCell, boolean seriesInCols)
    {
        CTChart ctChart = chart.getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);

        int firstDataRow = firstDataCell.getRow();
        int lastDataRow = lastDataCell.getRow();
        int firstDataCol = firstDataCell.getCol();
        int lastDataCol = lastDataCell.getCol();
        String dataSheet = firstDataCell.getSheetName();

        int idx = 0;

        if (seriesInCols)
        { //the series are in the columns of the data cells

            for (int c = firstDataCol + 1; c < lastDataCol + 1; c++)
            {
                CTBarSer ctBarSer = ctBarChart.addNewSer();
                CTSerTx ctSerTx = ctBarSer.addNewTx();
                CTStrRef ctStrRef = ctSerTx.addNewStrRef();
                ctStrRef.setF(new CellReference(dataSheet, firstDataRow, c, true, true).formatAsString());

                ctBarSer.addNewIdx().setVal(idx++);
                CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
                ctStrRef = cttAxDataSource.addNewStrRef();

                ctStrRef.setF(new AreaReference(
                        new CellReference(dataSheet, firstDataRow + 1, firstDataCol, true, true),
                        new CellReference(dataSheet, lastDataRow, firstDataCol, true, true),
                        SpreadsheetVersion.EXCEL2007).formatAsString());

                CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
                CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();

                ctNumRef.setF(new AreaReference(
                        new CellReference(dataSheet, firstDataRow + 1, c, true, true),
                        new CellReference(dataSheet, lastDataRow, c, true, true),
                        SpreadsheetVersion.EXCEL2007).formatAsString());

                //at least the border lines in Libreoffice Calc ;-)
                ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[]{0, 0, 0});
            }
        } else
        { //the series are in the rows of the data cells

            for (int r = firstDataRow + 1; r < lastDataRow + 1; r++)
            {
                CTBarSer ctBarSer = ctBarChart.addNewSer();
                CTSerTx ctSerTx = ctBarSer.addNewTx();
                CTStrRef ctStrRef = ctSerTx.addNewStrRef();
                ctStrRef.setF(new CellReference(dataSheet, r, firstDataCol, true, true).formatAsString());

                ctBarSer.addNewIdx().setVal(idx++);
                CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
                ctStrRef = cttAxDataSource.addNewStrRef();

                ctStrRef.setF(new AreaReference(
                        new CellReference(dataSheet, firstDataRow, firstDataCol + 1, true, true),
                        new CellReference(dataSheet, firstDataRow, lastDataCol, true, true),
                        SpreadsheetVersion.EXCEL2007).formatAsString());

                CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
                CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();

                ctNumRef.setF(new AreaReference(
                        new CellReference(dataSheet, r, firstDataCol + 1, true, true),
                        new CellReference(dataSheet, r, lastDataCol, true, true),
                        SpreadsheetVersion.EXCEL2007).formatAsString());

                //at least the border lines in Libreoffice Calc ;-)
                ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[]{0, 0, 0});
            }
        }

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123456);
        ctBarChart.addNewAxId().setVal(123457);

        //cat axis
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123456); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123457); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123457); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123456); //id of the cat axis
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //legend
        CTLegend ctLegend = ctChart.addNewLegend();
        ctLegend.addNewLegendPos().setVal(STLegendPos.B);
        ctLegend.addNewOverlay().setVal(false);

        return ctChart;
    }
}
