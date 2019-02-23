package com.chesapeake.technology.ui.fx;

import com.chesapeake.technology.JiraRestClient;
import com.chesapeake.technology.excel.ExcelFileWriter;
import com.chesapeake.technology.model.IJiraIssueListener;
import com.chesapeake.technology.model.IssueWrapper;
import com.typesafe.config.ConfigFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeItem;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.controlsfx.control.CheckTreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Allow the user to configure how their report is generated via a JavaFX UI.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since 1.0.0
 */
public class ConfigurationController implements Initializable, IJiraIssueListener
{
    private String username;

    private Map<Issue, List<Issue>> initiativeEpicMap;
    private Map<Issue, List<Issue>> epicStoryMap;
    private Map<String, String> fieldCustomIdMap;

    private CheckBoxTreeItem rootItem = new CheckBoxTreeItem<>("");
    private CheckBoxTreeItem filterRootItem = new CheckBoxTreeItem<>("Filters");
    private CheckBoxTreeItem presenceCheckRootItem = new CheckBoxTreeItem<>("Presence Test");
    private CheckBoxTreeItem initiativeRootItem = new CheckBoxTreeItem<>("Initiatives");
    private CheckBoxTreeItem labelRootItem = new CheckBoxTreeItem<>("Labels");
    private CheckBoxTreeItem sprintRootItem = new CheckBoxTreeItem<>("Sprints");

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @FXML
    private CheckTreeView jiraTreeView;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button generateReportButton;

    @FXML
    private CheckBox masterCheckBox;

    @FXML
    private CheckBox developerMetricsCheckBox;

    @FXML
    private CheckBox summaryCheckBox;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        jiraTreeView.setRoot(rootItem);

        rootItem.getChildren().addAll(filterRootItem, presenceCheckRootItem);

        filterRootItem.getChildren().addAll(initiativeRootItem, labelRootItem, sprintRootItem);
        filterRootItem.setExpanded(true);
        filterRootItem.setSelected(true);
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
        this.initiativeEpicMap = initiativeEpicMap;
        this.epicStoryMap = epicStoryMap;

        Platform.runLater(() -> {
            initiativeEpicMap.forEach((initiative, epics) -> {
                CheckBoxTreeItem initiativeItem = new CheckBoxTreeItem<>(new IssueWrapper(initiative));

                initiativeRootItem.getChildren().add(initiativeItem);
                initiativeItem.setSelected(true);
                epics.forEach(epic -> {
                    CheckBoxTreeItem epicItem = new CheckBoxTreeItem<>(new IssueWrapper(epic));

                    initiativeItem.getChildren().add(epicItem);
                    epicItem.setSelected(true);
                });
            });

            epicStoryMap.values().stream()
                    .flatMap(Collection::stream)
                    .flatMap(issue -> issue.getLabels().stream())
                    .distinct()
                    .sorted(String::compareToIgnoreCase)
                    .forEach(labelText -> {
                        CheckBoxTreeItem filterLabelItem = new CheckBoxTreeItem<>(labelText);
                        CheckBoxTreeItem presenceLabelItem = new CheckBoxTreeItem<>(labelText);

                        labelRootItem.getChildren().add(filterLabelItem);
                        presenceCheckRootItem.getChildren().add(presenceLabelItem);

                        filterLabelItem.setSelected(true);
                        presenceLabelItem.setSelected(false);
                    });

            generateReportButton.setDisable(false);
            progressBar.setProgress(100);

            jiraTreeView.setDisable(false);
        });
    }

    @FXML
    private void generateReportAction()
    {
        try
        {
            logger.info("Generating report action detected");

            Collection<Issue> activeInitiatives = new ArrayList<>();
            Collection<Issue> activeEpics = new ArrayList<>();
            Collection<String> activeSprints = new ArrayList<>();
            Collection<String> activeLabels = new ArrayList<>();
            List<String> presenceChecks = new ArrayList<>();

            for (Object checkedItem : jiraTreeView.getCheckModel().getCheckedItems())
            {
                TreeItem treeItem = (TreeItem) checkedItem;
                Object value = treeItem.getValue();

                boolean initiative = initiativeRootItem.equals(treeItem.getParent());
                boolean sprint = sprintRootItem.equals(treeItem.getParent());
                boolean label = labelRootItem.equals(treeItem.getParent());
                boolean presenceCheck = presenceCheckRootItem.equals(treeItem.getParent());
                boolean epic = !sprint && !label && !initiative && !presenceCheck && value instanceof IssueWrapper;

                if (initiative)
                {
                    activeInitiatives.add(((IssueWrapper) value).getIssue());
                } else if (epic)
                {
                    activeEpics.add(((IssueWrapper) value).getIssue());
                } else if (sprint)
                {
                    activeSprints.add(value.toString());
                } else if (label)
                {
                    activeLabels.add(value.toString());
                } else if (presenceCheck)
                {
                    presenceChecks.add(value.toString());
                }
            }

            logger.info("Preparing to create excel writer");

            ExcelFileWriter excelFileWriter = new ExcelFileWriter(initiativeEpicMap, epicStoryMap, fieldCustomIdMap);

            //TODO: Pass these settings through a Config object
//            excelFileWriter.setIncludeMasterReport(masterCheckBox.isSelected());
//            excelFileWriter.setIncludeDeveloperMetrics(developerMetricsCheckBox.isSelected());
//            excelFileWriter.setIncludeSummaryMetrics(summaryCheckBox.isSelected());

            logger.info("Setting active excel data");
            excelFileWriter.setActiveData(activeInitiatives, activeEpics, activeSprints, activeLabels, presenceChecks);

            excelFileWriter.createJIRAReport(ConfigFactory.defaultApplication());

            updateUserPreferences();
        } catch (Exception exception)
        {
            logger.warn("Failed to generate an excel report: ", exception);
        }
    }

    /**
     * Set the credentials used to authenticate with JIRA.
     *
     * @param username The unique identifier of the user.
     * @param password The key used to verify the user's identity.
     * @throws JiraException If the username / password combination is incorrect.
     */
    void setCredentials(String username, String password) throws JiraException
    {
        this.username = username;

        JiraRestClient requestClient = new JiraRestClient("https://jira.net/", username, password, false);

        fieldCustomIdMap = requestClient.getFieldCustomIdMapping();
        requestClient.addIssueListener(this);
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        //TODO: Build up the config before calling this
        Executors.newSingleThreadExecutor().submit(() -> requestClient.loadJiraIssues(ConfigFactory.defaultApplication()));
        executorService.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            double progress = progressBar.getProgress();

            if (progress > .90)
            {
                executorService.shutdown();
            }

            progressBar.setProgress(progress + .02);
        }), 0, 1, TimeUnit.SECONDS);
    }

    private void updateUserPreferences()
    {
        Set<String> initiativeTitles = new HashSet<>();
        Set<String> epicTitles = new HashSet<>();
        Set<String> labelTitles = new HashSet<>();
        Set<String> labelPresenceTitles = new HashSet<>();
        Set<String> sprintTitles = new HashSet<>();

        for (CheckBoxTreeItem initiativeTreeItem : (List<CheckBoxTreeItem>) initiativeRootItem.getChildren())
        {
            if (!initiativeTreeItem.isSelected())
            {
                initiativeTitles.add(initiativeTreeItem.getValue().toString());
            }

            for (CheckBoxTreeItem epicTreeItem : (List<CheckBoxTreeItem>) initiativeTreeItem.getChildren())
            {
                if (!epicTreeItem.isSelected())
                {
                    epicTitles.add(epicTreeItem.getValue().toString());
                }
            }
        }

        for (CheckBoxTreeItem labelItem : (List<CheckBoxTreeItem>) labelRootItem.getChildren())
        {
            if (!labelItem.isSelected())
            {
                labelTitles.add(labelItem.getValue().toString());
            }
        }

        for (CheckBoxTreeItem presenceCheckItem : (List<CheckBoxTreeItem>) presenceCheckRootItem.getChildren())
        {
            if (presenceCheckItem.isSelected())
            {
                labelPresenceTitles.add(presenceCheckItem.getValue().toString());
            }
        }

        for (CheckBoxTreeItem sprintItem : (List<CheckBoxTreeItem>) sprintRootItem.getChildren())
        {
            if (!sprintItem.isSelected())
            {
                sprintTitles.add(sprintItem.getValue().toString());
            }
        }
    }
}
