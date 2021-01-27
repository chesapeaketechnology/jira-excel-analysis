package com.chesapeake.technology.excel;

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
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private MasterExcelFileWriter masterExcelFileWriter;
    private DeveloperExcelFileWriter developerExcelFileWriter;
    private SummaryExcelFileWriter summaryExcelFileWriter;
    private XSSFWorkbook workbook;
    private boolean includeMasterReport = true;
    private boolean includeDeveloperMetrics = true;
    private boolean includeSummaryMetrics = true;

    private String fileName = "JIRA_Report";

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
            workbook = new XSSFWorkbook();
            developerExcelFileWriter = new DeveloperExcelFileWriter(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);
            masterExcelFileWriter = new MasterExcelFileWriter(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);
            summaryExcelFileWriter = new SummaryExcelFileWriter(workbook, initiativeEpicMap, epicStoryMap, fieldCustomIdMap);
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
        developerExcelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);
        masterExcelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);
        summaryExcelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);
    }

    /**
     * Writes the content to the excel report file. To configure which tabs are included in the report use
     * {@link #setIncludeMasterReport(boolean)}, {@link #setIncludeDeveloperMetrics(boolean)},
     * {@link #setIncludeSummaryMetrics(boolean)}. To configure what content is included within each of the
     * tabs use {@link #setActiveData(Collection, Collection, Collection, Collection, List)}.
     */
    public void createJIRAReport()
    {
        logger.info("Attempting to create Excel file");

        try
        {
            if (includeSummaryMetrics)
            {
                logger.info("Including goal summary information in excel file");

                summaryExcelFileWriter.generateReport();
            } else
            {
                logger.info("Clearing goal summary information in excel file");

                workbook.removeSheetAt(0);
            }

            if (includeDeveloperMetrics)
            {
                logger.info("Including individual metrics in excel file");

                developerExcelFileWriter.createJIRAReport();
            } else
            {
                logger.info("Clearing the individual metrics sheet from the excel file");

                workbook.removeSheetAt(1);
            }

            if (includeMasterReport)
            {
                logger.info("Including master report information in excel file");

                masterExcelFileWriter.createJIRAReport();
            } else
            {
                logger.info("Clearing goal summary information in excel file");

                workbook.removeSheetAt(0);
            }

            logger.info("Preparing to write excel file");
            masterExcelFileWriter.writeExcelFile(fileName);

            logger.info("Successfully finished writing Excel file");
        } catch (Exception exception)
        {
            logger.warn("Failed to create JIRA Report: ", exception);
        }
    }

    /**
     * Configures if the "master" tab will be included in the report. The "master" tab contains a breakdown
     * of all initiatives, epics, issues listed in a tree structure with some additional metadata on the issues.
     *
     * @param include {@code true} if the "master" tab should be included.
     */
    public void setIncludeMasterReport(boolean include)
    {
        includeMasterReport = include;
    }

    /**
     * Configures if the "developer metrics" tab will be included in the report. The "developer metrics" tab contains
     * an analysis of developers; ticket estimation abilities.
     *
     * @param include {@code true} if the "developer metrics" tab should be included.
     */
    public void setIncludeDeveloperMetrics(boolean include)
    {
        includeDeveloperMetrics = include;
    }

    /**
     * Configures if the "summary" tab will be included in the report. The summary report contains a subset of information
     * from the "master" tab including initiative and epic's percentage complete in both tabular and graphical representations.
     *
     * @param include {@code true} if the "master" tab should be included.
     */
    public void setIncludeSummaryMetrics(boolean include)
    {
        includeSummaryMetrics = include;
    }

    /**
     * Sets the name of the file that the excel report will be written to.
     *
     * @param fileName The name of the file without the file's extension.
     */
    void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
}
