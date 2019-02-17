package com.chesapeake.technology.model;

import net.rcarz.jiraclient.Issue;

/**
 * Model representation of a JIRA issue used to customize the issue's name within a UI component.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 */
public class IssueWrapper
{
    private Issue issue;
    private String displayText;

    /**
     * Creates an issue wrapper.
     *
     * @param issue The JIRA ticket to wrap.
     */
    public IssueWrapper(Issue issue)
    {
        this.issue = issue;
        this.displayText = issue.getSummary();
    }

    @Override
    public String toString()
    {
        return displayText;
    }

    /**
     * Get the issue that was wrapped.
     *
     * @return The issue being wrapped.
     */
    public Issue getIssue()
    {
        return issue;
    }
}
