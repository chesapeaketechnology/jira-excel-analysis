package com.chesapeaketechnology.excel;

import com.chesapeaketechnology.JiraRestClient;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarkerStyle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates an excel sheet that helps developers analyze their historical estimation capabilities.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
class DeveloperExcelFileWriter extends AExcelFileWriter
{
    private static final int DEVELOPER_COLUMN = 0;
    private static final int SPRINT_COLUMN = 1;
    private static final int SPRINT_COMMITMENT_COLUMN = 2;
    private static final int COMPLETED_POINTS_COLUMN = 3;
    private static final int POINTS_ADDED_COLUMN = 4;
    private static final int AVERAGE_TICKET_SIZE_COLUMN = 5;
    private static final int COMMITMENT_DELTA_SIZE_COLUMN = 6;
    private static final int NUMBER_OF_ROWS_PER_DEVELOPER = 25;

    private final XSSFSheet developerSheet;
    private final SimpleDateFormat jiraDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final SimpleDateFormat sprintStartDateFormat = new SimpleDateFormat("MM/yy (W)");

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Initializes an excel sheet to store developer analytic information.
     *
     * @param workbook          High level representation of a SpreadsheetML workbook.
     * @param initiativeEpicMap Map of initiative issues to the epics that compose them.
     * @param epicStoryMap      Map of epic issues to the user stories that compose them.
     */
    DeveloperExcelFileWriter(XSSFWorkbook workbook, Map<Issue, List<Issue>> initiativeEpicMap,
                             Map<Issue, List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap)
    {
        super(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

        developerSheet = workbook.createSheet("Team Metrics");
    }

    /**
     * Adds metadata about developers jira ticket loads and completion rates across multiple sprints.
     */
    void createJIRAReport()
    {
        developerSheet.setRowSumsBelow(false);

        configureDeveloperSheet();

        developerSheet.autoSizeColumn(DEVELOPER_COLUMN);
        developerSheet.autoSizeColumn(SPRINT_COLUMN);
        developerSheet.autoSizeColumn(SPRINT_COMMITMENT_COLUMN, true);
        developerSheet.autoSizeColumn(COMPLETED_POINTS_COLUMN, true);
        developerSheet.autoSizeColumn(POINTS_ADDED_COLUMN, true);
        developerSheet.autoSizeColumn(AVERAGE_TICKET_SIZE_COLUMN, true);
        developerSheet.autoSizeColumn(COMMITMENT_DELTA_SIZE_COLUMN, true);
    }

    /**
     * Creates a row of cells that include the column headers for the report.
     */
    private void addDeveloperHeaders()
    {
        Row titleRow = developerSheet.createRow(0);
        Cell userCell = titleRow.createCell(DEVELOPER_COLUMN);
        Cell sprintCell = titleRow.createCell(SPRINT_COLUMN);
        Cell sprintCommitmentCell = titleRow.createCell(SPRINT_COMMITMENT_COLUMN);
        Cell completedPointsCell = titleRow.createCell(COMPLETED_POINTS_COLUMN);
        Cell pointsAddedCell = titleRow.createCell(POINTS_ADDED_COLUMN);
        Cell averageCell = titleRow.createCell(AVERAGE_TICKET_SIZE_COLUMN);
        Cell deltaCell = titleRow.createCell(COMMITMENT_DELTA_SIZE_COLUMN);

        userCell.setCellValue("User");
        sprintCell.setCellValue("Sprint Month-Year (Week)");
        sprintCommitmentCell.setCellValue("Initial Sprint Commitment");
        completedPointsCell.setCellValue("Completed # of Points");
        pointsAddedCell.setCellValue("# of Points Added");
        averageCell.setCellValue("Average Ticket Size");
        deltaCell.setCellValue("Deltas");

        for (int i = 0; i <= COMMITMENT_DELTA_SIZE_COLUMN; i++)
        {
            titleRow.getCell(i).setCellStyle(titleStyle);
        }
    }

    /**
     * Creates the body of the excel report that includes all of the information about the developers. If there is any
     * data included then or more charts will be appended adjacent to series of data.
     */
    private void configureDeveloperSheet()
    {
        int row = 1;

        addDeveloperHeaders();

        List<Map.Entry<String, Date>> entries = new ArrayList<>(sprintDateMap.entrySet());

        List<String> sprints = new ArrayList<>();

        for (String username : getUsernames())
        {
            int startingRow = row;

            Row excelRow = developerSheet.createRow(row);

            excelRow.createCell(DEVELOPER_COLUMN).setCellValue(username);

            for (Map.Entry<String, Date> entry : entries)
            {
                if (sprintDateMap.containsKey(entry.getKey()))
                {
                    if (!sprints.contains(entry.getKey()))
                    {
                        sprints.add(entry.getKey());
                    }

                    Collection<Issue> filteredIssues = getUserSpecificIssues(username, sprintStoryBreakdown.get(entry.getKey()));

                    if (filteredIssues.size() > 0)
                    {
                        if (developerSheet.getRow(row) == null)
                        {
                            excelRow = developerSheet.createRow(row++);
                        } else
                        {
                            row++;
                        }
                        Cell sprintCell = excelRow.createCell(SPRINT_COLUMN);
                        Cell pointsEstimateCell = excelRow.createCell(SPRINT_COMMITMENT_COLUMN);
                        Cell pointsCompleteCell = excelRow.createCell(COMPLETED_POINTS_COLUMN);
                        Cell pointsAddedCell = excelRow.createCell(POINTS_ADDED_COLUMN);
                        Cell averageCell = excelRow.createCell(AVERAGE_TICKET_SIZE_COLUMN);
                        Cell deltaCell = excelRow.createCell(COMMITMENT_DELTA_SIZE_COLUMN);

                        Calendar sprintStartCalendar = Calendar.getInstance();
                        sprintStartCalendar.setTime(sprintDateMap.get(entry.getKey()));
                        sprintStartCalendar.add(Calendar.DAY_OF_WEEK, 1);

                        Collection<Issue> originalIssues = getOriginalIssues(filteredIssues, entry.getKey(), sprintStartCalendar.getTime());
                        int numStoryPointsAtStart = getNumStoryPoints(originalIssues);
                        int numStoryPointsAdded = getNumStoryPoints(filteredIssues) - numStoryPointsAtStart;
                        int numStoryPointsCompleted = getNumStoryPointsCompleted(filteredIssues, entry.getKey());
                        int delta = numStoryPointsCompleted - numStoryPointsAtStart;
                        double averageTicketSize = getAverageTicketSize(filteredIssues);

                        sprintCell.setCellValue(sprintStartDateFormat.format(entry.getValue()));
                        pointsEstimateCell.setCellValue(numStoryPointsAtStart);
                        pointsCompleteCell.setCellValue(numStoryPointsCompleted);
                        pointsAddedCell.setCellValue(numStoryPointsAdded);
                        averageCell.setCellValue(Double.parseDouble(decimalFormat.format(averageTicketSize)));
                        deltaCell.setCellValue(delta);
                    }
                }
            }

            if (developerSheet.getLastRowNum() > 1)
            {
                generateLineChart(startingRow);
            }

            //Create groups of rows to add a buffer between user's data
            while (row % NUMBER_OF_ROWS_PER_DEVELOPER != 0)
            {
                row++;
            }
        }

        List<String> sprintDates = sprintDateMap.values().stream().map(sprintStartDateFormat::format).collect(Collectors.toList());

        addTeamMetricsData(sprintDates, row);
    }

    private void addTeamMetricsData(List<String> sprintDates, int row)
    {
        if (developerSheet.getLastRowNum() > 1)
        {

            for (int i = 0; i < sprintDates.size(); i++)
            {
                Row excelRow = developerSheet.createRow(row + i);

                if (i == 0)
                {
                    excelRow.createCell(DEVELOPER_COLUMN).setCellValue("Team");
                }

                Cell sprintCell = excelRow.createCell(SPRINT_COLUMN);
                Cell pointsEstimateCell = excelRow.createCell(SPRINT_COMMITMENT_COLUMN);
                Cell pointsCompleteCell = excelRow.createCell(COMPLETED_POINTS_COLUMN);
                Cell pointsAddedCell = excelRow.createCell(POINTS_ADDED_COLUMN);
                Cell averageCell = excelRow.createCell(AVERAGE_TICKET_SIZE_COLUMN);
                Cell deltaCell = excelRow.createCell(COMMITMENT_DELTA_SIZE_COLUMN);

                String sprintName = sprintDates.get(i);

                sprintCell.setCellValue(sprintName);
                pointsEstimateCell.setCellValue(getSumOfSprintValues(sprintName, SPRINT_COMMITMENT_COLUMN, row));
                pointsCompleteCell.setCellValue(getSumOfSprintValues(sprintName, COMPLETED_POINTS_COLUMN, row));
                pointsAddedCell.setCellValue(getSumOfSprintValues(sprintName, POINTS_ADDED_COLUMN, row));
                averageCell.setCellValue(getAverageTicketSizeForSprint(sprintName, AVERAGE_TICKET_SIZE_COLUMN, row));
                deltaCell.setCellValue(getSumOfSprintValues(sprintName, COMMITMENT_DELTA_SIZE_COLUMN, row));
            }

            generateLineChart(row);
        }
    }

    private int getSumOfSprintValues(String sprintName, int column, int lastRow)
    {
        AtomicInteger sum = new AtomicInteger(0);

        return getAggregateSprintData(sprintName, column, lastRow, sum::addAndGet);
    }

    private double getAverageTicketSizeForSprint(String sprintName, int column, int lastRow)
    {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger sum = new AtomicInteger(0);

        getAggregateSprintData(sprintName, column, lastRow, value -> {
            count.incrementAndGet();
            return sum.addAndGet(value);
        });

        return (double) sum.get() / count.get();
    }

    private int getAggregateSprintData(String sprintName, int column, int lastRow, Function<Integer, Integer> aggregator)
    {
        int sum = 0;

        for (int rowIndex = 0; rowIndex < lastRow; rowIndex++)
        {
            Row row = developerSheet.getRow(rowIndex);

            if (row != null)
            {
                Cell sprintCell = row.getCell(SPRINT_COLUMN);

                if (sprintCell != null && sprintCell.getStringCellValue().equals(sprintName))
                {

                    Cell cell = row.getCell(column);

                    if (cell != null)
                    {
                        sum += cell.getNumericCellValue();
                    }
                }
            }
        }

        return sum;
    }

    /**
     * Gets the subset of issues that are assigned to {@code username} and have a least one label from the list of
     * active labels.
     *
     * @param username  The display name of the user to find tickets assigned to.
     * @param allIssues The collection of JIRA issues to filter.
     * @return A subset of JIRA issues that are assigned to {@code username} and that active filter constraints.
     */
    private Collection<Issue> getUserSpecificIssues(String username, Set<Issue> allIssues)
    {
        return allIssues.stream()
                .filter(issue -> issue.getAssignee().getDisplayName().equals(username))
                .collect(Collectors.toList());
    }

    /**
     * Get a unique set of user's display names that are assigned to active tickets.
     *
     * @return A unique set of user's display names that are assigned to active tickets.
     */
    private Set<String> getUsernames()
    {
        return sprintStoryBreakdown.values().stream()
                .flatMap(Collection::stream)
                .filter(this::containsActiveLabels)
                .map(Issue::getAssignee)
                .filter(Objects::nonNull)
                .map(User::getDisplayName)
                .collect(Collectors.toSet());
    }

    /**
     * Determines if ANY filter defined labels are present within {@code issue}.
     *
     * @param issue The JIRA ticket to test for the presence of labels.
     * @return {@code true} if ANY filter defined labels are present; otherwise {@code false}.
     */
    private boolean containsActiveLabels(Issue issue)
    {
        return activeLabels.isEmpty() || activeLabels.stream().anyMatch(label -> issue.getLabels().contains(label));
    }

    /**
     * Draws a line chart using a combination of metadata and data from excel rows.
     *
     * @param dataStartingRow The first row that data should be pulled from.
     */
    private void generateLineChart(int dataStartingRow)
    {
        XSSFDrawing drawing = developerSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, COMMITMENT_DELTA_SIZE_COLUMN + 2, dataStartingRow,
                AVERAGE_TICKET_SIZE_COLUMN + 15, dataStartingRow + 19);

        XSSFChart chart = drawing.createChart(anchor);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(org.apache.poi.xddf.usermodel.chart.LegendPosition.RIGHT);

        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        XDDFChartData chartData = chart.createData(ChartTypes.LINE, categoryAxis, valueAxis);

        valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        int lastRow = dataStartingRow;

        while (!isRowEmpty(lastRow))
        {
            lastRow++;
        }

        lastRow -= 1;

        XDDFNumericalDataSource<?> sprintDataSource = XDDFDataSourcesFactory.fromNumericCellRange(developerSheet,
                new CellRangeAddress(dataStartingRow, lastRow, SPRINT_COLUMN, SPRINT_COLUMN));
        XDDFNumericalDataSource<Double> completedDataSource = XDDFDataSourcesFactory.fromNumericCellRange(developerSheet,
                new CellRangeAddress(dataStartingRow, lastRow, COMPLETED_POINTS_COLUMN, COMPLETED_POINTS_COLUMN));
        XDDFNumericalDataSource<Double> commitmentDataSource = XDDFDataSourcesFactory.fromNumericCellRange(developerSheet,
                new CellRangeAddress(dataStartingRow, lastRow, SPRINT_COMMITMENT_COLUMN, SPRINT_COMMITMENT_COLUMN));
        XDDFNumericalDataSource<Double> pointsAddedDataSource = XDDFDataSourcesFactory.fromNumericCellRange(developerSheet,
                new CellRangeAddress(dataStartingRow, lastRow, POINTS_ADDED_COLUMN, POINTS_ADDED_COLUMN));
        XDDFNumericalDataSource<Double> averageTicketSizeSource = XDDFDataSourcesFactory.fromNumericCellRange(developerSheet,
                new CellRangeAddress(dataStartingRow, lastRow, AVERAGE_TICKET_SIZE_COLUMN, AVERAGE_TICKET_SIZE_COLUMN));

        XDDFChartData.Series completedSeries = chartData.addSeries(sprintDataSource, completedDataSource);
        XDDFChartData.Series commitmentSeries = chartData.addSeries(sprintDataSource, commitmentDataSource);
        XDDFChartData.Series pointsAddedSeries = chartData.addSeries(sprintDataSource, pointsAddedDataSource);
        XDDFChartData.Series averageTicketSizeSeries = chartData.addSeries(sprintDataSource, averageTicketSizeSource);

        completedSeries.setTitle("Completed", null);
        commitmentSeries.setTitle("Commitment", null);
        pointsAddedSeries.setTitle("Points Added", null);
        averageTicketSizeSeries.setTitle("Average Size", null);

        categoryAxis.setMaximum(15);

        categoryAxis.getOrAddMajorGridProperties();
        categoryAxis.getOrAddShapeProperties();

        completedSeries.plot();
        commitmentSeries.plot();
        pointsAddedSeries.plot();
        averageTicketSizeSeries.plot();

        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        plotArea.getLineChartArray(0).getSmooth();
        CTBoolean ctBool = CTBoolean.Factory.newInstance();
        ctBool.setVal(false);
        plotArea.getLineChartArray(0).setSmooth(ctBool);

        for (CTLineSer ser : plotArea.getLineChartArray(0).getSerList())
        {
            ser.setSmooth(ctBool);
        }

        CTMarker ctMarker = CTMarker.Factory.newInstance();
        ctMarker.setSymbol(CTMarkerStyle.Factory.newInstance());

        for (CTLineSer ser : plotArea.getLineChartList().get(0).getSerList())
        {
            ser.setMarker(ctMarker);
        }
    }

    private boolean isRowEmpty(int rowNum)
    {
        Row row = developerSheet.getRow(rowNum);

        return row == null || row.getCell(SPRINT_COLUMN) == null;
    }

    /**
     * Get the average number of points from {@code issues}
     *
     * @param issues The JIRA tickets to extract the average number of story points from.
     * @return Get the average number of points from {@code issues}
     */
    private double getAverageTicketSize(Collection<Issue> issues)
    {
        return issues.stream()
                .mapToInt(issue -> getNumStoryPoints(Collections.singleton(issue)))
                .average().orElse(0);
    }

    /**
     * Determines the number of story points from issues that are completed. A ticket is considered resolved if the
     * issue's resolution date is before the sprint's completion date. The Sprint's completion date is determined from
     * the issue's work history.
     *
     * @param issues     The issues to count the story points of.
     * @param sprintName The name of the sprint to get the completion date of.
     * @return Determines the number of story points from issues that are completed.
     */
    private int getNumStoryPointsCompleted(Collection<Issue> issues, String sprintName)
    {
        return issues.stream()
                .mapToInt(issue -> {
                    long completionTime = getCompletionDateMilliseconds(issue, sprintName);

                    if (issue.getResolutionDate() != null && issue.getResolutionDate().getTime() < completionTime)
                    {
                        return getNumStoryPoints(Collections.singleton(issue));
                    }

                    return 0;
                }).sum();
    }

    /**
     * Gets the completion date of the sprint whose name matches {@code sprintName}.
     *
     * @param issue      A jira ticket which includes a work history from which the sprint's com
     * @param sprintName The unique identifier for an assigned set of tickets.
     * @return The completion date of the sprint in milliseconds.
     */
    private long getCompletionDateMilliseconds(Issue issue, String sprintName)
    {
        String customSprintId = fieldCustomIdMap.get(JiraRestClient.SPRINT_KEY);
        List<Properties> properties = JiraRestClient.getSprintProperties(issue, customSprintId);

        jiraDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        AtomicLong time = new AtomicLong();

        properties.forEach(propertySet -> {
            if (sprintName.equals(propertySet.get("name")))
            {
                String completionDate = propertySet.get("endDate").toString();
                if (completionDate != null && !completionDate.contains("null"))
                {
                    try
                    {
                        time.set(jiraDateFormat.parse(completionDate).getTime());
                    } catch (ParseException exception)
                    {
                        logger.warn("Failed to parse time: ", exception);
                    }
                }
            }
        });

        return time.get();
    }

    /**
     * Determines if an issue was added after a sprint was started
     *
     * @param issue           The JIRA ticket to evaluate.
     * @param sprintName      The name of the sprint from which the start date was pulled.
     * @param sprintStartDate The date at which the sprint started.
     * @return {@code true} if the {@code issue} was added after {@code sprintStartDate}.
     */
    private boolean isAddedIssue(Issue issue, String sprintName, Date sprintStartDate)
    {
        return issue.getChangeLog() != null && issue.getChangeLog().getEntries().stream()
                .filter(entry -> entry.getCreated().after(sprintStartDate))
                .anyMatch(entry -> entry.getItems().stream().anyMatch(item -> sprintName.equals(item.getToString())));
    }

    /**
     * Gets the total number of story points across a collection of issues.
     *
     * @param issues The issues to whose story points will be summed.
     * @return The total number of story points across a collection of issues.
     */
    private int getNumStoryPoints(Collection<Issue> issues)
    {
        int sum = 0;

        String storyPointCustomId = fieldCustomIdMap.get(JiraRestClient.STORY_POINTS_KEY);

        for (Issue issue : issues)
        {
            Object points = issue.getField(storyPointCustomId);

            if (points != null && !points.equals("null"))
            {
                sum += Double.valueOf(points.toString()).intValue();
            }
        }

        return sum;
    }

    /**
     * Gets the subset of issues from a sprint that were added before the sprint's start date.
     *
     * @param issues          A collection of JIRA tickets to filter based on the date they were added to a sprint
     * @param sprintName      The name of a the sprint whose start date is being compared against.
     * @param sprintStartDate The date at which the sprint started.
     * @return The subset of issues from a sprint that were added before the sprint's start date.
     */
    private Collection<Issue> getOriginalIssues(Collection<Issue> issues, String sprintName, Date sprintStartDate)
    {
        Collection<Issue> originalIssues = new ArrayList<>();

        for (Issue issue : issues)
        {
            if (!isAddedIssue(issue, sprintName, sprintStartDate))
            {
                originalIssues.add(issue);
            }
        }

        return originalIssues;
    }
}
