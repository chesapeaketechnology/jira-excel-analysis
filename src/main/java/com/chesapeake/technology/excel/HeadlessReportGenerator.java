package com.chesapeake.technology.excel;

import com.chesapeake.technology.model.IJiraIssueListener;
import com.chesapeake.technology.ui.fx.ConfigurationController;
import com.typesafe.config.Config;
import net.rcarz.jiraclient.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Used to generate pre configured reports. To enable the user to create a custom report
 * see {@link ConfigurationController}.
 *
 * @since 1.0.0
 */
public class HeadlessReportGenerator implements IJiraIssueListener
{
    private Config config;

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Creates a report generator that can be run from a headless environment.
     *
     * @param config User preferences.
     */
    HeadlessReportGenerator(Config config)
    {
        this.config = config;
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

        if (config.getBoolean("jira.removeOldReports"))
        {
            deleteOldReports(directory);
        }

        config.getConfigList("jira.reports").forEach(nestedConfiguration -> {
            String fileName = nestedConfiguration.getString("fileName");
            Collection<String> labels = nestedConfiguration.getStringList("filters.labels");
            List<String> presenceChecks = nestedConfiguration.getStringList("presence.labels");
            List<String> sprints = nestedConfiguration.getStringList("filters.sprints");

            try
            {
                logger.info("Generating report for: {}", fileName);

                ExcelFileWriter excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

                Collection<Issue> activeEpics = getActiveEpics(epicStoryMap);
                Collection<Issue> activeInitiatives = getActiveInitiatives(initiativeEpicMap, activeEpics);

                excelFileWriter.setActiveData(activeInitiatives, activeEpics, sprints, labels, presenceChecks);
                excelFileWriter.setFileName(fileName);
                excelFileWriter.createJIRAReport(config);
            } catch (Exception exception)
            {
                logger.warn("Failed to write excel file: ", exception);
            }
        });
    }

    /**
     * Gets the initiatives that match the user's filters.
     *
     * @param initiativeEpicMap Mapping from Initiative JIRA tickets to Epic JIRA tickets.
     * @param activeEpics       Epics that have been filtered.
     * @return The initiatives that match the user's filters.
     */
    private Collection<Issue> getActiveInitiatives(Map<Issue, List<Issue>> initiativeEpicMap, Collection<Issue> activeEpics)
    {
        Collection<Issue> activeInitiatives = initiativeEpicMap.keySet();

        boolean includeAllInitiatives = config.getBoolean("jira.filters.hideEmptyGroups");

        if (!includeAllInitiatives)
        {
            activeInitiatives = initiativeEpicMap.entrySet().stream()
                    .filter(initiativeEntry -> initiativeEntry.getValue().stream().anyMatch(activeEpics::contains))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        return activeInitiatives;
    }

    /**
     * Gets the epics that match the user's filters.
     *
     * @param epicStoryMap Mapping from Epic JIRA tickets to Story JIRA tickets.
     * @return The epics that match the user's filters.
     */
    private Collection<Issue> getActiveEpics(Map<Issue, List<Issue>> epicStoryMap)
    {
        boolean includeAllInitiatives = config.getBoolean("jira.filters.hideEmptyGroups");

        return epicStoryMap.keySet().stream()
                .filter(epic -> includeAllInitiatives || epicStoryMap.get(epic).size() > 0)
                .collect(Collectors.toList());
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