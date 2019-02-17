import com.chesapeake.technology.JiraRestClient;
import com.chesapeake.technology.data.MockIssue;
import com.chesapeake.technology.excel.ExcelFileWriter;
import net.rcarz.jiraclient.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public abstract class AExcelFileWriterTest
{
    private Random random = new Random();

    Map<Issue, List<Issue>> initiativeEpicMap = createMockInitiativeEpicMap();
    Map<Issue, List<Issue>> epicStoryMap = createMockEpicStoryMap(initiativeEpicMap);
    Map<String, String> customFieldMapping = createCustomFieldMapping();
    ExcelFileWriter excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, customFieldMapping);

    String reportDirectoryPath = "reports/JIRA_Report";

    @BeforeClass
    public void deleteOldReports()
    {
        File reportDirectory = new File(reportDirectoryPath);

        File[] filesToRemove = reportDirectory.listFiles();

        if (filesToRemove != null)
        {
            for (File file : filesToRemove)
            {
                file.delete();
            }
        }
    }

    @BeforeTest
    public void initializeActiveData()
    {
        excelFileWriter.setActiveData(initiativeEpicMap.keySet(), epicStoryMap.keySet(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
    }

    private Map<Issue, List<Issue>> createMockInitiativeEpicMap()
    {
        Map<Issue, List<Issue>> initiativeEpicMap = new HashMap<>();
        int numInitiatives = random.nextInt(5) + 5;

        for (int initiativeIndex = 0; initiativeIndex < numInitiatives; initiativeIndex++)
        {
            int numEpics = random.nextInt(10) + 5;

            Issue initiative = new MockIssue();

            List<Issue> epics = new ArrayList<>(numEpics);

            for (int epicIndex = 0; epicIndex < numEpics; epicIndex++)
            {
                epics.add(new MockIssue());
            }

            initiativeEpicMap.put(initiative, epics);
        }

        return initiativeEpicMap;
    }

    private Map<Issue, List<Issue>> createMockEpicStoryMap(Map<Issue, List<Issue>> initiativeEpicMap)
    {
        Map<Issue, List<Issue>> epicStoryMap = new HashMap<>();

        initiativeEpicMap.values().stream()
                .flatMap(Collection::stream)
                .forEach(epic -> {
                    int numIssues = random.nextInt(20) + 5;
                    List<Issue> issues = new ArrayList<>();

                    for (int issueIndex = 0; issueIndex < numIssues; issueIndex++)
                    {
                        issues.add(new MockIssue());
                    }

                    epicStoryMap.put(epic, issues);
                });

        return epicStoryMap;
    }

    private Map<String, String> createCustomFieldMapping()
    {
        Map<String, String> customFieldReadbaleNameMap = new HashMap<>();

        customFieldReadbaleNameMap.put(JiraRestClient.STORY_POINTS_KEY, JiraRestClient.STORY_POINTS_KEY);
        customFieldReadbaleNameMap.put(JiraRestClient.PROGRAM_KEY, JiraRestClient.PROGRAM_KEY);
        customFieldReadbaleNameMap.put(JiraRestClient.SPRINT_KEY, JiraRestClient.SPRINT_KEY);
        customFieldReadbaleNameMap.put(JiraRestClient.EPIC_LINK, JiraRestClient.EPIC_LINK);

        return customFieldReadbaleNameMap;
    }
}
