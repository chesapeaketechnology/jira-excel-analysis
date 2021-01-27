package com.chesapeake.technology.excel;

import com.chesapeake.technology.model.IJiraIssueListener;
import com.typesafe.config.Config;
import net.rcarz.jiraclient.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Used to generate pre configured reports.
 *
 * @since 1.0.0
 */
public class HeadlessReportGenerator implements IJiraIssueListener
{
    private Config headlessPreferences;

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public HeadlessReportGenerator(Config headlessPreferences)
    {
        this.headlessPreferences = headlessPreferences;
    }

    @Override
    public void initiativesLoaded(Collection<Issue> initiatives)
    {

    }

    @Override
    public void childrenRetrieved(Issue parent, List<Issue> childIssues)
    {

    }

    @Override
    public void allIssuesRetrieved(Map<Issue, List<Issue>> initiativeEpicMap, Map<Issue, List<Issue>> epicStoryMap,
                                   Map<String, String> fieldCustomIdMap)
    {
        logger.info("Retrieved all issues");

        File directory = new File("reports/");

        deleteOldReports(directory);

        headlessPreferences.getObject("jira-excel-analysis").toConfig().getConfigList("reports").forEach(config -> {
            String fileName = config.getString("fileName");
            Collection<String> labels = config.getStringList("labelFilters");

            try
            {
                logger.info("Generating report for: {}", fileName);

                ExcelFileWriter excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

                // Disabled because this feature is no longer operational
                // TODO: Evaluate the level of effort required to restore this project
//                excelFileWriter.setIncludeSummaryMetrics(false);

                List<Issue> activeEpics = epicStoryMap.keySet().stream().filter(epic -> epic.getLabels().containsAll(labels)).collect(Collectors.toList());
                List<Issue> finalActiveEpics = activeEpics;
                List<Issue> activeInitiatives = initiativeEpicMap.entrySet().stream()
                        .filter(initiativeEntry -> initiativeEntry.getValue().stream().anyMatch(finalActiveEpics::contains))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                if (labels.contains("Unassigned"))
                {
                    activeInitiatives = new ArrayList<>();
                    activeEpics = Collections.emptyList();
                }
                excelFileWriter.setActiveData(activeInitiatives, activeEpics, Collections.emptyList(), labels, Collections.emptyList());
                excelFileWriter.setFileName(fileName);
                excelFileWriter.createJIRAReport();
            } catch (Exception exception)
            {
                exception.printStackTrace();
            }
        });
    }

    /**
     * Recursively removes all previously generated reports.
     *
     * @param directory The root directory to remove all files below.
     */
    private void deleteOldReports(File directory)
    {
        if (directory.exists())
        {
            File[] filesToRemove = directory.listFiles();

            if (filesToRemove != null)
            {
                for (File file : filesToRemove)
                {
                    if (file.isDirectory())
                    {
                        deleteOldReports(file);
                    } else
                    {
                        file.delete();
                    }
                }
            }
        }
    }
}