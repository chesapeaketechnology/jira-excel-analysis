package com.chesapeake.technology.model;

import net.rcarz.jiraclient.Issue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
public interface IJiraIssueListener
{
    void initiativesLoaded(Collection<Issue> initiatives);

    void childrenRetrieved(Issue parent, List<Issue> childIssues);

    void allIssuesRetrieved(Map<Issue, List<Issue>> initiativeEpicMap, Map<Issue,
            List<Issue>> epicStoryMap, Map<String, String> fieldCustomIdMap);
}
