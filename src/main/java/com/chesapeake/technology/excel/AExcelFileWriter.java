package com.chesapeake.technology.excel;

import com.chesapeake.technology.JiraRestClient;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A data analytic tool that consumes information from JIRA and populates an Excel workbook.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
class AExcelFileWriter
{
    CreationHelper creationHelper;

    CellStyle titleStyle;
    CellStyle initiativeStyle;
    CellStyle epicStyle;
    CellStyle wrapStyle;
    CellStyle urlStyle;

    Map<Issue, List<Issue>> initiativeEpicMap;
    Map<Issue, List<Issue>> epicStoryMap;
    Set<Issue> completedIssues;

    DecimalFormat decimalFormat = new DecimalFormat("###.#");

    Collection<Issue> activeInitiatives = new ArrayList<>();
    Collection<Issue> activeEpics = new ArrayList<>();
    Collection<String> activeSprints = new ArrayList<>();
    Collection<String> activeLabels = new ArrayList<>();
    List<String> presenceChecks = new ArrayList<>();

    XSSFWorkbook workbook;

    Map<String, Set<Issue>> sprintStoryBreakdown;
    Map<String, Date> sprintDateMap = new HashMap<>();

    Map<String, String> fieldCustomIdMap;

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Initializes an excel workbook and pre-processes data that will be used when anaylzing JIRA data.
     *
     * @param workbook          The excel workbook where new sheets should be created from.
     * @param initiativeEpicMap A mapping of JIRA initiatives to JIRA epics.
     * @param epicStoryMap      A mapping of JIRA epics to JIRA stories.
     */
    AExcelFileWriter(XSSFWorkbook workbook, Map<Issue, List<Issue>> initiativeEpicMap,
                     Map<Issue, List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap)
    {
        this.initiativeEpicMap = initiativeEpicMap;
        this.epicStoryMap = epicStoryMap;
        this.workbook = workbook;
        this.fieldCustomIdMap = fieldCustomIdMap;

        initializeWorkbook();
        completedIssues = getCompletedIssues(epicStoryMap.values());

        sprintStoryBreakdown = getSprintBreakdown();
    }

    /**
     * Gets the initiative Issue objects that match the constraints of current filters.
     *
     * @return the initiative Issue objects that match the constraints of current filters.
     */
    Collection<Map.Entry<Issue, List<Issue>>> getInitativeEntriesMap()
    {
        Collection<Map.Entry<Issue, List<Issue>>> initativeEntries = initiativeEpicMap.entrySet();

        if (activeInitiatives.size() > 0)
        {
            initativeEntries = initativeEntries.stream().filter(entry -> activeInitiatives.contains(entry.getKey())).collect(Collectors.toList());
        }

        return initativeEntries;
    }

    Set<Issue> getAllNestedIssues(Issue issue)
    {
        Set<Issue> allNestedStories = new HashSet<>();

        if (initiativeEpicMap.containsKey(issue))
        {
            allNestedStories = initiativeEpicMap.get(issue).stream()
                    .flatMap(epic -> epicStoryMap.getOrDefault(epic, new ArrayList<>()).stream())
                    .collect(Collectors.toSet());
        } else if (epicStoryMap.containsKey(issue))
        {
            allNestedStories = new HashSet<>(epicStoryMap.getOrDefault(issue, new ArrayList<>()));
        }

        return allNestedStories;
    }

    /**
     * Gets the subset of issues within {@code issueCollections} that have been marked as completed.
     *
     * @param issueCollections Groups of story issues.
     * @return Unique issues that are marked as completed.
     */
    private Set<Issue> getCompletedIssues(Collection<List<Issue>> issueCollections)
    {
        return issueCollections.stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(issue -> issue.getStatus() != null)
                .filter(issue -> {
                    String name = issue.getStatus().getName();

                    return name.equalsIgnoreCase("Done") || name.equalsIgnoreCase("Resolved");
                })
                .collect(Collectors.toSet());
    }

    /**
     * Setup excel cell styles and fonts.
     */
    private void initializeWorkbook()
    {
        titleStyle = workbook.createCellStyle();
        creationHelper = workbook.getCreationHelper();

        Font headerFont = workbook.createFont();

        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        titleStyle.setFont(headerFont);
        titleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        initiativeStyle = workbook.createCellStyle();
        initiativeStyle.setFont(headerFont);
        initiativeStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
        initiativeStyle.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        initiativeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        epicStyle = workbook.createCellStyle();
        epicStyle.setFont(headerFont);
        epicStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
        epicStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        epicStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        urlStyle = workbook.createCellStyle();
        Font urlFont = workbook.createFont();
        urlFont.setUnderline(Font.U_SINGLE);
        urlFont.setColor(IndexedColors.BLUE.getIndex());
        urlStyle.setFont(urlFont);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
    }

    /**
     * Sets the subsets of elements that should be processed when generating excel reports. All active element filters
     * will be applied to find the interesection of the set of elements that match.
     *
     * @param activeInitiatives The JIRA initiatives that should be included in reports.
     * @param activeEpics       The JIRA epics that should be included in reports.
     * @param activeSprints     The JIRA sprints that should be included in reports.
     * @param activeLabels      The JIRA labels that should be included in reports.
     * @param presenceChecks    The JIRA labels that should be checked for presence.
     */
    void setActiveData(Collection<Issue> activeInitiatives, Collection<Issue> activeEpics,
                       Collection<String> activeSprints, Collection<String> activeLabels,
                       List<String> presenceChecks)
    {
        this.activeInitiatives = activeInitiatives;
        this.activeEpics = activeEpics;
        this.activeSprints = activeSprints;
        this.activeLabels = activeLabels;
        this.presenceChecks = presenceChecks;
    }

    /**
     * Evaluates if a JIRA issue contains any of a series of labels.
     *
     * @param issue  The issue to check for the presence of labels in.
     * @param labels The JIRA issue identification tags to check for the presence of within an issue.
     * @return True if {@code issue} contains any issues with {@code labels}.
     */
    boolean containsLabel(Issue issue, Collection<String> labels)
    {
        return labels.isEmpty() || issue.getLabels().stream().anyMatch(labels::contains);
    }

    /**
     * Evaluates if a JIRA issue is part of any of any sprint within {@code sprints}.
     *
     * @param issue   The issue to check for within a sprint.
     * @param sprints The JIRA sprints to check for the presence of the issue within.
     * @return True if the {@code issue} is contained within {@code sprints}.
     */
    boolean containsSprint(Issue issue, Collection<String> sprints)
    {
        String sprintCustomId = fieldCustomIdMap.get(JiraRestClient.SPRINT_KEY);

        return sprints.isEmpty()
                || !(JiraRestClient.getSprintProperties(issue, sprintCustomId).size() > 0)
                || JiraRestClient.getSprintProperties(issue, sprintCustomId).stream()
                .map(properties -> properties.getProperty("name"))
                .anyMatch(sprints::contains);
    }

    /**
     * Writes the content of the workbook to a file.
     *
     * @param fileName The name of the file to write.
     */
    void writeExcelFile(String fileName)
    {
        DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String date = timeStampPattern.format(java.time.LocalDateTime.now());

        File file = new File("reports/" + fileName + "/Jira-Report - " + date + ".xlsx");

        boolean directoriesMade = file.getParentFile().mkdirs();
        logger.info("Excel file directories successfully made: ", directoriesMade);

        try (FileOutputStream outputStream = new FileOutputStream(file))
        {
            workbook.write(outputStream);
            outputStream.flush();
        } catch (Exception exception)
        {
            logger.warn("Failed to write excel file: ", exception);
        }
    }

    /**
     * Gets a mapping of sprint names to their corresponding issues.
     *
     * @return A mapping of sprint names to their corresponding issues.
     */
    private Map<String, Set<Issue>> getSprintBreakdown()
    {
        sprintStoryBreakdown = new HashMap<>();

        Collection<Issue> developerStories = epicStoryMap.values().stream().flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(story -> story.getAssignee() != null)
                .collect(Collectors.toSet());

        String sprintCustomId = fieldCustomIdMap.get(JiraRestClient.SPRINT_KEY);

        developerStories.forEach(story -> {
            List<Properties> properties = JiraRestClient.getSprintProperties(story, sprintCustomId);

            properties.forEach(sprint -> {
                String name = sprint.getProperty("name");
                String startDate = sprint.getProperty("startDate");
                Set<Issue> sprintIssues = sprintStoryBreakdown.getOrDefault(name, new HashSet<>());

                sprintIssues.add(story);

                if (startDate != null && !startDate.contains("null"))
                {
                    sprintDateMap.put(name, Field.getDate(startDate));
                }
                sprintStoryBreakdown.put(name, sprintIssues);
            });
        });

        sprintDateMap = MapUtil.sortByValue(sprintDateMap);

        return sprintStoryBreakdown;
    }
}
