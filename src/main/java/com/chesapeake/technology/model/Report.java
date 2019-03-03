package com.chesapeake.technology.model;

import java.util.Collection;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class Report
{
    private String fileName;
    private Filter filters;
    private Collection<String> presence;

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public Collection<String> getPresence()
    {
        return presence;
    }

    public void setPresence(Collection<String> presence)
    {
        this.presence = presence;
    }

    public Filter getFilters()
    {
        return filters;
    }

    public void setFilters(Filter filters)
    {
        this.filters = filters;
    }

    public static class Filter
    {
        private Collection<String> sprints;
        private Collection<String> labels;

        public Collection<String> getSprintNames()
        {
            return sprints;
        }

        public void setSprintNames(Collection<String> sprintNames)
        {
            this.sprints = sprintNames;
        }

        public Collection<String> getLabels()
        {
            return labels;
        }

        public void setLabels(Collection<String> labels)
        {
            this.labels = labels;
        }
    }
}
