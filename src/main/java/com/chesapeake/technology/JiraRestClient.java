package com.chesapeake.technology;

import com.chesapeake.technology.model.IJiraIssueListener;
import com.typesafe.config.Config;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Provides access to query data from JIRA.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
public class JiraRestClient
{
    public static final String STORY_POINTS_KEY = "Story Points";
    public static final String PROGRAM_KEY = "Program / Project";
    public static final String SPRINT_KEY = "Sprint";
    public static final String EPIC_LINK = "Epic Link";

    private final boolean includeChangeLogs;

    private Set<IJiraIssueListener> jiraIssueListeners = new CopyOnWriteArraySet<>();

    private ConcurrentMap<Issue, List<Issue>> initiativeEpicMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Issue, List<Issue>> epicStoryMap = new ConcurrentHashMap<>();

    private CloseableHttpClient httpClient = getHttpClient();

    private ICredentials credentials;
    private String baseUrl;

    private EmptyIssue unassignedEpic = new EmptyIssue("Unassigned Epic");

    private static Map<String, String> fieldCustomIdMapping = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private CloseableHttpClient getHttpClient()
    {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public JiraRestClient(String baseUrl, String username, String password, boolean includeChangeLogs) throws JiraException
    {
        this(baseUrl, new BasicCredentials(username, password), includeChangeLogs);
    }

    /**
     * Creates a JIRA client that can be used to retrieve information from queries.
     *
     * @param baseUrl           The root URL where JIRA tickets are stored. For DI2E use https://jira.di2e.net/.
     * @param credentials       The unique username and password combination that identifies a user.
     * @param includeChangeLogs True if the changelogs should be included. If changelogs are included a more accurate
     *                          report can be generated but the reports will take an order
     * @throws JiraException Thrown if the {@code credentials} are invalid or the user could not be authenticated
     */
    public JiraRestClient(String baseUrl, ICredentials credentials, boolean includeChangeLogs) throws JiraException
    {
        this.baseUrl = baseUrl;
        this.credentials = credentials;
        this.includeChangeLogs = includeChangeLogs;

        //Validate credentials - This will exception after attempting to use any invalid credentials on the first try
        // to prevent locking the user out of their account
        new JiraClient(baseUrl, credentials);
    }

    /**
     * Gets the properties of all sprints that an issue was part of.
     *
     * @param issue The issue to identify that is used to identify sprint properties.
     * @return The properties of all sprints that an issue was part of.
     */
    public static List<Properties> getSprintProperties(Issue issue, String sprintCustomField)
    {
        List<Properties> properties = new ArrayList<>();

        if (!(issue.getField(sprintCustomField) instanceof JSONNull))
        {
            JSONArray jsonArray = (JSONArray) issue.getField(sprintCustomField);

            for (Object object : jsonArray)
            {
                properties.add(getSprint(object.toString()));
            }
        }

        return properties;
    }

    /**
     * Registers a listener that will be notified at milestones within the querying process.
     *
     * @param jiraIssueListener The listener that will be notified of progress in the JIRA querying process.
     */
    public void addIssueListener(IJiraIssueListener jiraIssueListener)
    {
        jiraIssueListeners.add(jiraIssueListener);
    }

    /**
     * Performs a series of JIRA queries to retrieve information about Initiatives, Epics, and User Stories.
     */
    public void loadJiraIssues(Config config)
    {
        logger.info("Loading Initiatives, epics, and stories");

        long startTime = System.nanoTime();
        boolean loadInitiatives = config.getBoolean("jira.filters.initiatives");
        Set<String> projects = new HashSet<>(config.getStringList("jira.filters.projects"));

        String epicQuery = getEpicQuery(config);
        loadCustomFields(projects.iterator().next());

        Collection<CompletableFuture> completableFutures = null;

        if (loadInitiatives)
        {
            completableFutures = loadIssueMapsFromInitiatives(projects, epicQuery);
        } else
        {
            //TODO: Load a completable future
            loadEpicsDirectly(epicQuery, projects);
        }

        completableFutures.forEach(CompletableFuture::join);
        epicStoryMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        initiativeEpicMap.values()
                .forEach(epics -> epics
                        .removeIf(epic -> !epicStoryMap.containsKey(epic)));
        initiativeEpicMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        long endTime = System.nanoTime();

        logger.info("Finished querying in: {} seconds", ((endTime - startTime) / 1_000_000_000.0));

        jiraIssueListeners.forEach(listener -> listener.allIssuesRetrieved(initiativeEpicMap, epicStoryMap, fieldCustomIdMapping));
    }

    private Set<String> getReportFilters(Config config, String key)
    {
        List<? extends Config> configs = config.getConfigList("jira.reports");
        Set<String> filters = new HashSet<>();

        boolean includeAllIssues = configs.stream()
                .map(reportConfig -> reportConfig.getStringList("filters." + key))
                .anyMatch(List::isEmpty);

        if (!includeAllIssues)
        {
            filters = configs.stream()
                    .flatMap(reportConfig -> reportConfig.getStringList("filters." + key).stream())
                    .collect(Collectors.toSet());
        }

        return filters;
    }

    /**
     * Get the mappings of custom identifier numbers from JIRA to the human readable forms.
     *
     * @return The mappings of custom identifier numbers from JIRA to the human readable forms.
     */
    public Map<String, String> getFieldCustomIdMapping()
    {
        return fieldCustomIdMapping;
    }

    /**
     * Build up a data model by pulling initiatives and then querying their children.
     *
     * @param projects JIRA defining groupings of initiatives, epics, and issues.
     */
    private List<CompletableFuture> loadIssueMapsFromInitiatives(Set<String> projects, String epicQuery)
    {
        String projectsFilter = String.join(",", projects);
        String initiativeJQL = "project in (" + projectsFilter + ") AND issuetype = Initiative";
        String childrenJQL = "issuekey in childIssuesOf(";

        try
        {
            Issue.SearchResult initiativeQueryResult = searchIssues(initiativeJQL, "names");

            jiraIssueListeners.forEach(listener -> listener.initiativesLoaded(initiativeQueryResult.issues));

            logger.info("Querying children of {} initiatives", initiativeQueryResult.issues.size());

            return initiativeQueryResult.issues.stream()
                    .map(initiative -> CompletableFuture.runAsync(() -> {
                        try
                        {
                            logger.info("Queued up children of: " + initiative.getKey());
                            Issue.SearchResult epicQueryResult;

                            String changeLog = "";

                            if (includeChangeLogs)
                            {
                                changeLog = "changelog";
                            }

                            //We need to requery for each initiative to find the associated child tickets. This query will include both Epics and User Stories
                            epicQueryResult = searchIssues(childrenJQL + initiative.getKey() + ")" + epicQuery, changeLog);

                            epicStoryMap.putAll(getEpicStoryMap(epicQueryResult, projects));
                            logger.info("Successfully queried children of: {}", initiative.getKey());
                            jiraIssueListeners.forEach(listener -> listener.childrenRetrieved(initiative, epicQueryResult.issues));

                            initiativeEpicMap.put(initiative, epicQueryResult.issues.stream()
                                    .filter(issue -> issue.getIssueType().getName().equalsIgnoreCase("Epic"))
                                    .filter(issue -> projects.contains(issue.getProject().getName()))
                                    .collect(Collectors.toList()));
                        } catch (Exception exception)
                        {
                            logger.warn("Failed to query children of: {}", initiative.getKey(), exception);
                        }
                    })).collect(Collectors.toList());
        } catch (Exception exception)
        {
            logger.warn("Failed to search issues: ", exception);
        }

        return new ArrayList<>();
    }

    /**
     * Build up a data model by pulling issues directly based on projects and assignees.
     *
     * @param epicQuery The JQL text that defines the constraints of what issues to process.
     */
    private void loadEpicsDirectly(String epicQuery, Collection<String> projects)
    {
        try
        {
            Issue.SearchResult epicQueryResult = searchIssues(epicQuery, "names");
            logger.info("Successfully queried children of: {}", epicQueryResult);
            epicStoryMap.putAll(getEpicStoryMap(epicQueryResult, projects));
            initiativeEpicMap.put(new EmptyIssue("Unassigned Epic"), new ArrayList<>(epicStoryMap.keySet()));
        } catch (Exception exception)
        {
            logger.warn("Failed to search issues: ", exception);
        }
    }

    private String getEpicQuery(Config config)
    {
        Collection<String> epics = config.getStringList("jira.filters.epics");

        String query = "";
        String epicsFilter = String.join(",", epics);
        String labelJQL = getFilterQuery(config, "labels", "labels");
        String sprintsJQL = getFilterQuery(config, "sprints", "sprint");

        if (epics.size() > 0)
        {
            query = "AND (\"epic link\" in (" + epicsFilter + ") OR id in (" + epicsFilter + "))";
        }

        /*
         * Projects should not be included in the query to avoid adding significant overhead to the process. The delay
         * is caused by additional layers of permissions and security checks that projects queries add. Instead projects
         * will be filtered post query before passing the data along to be used to generate a report.
         */
        return query + labelJQL + sprintsJQL;
    }

    private String getFilterQuery(Config config, String configKey, String jqlKey)
    {
        Set<String> values = getReportFilters(config, configKey);

        values = values.stream().map(value -> "\"" + value + "\"").collect(Collectors.toSet());
        return getFilterQuery(values, jqlKey);
    }

    private String getFilterQuery(Collection<String> values, String jqlKey)
    {
        if (values.isEmpty())
        {
            return "";
        }

        return " AND (" + jqlKey + " IN (" + String.join(",", values) + ") OR " + jqlKey + " IS EMPTY)";
    }

    /**
     * Gets the mapping of epics to low level tasks and stories.
     *
     * @param epicQueryResult The data model representation of a group of Epics.
     * @return The mapping of epics to low level tasks and stories.
     */
    private Map<Issue, List<Issue>> getEpicStoryMap(Issue.SearchResult epicQueryResult, Collection<String> projects)
    {
        String epicLink = fieldCustomIdMapping.get(EPIC_LINK);

        Map<String, Issue> epicIdIssueMap = new HashMap<>();
        Map<Issue, List<Issue>> epicStoryMap = new HashMap<>();

        epicQueryResult.issues.stream()
                .filter(issue -> issue.getField(epicLink) == null || issue.getField(epicLink) instanceof JSONNull)
                .filter(issue -> projects.contains(issue.getProject().getName()))
                .forEach(epic -> {
                    epicIdIssueMap.put(epic.getKey(), epic);
                    epicStoryMap.put(epic, new ArrayList<>());
                });

        epicQueryResult.issues.forEach(userStory -> {

            String epicId = unassignedEpic.getId();
            Object field = userStory.getField(epicLink);

            if (field instanceof String)
            {
                epicId = (String) field;
            }

            Issue epic = epicIdIssueMap.get(epicId);
            if (epic == null)
            {
                epic = unassignedEpic;
            }

            List<Issue> stories = epicStoryMap.getOrDefault(epic, new ArrayList<>());

            stories.add(userStory);
            epicStoryMap.put(epic, stories);
        });

        return epicStoryMap;
    }

    /**
     * Calls the REST API and parses the corresponding JSON object into data objects.
     *
     * @param query        A Jira Query Language (JQL) request. See https://confluence.atlassian.com/jiracore/blog/2015/07/search-jira-like-a-boss-with-jql
     *                     for additional details on how to create a request.
     * @param expandFields Fields from the request that should be populated. Each request will only populate a subset of
     *                     fields by default to reduce bandwidth and process requirements.
     * @return A Jira data object.
     * @throws JiraException If the query string is malformed or the query failed for any other reason.
     */
    private Issue.SearchResult searchIssues(String query, String expandFields) throws JiraException
    {
        JiraClient jiraClient = new JiraClient(httpClient, baseUrl, credentials);
        String storyPointCustomField = fieldCustomIdMapping.get(STORY_POINTS_KEY);
        String sprintKeyCustomField = fieldCustomIdMapping.get(SPRINT_KEY);
        String epicCustomField = fieldCustomIdMapping.get(EPIC_LINK);
        String programCustomField = fieldCustomIdMapping.get(PROGRAM_KEY);

        String includedFields = "project, key, summary, description, status, issuetype, created, resolutiondate, " +
                "issues, labels, assignee, assignee, reporter, priority, fixVersions, duedate, components, description," +
                storyPointCustomField + ", " + sprintKeyCustomField + ", " + epicCustomField + ", " + programCustomField;

        return jiraClient.searchIssues(query, includedFields, expandFields, 10_000, 0);
    }

    /**
     * Loads the mapping of custom identifiers in JIRA ticket fields to human readable names. If the field mappings have
     * been previously loaded then no additional actions will be taken.
     *
     * @param project The project to identify the custom field mappings from.
     */
    private void loadCustomFields(String project)
    {
        if (fieldCustomIdMapping.isEmpty())
        {
            try
            {
                JiraClient jiraClient = new JiraClient(httpClient, baseUrl, credentials);

                String epicsJQL = "project in (" + project + ")";

                fieldCustomIdMapping = Issue.getCustomFieldMappings(jiraClient.getRestClient(), epicsJQL);
            } catch (Exception exception)
            {
                logger.warn("Failed to load custom fields for project {}: ", project, exception);
            }
        }
    }

    /**
     * Converts the comma separated sprint characteristics into a map.
     *
     * @param sprintString A comma separated list of characteristics in a map.
     * @return A map of sprint property names to corresponding values.
     */
    private static Properties getSprint(String sprintString)
    {
        String propertiesFormat = sprintString.replaceAll(",", "\n");
        Properties properties = new Properties();

        try
        {
            properties.load(new StringReader(propertiesFormat));
        } catch (Exception exception)
        {
            logger.warn("Failed to retrieve sprint properties: ", exception);
        }

        return properties;
    }

    /**
     * A hollow JIRA issue representation to enable grouping tickets that don't have initiatives or epics.
     *
     * @since 1.0.0
     */
    private class EmptyIssue extends Issue
    {
        String key;

        EmptyIssue(String key)
        {
            super(null, null);
            this.key = key;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public Object getField(String name)
        {
            return "Unassigned";
        }

        @Override
        public List<String> getLabels()
        {
            return Collections.emptyList();
        }
    }
}
