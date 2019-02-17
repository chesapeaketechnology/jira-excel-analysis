package com.chesapeake.technology.data;

import com.chesapeake.technology.JiraRestClient;
import net.rcarz.jiraclient.Attachment;
import net.rcarz.jiraclient.ChangeLog;
import net.rcarz.jiraclient.Comment;
import net.rcarz.jiraclient.Component;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.IssueLink;
import net.rcarz.jiraclient.IssueType;
import net.rcarz.jiraclient.Priority;
import net.rcarz.jiraclient.Project;
import net.rcarz.jiraclient.RemoteLink;
import net.rcarz.jiraclient.Resolution;
import net.rcarz.jiraclient.Security;
import net.rcarz.jiraclient.Status;
import net.rcarz.jiraclient.TimeTracking;
import net.rcarz.jiraclient.Transition;
import net.rcarz.jiraclient.User;
import net.rcarz.jiraclient.Version;
import net.rcarz.jiraclient.Votes;
import net.rcarz.jiraclient.Watches;
import net.rcarz.jiraclient.WorkLog;
import net.sf.json.JSONArray;
import org.joda.time.DateTime;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class MockIssue extends Issue
{
    private static Random random = new Random();

    private int timeSpent = random.nextInt();
    private int timeEstimate = random.nextInt();

    private String issueKey = UUID.randomUUID().toString();
    private String description = UUID.randomUUID().toString();
    private String summary = UUID.randomUUID().toString();

    private Date dueDate = Calendar.getInstance().getTime();
    private Date createdDate = Calendar.getInstance().getTime();
    private Date updatedDate = Calendar.getInstance().getTime();
    private Date resolutionDate = Calendar.getInstance().getTime();

    private List<String> labels = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    private Map<String, Object> issueFieldValueMap = createIssueFieldValueMap();

    private MockUser assignee = new MockUser();
    private MockUser reporter = new MockUser();
    private MockStatus status = new MockStatus();
    private MockProject mockProject = new MockProject();

    //Creates an Issue with random attributes
    public MockIssue()
    {
        super(null, null);
    }

    public void setFieldMap(Map<String, Object> fieldValueMap)
    {
        this.issueFieldValueMap = fieldValueMap;
    }

    @Override
    public Object getField(String name)
    {
        return issueFieldValueMap.get(name);
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    @Override
    public ChangeLog getChangeLog()
    {
        //TODO: Corrigan
        return null;
    }

    @Override
    public String getKey()
    {
        return issueKey;
    }

    @Override
    public User getAssignee()
    {
        return assignee;
    }

    @Override
    public List<Comment> getComments()
    {
        return Collections.emptyList();
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public Date getDueDate()
    {
        return dueDate;
    }

    @Override
    public List<Version> getFixVersions()
    {
        return Collections.emptyList();
    }

    @Override
    public IssueType getIssueType()
    {
        return null;
    }

    @Override
    public List<String> getLabels()
    {
        return labels;
    }

    @Override
    public Project getProject()
    {
        return mockProject;
    }

    @Override
    public User getReporter()
    {
        return reporter;
    }

    @Override
    public Status getStatus()
    {
        return status;
    }

    @Override
    public String getSummary()
    {
        return summary;
    }

    @Override
    public List<WorkLog> getWorkLogs()
    {
        return Collections.emptyList();
    }

    @Override
    public List<WorkLog> getAllWorkLogs()
    {
        return Collections.emptyList();
    }

    @Override
    public Integer getTimeSpent()
    {
        return timeSpent;
    }

    @Override
    public Integer getTimeEstimate()
    {
        return timeEstimate;
    }

    @Override
    public Date getCreatedDate()
    {
        return createdDate;
    }

    @Override
    public Date getUpdatedDate()
    {
        return updatedDate;
    }

    @Override
    public boolean delete(boolean deleteSubtasks)
    {
        return false;
    }

    @Override
    public List<Transition> getTransitions()
    {
        return Collections.emptyList();
    }

    @Override
    public void addAttachment(File file)
    {
    }

    @Override
    public void addRemoteLink(String url, String title, String summary)
    {
    }

    @Override
    public FluentRemoteLink remoteLink()
    {
        return null;
    }

    @Override
    public void addAttachments(NewAttachment... attachments)
    {
    }

    @Override
    public void removeAttachment(String attachmentId)
    {
    }

    @Override
    public Comment addComment(String body)
    {
        return null;
    }

    @Override
    public Comment addComment(String body, String visType, String visName)
    {
        return null;
    }

    @Override
    public WorkLog addWorkLog(String comment, DateTime startDate, long timeSpentSeconds)
    {
        return null;
    }

    @Override
    public void link(String issue, String type)
    {
    }

    @Override
    public void link(String issue, String type, String body)
    {
    }

    @Override
    public void link(String issue, String type, String body, String visType, String visName)
    {
    }

    @Override
    public FluentCreate createSubtask()
    {
        return null;
    }

    @Override
    public void refresh()
    {
    }

    @Override
    public void refresh(String includedFields)
    {
    }

    @Override
    public FluentTransition transition()
    {
        return null;
    }

    @Override
    public FluentUpdate update()
    {
        return null;
    }

    @Override
    public void vote()
    {
    }

    @Override
    public void unvote()
    {
    }

    @Override
    public void addWatcher(String username)
    {
    }

    @Override
    public void deleteWatcher(String username)
    {
    }

    @Override
    public List<Attachment> getAttachments()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Component> getComponents()
    {
        return Collections.emptyList();
    }

    @Override
    public List<IssueLink> getIssueLinks()
    {
        return Collections.emptyList();
    }

    @Override
    public Issue getParent()
    {
        return null;
    }

    @Override
    public Priority getPriority()
    {
        return null;
    }

    @Override
    public List<RemoteLink> getRemoteLinks()
    {
        return Collections.emptyList();
    }

    @Override
    public Resolution getResolution()
    {
        return null;
    }

    @Override
    public Date getResolutionDate()
    {
        return resolutionDate;
    }

    @Override
    public List<Issue> getSubtasks()
    {
        return Collections.emptyList();
    }

    @Override
    public TimeTracking getTimeTracking()
    {
        return null;
    }

    @Override
    public List<Version> getVersions()
    {
        return Collections.emptyList();
    }

    @Override
    public Votes getVotes()
    {
        return null;
    }

    @Override
    public Watches getWatches()
    {
        return null;
    }

    @Override
    public Security getSecurity()
    {
        return null;
    }

    private Map<String, Object> createIssueFieldValueMap()
    {
        Map<String, Object> issueFieldValueMap = new HashMap<>();

        issueFieldValueMap.put(JiraRestClient.SPRINT_KEY, new JSONArray());

        return issueFieldValueMap;
    }
}
