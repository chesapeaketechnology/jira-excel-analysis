package com.chesapeake.technology.excel;

import com.typesafe.config.Config;
import net.rcarz.jiraclient.Issue;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Writes JIRA analytic information to an excel file.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
public class ExcelFileWriter
{
    private IssueSummaryExcelFileWriter issueSummaryExcelFileWriter;
    private TeamSummaryExcelFileWriter teamExcelFileWriter;
    private GoalMetricsExcelFileWriter goalSummaryExcelFileWriter;
    private XSSFWorkbook workbook;

    private String fileName = "JIRA_Report";

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Initializes an excel workbook and creates the sheets that will show analytics within it.
     *
     * @param initiativeEpicMap A mapping of initiatives to epics.
     * @param epicStoryMap      A mapping of epics to user stories.
     */
    public ExcelFileWriter(Map<Issue, List<Issue>> initiativeEpicMap, Map<Issue,
            List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap)
    {
        try
        {
            //TODO: Optionally create the workbook from the DefaultSheet.xlsx to include additional details about the environment
//            File file = new File(ExcelFileWriter.class.getResource("/DefaultSheet.xlsx").toURI());
//            workbook = new XSSFWorkbook(file);
//            workbook.setSheetOrder("About", 3);

            workbook = new XSSFWorkbook();
            issueSummaryExcelFileWriter = new IssueSummaryExcelFileWriter(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);
            teamExcelFileWriter = new TeamSummaryExcelFileWriter(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);
            goalSummaryExcelFileWriter = new GoalMetricsExcelFileWriter(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);
        } catch (Exception exception)
        {
            logger.warn("Failed to construct workbook: ", exception);
        }
    }

    /**
     * Sets the initiatives, epics, sprints, labels, and presence checks to include in the report. These configurations
     * enable the tool to pull data one time while allowing the user to generate more than one report with variable
     * settings.
     *
     * @param activeInitiatives The subset of initiatives that should be included in generated reports or an empty list
     *                          if all initiatives should be included.
     * @param activeEpics       The subset of epics that should be included in generated reports or an empty list
     *                          if all epics should be included.
     * @param activeSprints     The subset of sprints that should be included in generated reports or an empty list
     *                          if all sprints should be included.
     * @param activeLabels      The labels that issues must contain to be included in the report. If any label is found
     *                          within the issue then the issue will be included.
     */
    public void setActiveData(Collection<Issue> activeInitiatives, Collection<Issue> activeEpics,
                              Collection<String> activeSprints, Collection<String> activeLabels,
                              List<String> presenceChecks)
    {
        issueSummaryExcelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);
        teamExcelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);
        goalSummaryExcelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);
    }

    /**
     * Writes the content to the excel report file. To configure which tabs are included in the report use
     * To configure what content is included within each of the tabs use
     * {@link #setActiveData(Collection, Collection, Collection, Collection, List)}.
     */
    public void createJIRAReport(Config config)
    {
        logger.info("Attempting to create Excel file");

        if (!config.getBoolean("jira.sheets.Goal Metrics.hidden"))
        {
            logger.info("Including goal summary information in excel file");

            goalSummaryExcelFileWriter.createJiraReport(config);
        } else
        {
            logger.info("Clearing goal summary information in excel file");

            workbook.removeSheetAt(workbook.getSheetIndex(goalSummaryExcelFileWriter.getSheet()));
        }

        if (!config.getBoolean("jira.sheets.Team Metrics.hidden"))
        {
            logger.info("Including team metrics in excel file");

            teamExcelFileWriter.createJiraReport(config);
        } else
        {
            logger.info("Clearing the individual metrics sheet from the excel file");

            workbook.removeSheetAt(workbook.getSheetIndex(teamExcelFileWriter.getSheet()));
        }

        if (!config.getBoolean("jira.sheets.Issue Summary.hidden"))
        {
            logger.info("Including master report information in excel file");

            issueSummaryExcelFileWriter.createJiraReport(config);
        } else
        {
            logger.info("Clearing goal summary information in excel file");

            workbook.removeSheetAt(workbook.getSheetIndex(issueSummaryExcelFileWriter.getSheet()));
        }

        logger.info("Preparing to write excel file");
        issueSummaryExcelFileWriter.writeExcelFile(fileName);

        logger.info("Successfully finished writing Excel file");
    }

    /**
     * Sets the name of the report.
     *
     * @param fileName The name of the file.
     */
    void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
}
