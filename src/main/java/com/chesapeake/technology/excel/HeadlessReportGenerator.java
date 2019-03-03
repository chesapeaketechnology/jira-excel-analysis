package com.chesapeake.technology.excel;

import com.chesapeake.technology.JiraRestClient;
import com.chesapeake.technology.model.IJiraIssueListener;
import com.chesapeake.technology.ui.fx.ConfigurationController;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * @param configFilePath User preferences.
     * @param username       Authentication key to access JIRA server
     * @param password       Authentication password to access JIRA server
     */
    HeadlessReportGenerator(String configFilePath, String username, String password) throws JiraException
    {
        Config config = ConfigFactory.load(configFilePath);
        String baseUrl = config.getString("jira.baseUrl");
        boolean includeChangeLogs = config.getBoolean("jira.importFullHistory");

        JiraRestClient requestClient = new JiraRestClient(baseUrl, new BasicCredentials(username, password), includeChangeLogs);

        requestClient.addIssueListener(this);
        requestClient.loadJiraIssues(config);
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
            List<String> presenceChecks = nestedConfiguration.hasPath("presence.labels") ? new ArrayList<>()
                    : nestedConfiguration.getStringList("presence.labels");
            List<String> sprints = nestedConfiguration.hasPath("filters.sprints") ? new ArrayList<>() :
                    nestedConfiguration.getStringList("filters.sprints");

            try
            {
                logger.info("Generating report for: {}", fileName);

                ExcelFileWriter excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

                excelFileWriter.setActiveData(initiativeEpicMap.keySet(), epicStoryMap.keySet(), sprints, labels, presenceChecks);
                excelFileWriter.setFileName(fileName);
                excelFileWriter.createJIRAReport(config);
            } catch (Exception exception)
            {
                logger.warn("Failed to write excel file: ", exception);
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