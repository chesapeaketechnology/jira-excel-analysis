package com.chesapeaketechnology.model;

import net.rcarz.jiraclient.Issue;

import java.util.Comparator;

/**
 * Compares stories based on their names and completion status.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 */
public class StoryIssueComparator implements Comparator<Issue>
{

    @Override
    public int compare(Issue first, Issue second)
    {
        String firstStatus = first.getStatus().getName();
        String secondStatus = second.getStatus().getName();

        String firstProject = first.getProject().getName();
        String secondProject = second.getProject().getName();

        if (firstProject.equals(secondProject))
        {
            if (firstStatus.equals(secondStatus))
            {
                return 0;
            } else if (firstStatus.equals("Done"))
            {
                return 1;
            } else if (secondStatus.equals("Done"))
            {
                return -1;
            } else
            {
                return firstStatus.compareTo(secondStatus);
            }
        } else
        {
            return firstProject.compareTo(secondProject);
        }
    }
}
