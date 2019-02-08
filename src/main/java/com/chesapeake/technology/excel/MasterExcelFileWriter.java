package com.chesapeake.technology.excel;

import com.chesapeake.technology.JiraRestClient;
import com.chesapeake.technology.model.StoryIssueComparator;
import net.rcarz.jiraclient.Component;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Version;
import net.sf.json.JSONObject;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates an excel sheet that decomposes initiatives into epics and further into user stories.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
class MasterExcelFileWriter extends AExcelFileWriter
{
    private XSSFSheet bigPictureSheet;

    private StoryIssueComparator storyIssueComparator = new StoryIssueComparator();

    private static final int KEY_COLUMN = 0;
    private static final int INITIATIVE_COLUMN = 1;
    private static final int EPIC_COLUMN = 2;
    private static final int PROGRAM_COLUMN = 3;
    private static final int SPACE_COLUMN = 4;
    private static final int SPRINT_COLUMN = 5;
    private static final int STORY_COLUMN = 6;
    private static final int STATUS_COLUMN = 7;
    private static final int STORY_POINT_COLUMN = 8;
    private static final int ISSUE_TYPE_COLUMN = 9;
    private static final int ASSIGNEE_COLUMN = 10;
    private static final int REPORTER_COLUMN = 11;
    private static final int PRIORITY_COLUMN = 12;
    private static final int LABELS_COLUMN = 13;
    private static final int COMPONENTS_COLUMN = 14;
    private static final int FIX_VERSION_COLUMN = 15;
    private static final int DUE_DATE_COLUMN = 16;
    private static final int DESCRIPTION_COLUMN = 17;

    private CellStyle hiddenStyle;

    /**
     * Creates an excel sheet and initializes fonts and styles.
     *
     * @param workbook          A excel sheet wrapper.
     * @param initiativeEpicMap A decomposition of initiatives into epics.
     * @param epicStoryMap      A decomposition of epics into initiatives.
     */
    MasterExcelFileWriter(XSSFWorkbook workbook, Map<Issue, List<Issue>> initiativeEpicMap, Map<Issue,
            List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap)
    {
        super(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

        bigPictureSheet = workbook.createSheet("All Issues");
        hiddenStyle = workbook.createCellStyle();

        XSSFFont font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());

        hiddenStyle.setFont(font);
    }

    /**
     * Adds a summary of initiatives, epics, and user stories to an excel sheet.
     */
    void createJIRAReport()
    {
        bigPictureSheet.setRowSumsBelow(false);

        initializeSummaryHeaders();

        int row = 1;

        for (Map.Entry<Issue, List<Issue>> initiativeEntry : getInitativeEntriesMap())
        {
            row = createHeaderCell(row, INITIATIVE_COLUMN, initiativeStyle, initiativeEntry.getKey());
            int initiativeRowStart = row;

            for (Issue epicIssue : initiativeEntry.getValue())
            {
                if (activeEpics.isEmpty() || activeEpics.contains(epicIssue))
                {
                    row = createHeaderCell(row, EPIC_COLUMN, epicStyle, epicIssue);
                    int epicRowStart = row;

                    List<Issue> sortedStories = getSortedStories(epicIssue);

                    if (sortedStories.size() > 0)
                    {
                        int projectRowStart = row + 1;
                        boolean first = true;
                        String project = sortedStories.get(0).getProject().getName();

                        for (Issue storyIssue : sortedStories)
                        {
                            if (containsLabel(storyIssue, activeLabels) && containsSprint(storyIssue, activeSprints))
                            {
                                if (!storyIssue.getProject().getName().equals(project))
                                {
                                    if (!first)
                                    {
                                        projectRowStart += 1;
                                    }
                                    bigPictureSheet.groupRow(projectRowStart, row - 1);
                                    projectRowStart = row;
                                    project = storyIssue.getProject().getName();

                                    first = false;
                                }

                                row = createFieldCells(row, storyIssue);
                            }
                        }
                        bigPictureSheet.groupRow(projectRowStart + 1, row - 1);
                    }

                    bigPictureSheet.groupRow(epicRowStart, row - 1);
                }
            }

            bigPictureSheet.groupRow(initiativeRowStart, row - 1);
        }

        writePresenceTests();
        bigPictureSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, DESCRIPTION_COLUMN + presenceChecks.size()));
        setColumnWidths();
    }

    /**
     * Writes out a column per label whose presence should be tested for a given row.
     */
    private void writePresenceTests()
    {
        for (int rowIndex = bigPictureSheet.getFirstRowNum(); rowIndex < bigPictureSheet.getLastRowNum(); rowIndex++)
        {
            for (int columnIndex = 0; columnIndex < presenceChecks.size(); columnIndex++)
            {
                int columnOffset = DESCRIPTION_COLUMN + 1;
                Row presenceRow = bigPictureSheet.getRow(rowIndex);
                Cell presenceCell = presenceRow.createCell(columnIndex + columnOffset);
                Cell labelsCell = presenceRow.getCell(LABELS_COLUMN);

                String label = presenceChecks.get(columnIndex);

                if (rowIndex == bigPictureSheet.getFirstRowNum())
                {
                    presenceCell.setCellValue(label);
                    presenceCell.setCellStyle(titleStyle);
                } else
                {
                    boolean present = Arrays.asList(labelsCell.getStringCellValue().split(",")).contains(label);
                    presenceCell.setCellValue(present);
                }
            }
        }
    }

    /**
     * Specify which columns should be autosized and which should have concrete sizes.
     */
    private void setColumnWidths()
    {
        bigPictureSheet.setDefaultColumnWidth(20);
        bigPictureSheet.autoSizeColumn(KEY_COLUMN);
        bigPictureSheet.setColumnWidth(INITIATIVE_COLUMN, 3000);
        bigPictureSheet.setColumnWidth(EPIC_COLUMN, 2000);
        bigPictureSheet.autoSizeColumn(PROGRAM_COLUMN);
        bigPictureSheet.autoSizeColumn(SPACE_COLUMN);
        bigPictureSheet.autoSizeColumn(SPRINT_COLUMN);
        bigPictureSheet.autoSizeColumn(STORY_COLUMN);
        bigPictureSheet.autoSizeColumn(STATUS_COLUMN);
        bigPictureSheet.autoSizeColumn(STORY_POINT_COLUMN);
        bigPictureSheet.setColumnWidth(DESCRIPTION_COLUMN, 30000);
    }

    /**
     * Creates a row of header cells in the excel sheet to describe the content that can be found in each column.
     */
    private void initializeSummaryHeaders()
    {
        Row titleRow = bigPictureSheet.createRow(0);
        Cell keyCell = titleRow.createCell(KEY_COLUMN);
        Cell initiativeCell = titleRow.createCell(INITIATIVE_COLUMN);
        Cell programCell = titleRow.createCell(PROGRAM_COLUMN);
        Cell projectCell = titleRow.createCell(SPACE_COLUMN);
        Cell sprintCell = titleRow.createCell(SPRINT_COLUMN);
        Cell epicCell = titleRow.createCell(EPIC_COLUMN);
        Cell storyCell = titleRow.createCell(STORY_COLUMN);
        Cell statusCell = titleRow.createCell(STATUS_COLUMN);
        Cell storyPointCell = titleRow.createCell(STORY_POINT_COLUMN);
        Cell assigneeCell = titleRow.createCell(ASSIGNEE_COLUMN);
        Cell issueTypeCell = titleRow.createCell(ISSUE_TYPE_COLUMN);
        Cell reporterCell = titleRow.createCell(REPORTER_COLUMN);
        Cell priorityCell = titleRow.createCell(PRIORITY_COLUMN);
        Cell fixVersionCell = titleRow.createCell(FIX_VERSION_COLUMN);
        Cell labelsCell = titleRow.createCell(LABELS_COLUMN);
        Cell dueDateCell = titleRow.createCell(DUE_DATE_COLUMN);
        Cell descriptionCell = titleRow.createCell(DESCRIPTION_COLUMN);
        Cell componentsCell = titleRow.createCell(COMPONENTS_COLUMN);

        keyCell.setCellValue("Key");
        initiativeCell.setCellValue("Initiative");
        epicCell.setCellValue("Epic");
        programCell.setCellValue("Program / Project");
        projectCell.setCellValue("Space");
        sprintCell.setCellValue("Sprint");
        storyCell.setCellValue("User Story");
        statusCell.setCellValue("Status");
        issueTypeCell.setCellValue("Issue Type");
        storyPointCell.setCellValue("Story Points");
        reporterCell.setCellValue("Reporter");
        priorityCell.setCellValue("Priority");
        fixVersionCell.setCellValue("Fix Version");
        labelsCell.setCellValue("Labels");
        dueDateCell.setCellValue("Due Date");
        descriptionCell.setCellValue("Description");
        componentsCell.setCellValue("Components");
        assigneeCell.setCellValue("Assignee");

        for (int i = 0; i <= DESCRIPTION_COLUMN; i++)
        {
            titleRow.getCell(i).setCellStyle(titleStyle);
        }
    }

    /**
     * Links a cell to it's corresponding issue in JIRA.
     *
     * @param issue The issue to extract URL information from.
     * @param cell  The excel cell to link to the JIRA issue.
     */
    private void addURLLink(Issue issue, Cell cell)
    {
        XSSFHyperlink link = (XSSFHyperlink) creationHelper.createHyperlink(HyperlinkType.URL);

        if (issue.getKey().contains("https"))
        {
            link.setAddress("https://jira.di2e.net/browse/" + issue.getKey());
            cell.setHyperlink(link);
        }
    }

    /**
     * Creates a special header row cell. These rows represent groups of cells and their content is spread over multiple cells.
     *
     * @param row       The index of the row to create the new cell in the range [1, Integer.MAX_VALUE).
     * @param column    The index of the column to create the new cell in the range [1, Integer.MAX_VALUE)
     * @param cellStyle The color and font configuration used to decorate the cell and all subsequent cells in the same row.
     * @param issue     The issue to be added to a cell.
     * @return The next row to add new data to.
     */
    private int createHeaderCell(int row, int column, CellStyle cellStyle, Issue issue)
    {
        Row excelRow = bigPictureSheet.createRow(row++);
        Cell excelCell = excelRow.createCell(column);

        excelCell.setCellStyle(cellStyle);
        excelCell.setCellValue(issue.getSummary());

        configureCommonCells(excelRow, issue, false);

        for (int columnToStyle = column + 1; columnToStyle <= DESCRIPTION_COLUMN; columnToStyle++)
        {
            Cell cellToStyle = excelRow.getCell(columnToStyle);

            if (cellToStyle == null)
            {
                cellToStyle = excelRow.createCell(columnToStyle);
            }

            cellToStyle.setCellStyle(cellStyle);
        }

        return row;
    }

    /**
     * Sets the style and values of cells that all rows will contain.
     *
     * @param excelRow    The index of the row to create the new cell in the range [1, Integer.MAX_VALUE).
     * @param issue       The issue to create a URL link from.
     * @param setEpicCell True if the epic cell should be overriden with the value of the row above it.
     */
    private void configureCommonCells(Row excelRow, Issue issue, boolean setEpicCell)
    {
        Cell keyCell = excelRow.createCell(KEY_COLUMN);
        Cell statusCell = excelRow.createCell(STATUS_COLUMN);

        Row previousRow = bigPictureSheet.getRow(excelRow.getRowNum() - 1);

        if (previousRow != null)
        {
            if (excelRow.getCell(INITIATIVE_COLUMN) == null && previousRow.getCell(INITIATIVE_COLUMN) != null)
            {
                Cell initiativeCell = excelRow.createCell(INITIATIVE_COLUMN);
                initiativeCell.setCellValue(previousRow.getCell(INITIATIVE_COLUMN).getStringCellValue());
                initiativeCell.setCellStyle(hiddenStyle);
            }
            if (setEpicCell && excelRow.getCell(EPIC_COLUMN) == null && previousRow.getCell(EPIC_COLUMN) != null)
            {
                Cell epicCell = excelRow.createCell(EPIC_COLUMN);
                epicCell.setCellValue(previousRow.getCell(EPIC_COLUMN).getStringCellValue());
                epicCell.setCellStyle(hiddenStyle);
            }
        }

        keyCell.setCellValue(issue.getKey());
        keyCell.setCellStyle(urlStyle);
        addURLLink(issue, keyCell);

        String status = getStatus(issue);

        if (status.contains("%"))
        {
            double percent = Double.parseDouble(status.replace("%", ""));
            statusCell.setCellValue(percent);
        } else
        {
            statusCell.setCellValue(status);
        }
    }

    /**
     * Create and populate the cells that display the values of fields within a story.
     *
     * @param row        The index of the row to create the new cell in the range [1, Integer.MAX_VALUE).
     * @param storyIssue The issue to retrieve information from.
     * @return The next row to add new data to.
     */
    private int createFieldCells(int row, Issue storyIssue)
    {
        Row excelRow = bigPictureSheet.createRow(row++);

        Cell programCell = excelRow.createCell(PROGRAM_COLUMN);
        Cell projectCell = excelRow.createCell(SPACE_COLUMN);
        Cell sprintCell = excelRow.createCell(SPRINT_COLUMN);
        Cell summaryCell = excelRow.createCell(STORY_COLUMN);
        Cell issueTypeCell = excelRow.createCell(ISSUE_TYPE_COLUMN);
        Cell storyPointCell = excelRow.createCell(STORY_POINT_COLUMN);
        Cell assigneeCell = excelRow.createCell(ASSIGNEE_COLUMN);
        Cell reporterCell = excelRow.createCell(REPORTER_COLUMN);
        Cell priorityCell = excelRow.createCell(PRIORITY_COLUMN);
        Cell fixVersionCell = excelRow.createCell(FIX_VERSION_COLUMN);
        Cell labelsCell = excelRow.createCell(LABELS_COLUMN);
        Cell dueDateCell = excelRow.createCell(DUE_DATE_COLUMN);
        Cell descriptionCell = excelRow.createCell(DESCRIPTION_COLUMN);
        Cell componentsCell = excelRow.createCell(COMPONENTS_COLUMN);

        String programCustomId = fieldCustomIdMap.get(JiraRestClient.PROGRAM_KEY);
        String storyPointCustomId = fieldCustomIdMap.get(JiraRestClient.STORY_POINTS_KEY);
        String sprintCustomId = fieldCustomIdMap.get(JiraRestClient.SPRINT_KEY);

        Object jsonObject = storyIssue.getField(programCustomId);
        Object storyPoints = storyIssue.getField(storyPointCustomId);
        List<Properties> properties = JiraRestClient.getSprintProperties(storyIssue, sprintCustomId);

        if (jsonObject instanceof JSONObject)
        {
            String programProject = ((JSONObject) jsonObject).getString("value");

            programCell.setCellValue(programProject);
        }
        if (storyIssue.getProject() != null)
        {
            projectCell.setCellValue(storyIssue.getProject().getName());
        }
        if (storyIssue.getIssueType() != null)
        {
            issueTypeCell.setCellValue(storyIssue.getIssueType().getName());
        }
        if (storyIssue.getAssignee() != null)
        {
            assigneeCell.setCellValue(storyIssue.getAssignee().getDisplayName());
        }
        if (storyIssue.getReporter() != null)
        {
            reporterCell.setCellValue(String.join(", ", storyIssue.getReporter().getName()));
        }
        if (storyIssue.getPriority() != null)
        {
            priorityCell.setCellValue(String.join(", ", storyIssue.getPriority().getName()));
        }
        if (storyIssue.getFixVersions() != null)
        {
            fixVersionCell.setCellValue(storyIssue.getFixVersions().stream().map(Version::getName).collect(Collectors.joining(", ")));
        }
        if (storyIssue.getLabels() != null)
        {
            labelsCell.setCellValue(String.join(", ", storyIssue.getLabels()));
        }
        if (storyIssue.getDueDate() != null)
        {
            dueDateCell.setCellValue(storyIssue.getDueDate());
        }
        if (storyIssue.getDescription() != null)
        {
            descriptionCell.setCellValue(storyIssue.getDescription());
        }
        if (storyIssue.getComponents() != null)
        {
            componentsCell.setCellValue(storyIssue.getComponents().stream().map(Component::getName).collect(Collectors.joining(", ")));
        }
        if (properties.size() > 0)
        {
            //Get the name of the last sprint this ticket was in
            sprintCell.setCellValue(properties.get(properties.size() - 1).getProperty("name"));
        }
        if (storyPoints instanceof Double)
        {
            storyPointCell.setCellValue((double) storyPoints);
        }

        configureCommonCells(excelRow, storyIssue, true);
        summaryCell.setCellValue(storyIssue.getSummary());

        for (int column = PROGRAM_COLUMN; column <= DESCRIPTION_COLUMN; column++)
        {
            Cell cell = excelRow.getCell(column);

            if (cell != null)
            {
                excelRow.getCell(column).setCellStyle(wrapStyle);
            }
        }

        return row;
    }

    /**
     * Get's the progress stage that a ticket is in.
     *
     * @param issue The issue to retrieve information from.
     * @return The progress stage that a ticket is in.
     */
    private String getStatus(Issue issue)
    {
        Set<Issue> allNestedStories = getAllNestedIssues(issue);

        if (completedIssues.contains(issue) || allNestedStories.isEmpty())
        {
            return issue.getStatus().getName();
        }

        double completedNestedStories = allNestedStories.stream().filter(completedIssues::contains).count();

        return decimalFormat.format(completedNestedStories / allNestedStories.size()) + "%";
    }

    /**
     * Gets a sorted list of child issues from {@code epic}.
     *
     * @param epic The epic to get the sorted chidren of.
     * @return A sorted list of child issues from {@code epic}.
     */
    private List<Issue> getSortedStories(Issue epic)
    {
        List<Issue> issues = epicStoryMap.get(epic);

        issues = issues.stream().filter(Objects::nonNull).collect(Collectors.toList());
        issues.sort(storyIssueComparator);

        return issues;
    }
}
