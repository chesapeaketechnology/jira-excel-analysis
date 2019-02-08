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

    public IssueWrapper(Issue issue)
    {
        this(issue, issue.getSummary());
    }

    private IssueWrapper(Issue issue, String displayText)
    {
        this.issue = issue;
        this.displayText = displayText;
    }

    @Override
    public String toString()
    {
        return displayText;
    }

    public Issue getIssue()
    {
        return issue;
    }

    public String getDisplayText()
    {
        return displayText;
    }
}
