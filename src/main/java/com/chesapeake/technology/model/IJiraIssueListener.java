package com.chesapeake.technology.model;

import net.rcarz.jiraclient.Issue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Callback hooks to be notified when various stages of querying JIRA tickets is completed.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @see com.chesapeake.technology.JiraRestClient
 * @since 1.0.0
 */
public interface IJiraIssueListener
{
    /**
     * Notification when the highest level of issues has been queried. Depending on user configurations this may never
     * be called.
     *
     * @param initiatives JIRA tickets that can extend over months to years.
     */
    void initiativesLoaded(Collection<Issue> initiatives);

    /**
     * The link between an initiative and nested issues has been established.
     *
     * @param parent      The root JIRA ticket.
     * @param childIssues The child JIRA tickets.
     */
    void childrenRetrieved(Issue parent, List<Issue> childIssues);

    /**
     * Notification that all JIRA queries have been completed with all of the JIRA tickets that were retrieved.
     *
     * @param initiativeEpicMap Mapping from Initiative JIRA tickets to Epic tickets.
     * @param epicStoryMap      Mapping from Epics JIRA tickets to Story tickets.
     * @param fieldCustomIdMap  Mapping of custom identifier numbers from JIRA to the human readable forms.
     */
    void allIssuesRetrieved(Map<Issue, List<Issue>> initiativeEpicMap, Map<Issue,
            List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap);
}
