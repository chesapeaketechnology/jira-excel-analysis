/*
 * Copyright (c) 2011, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.chesapeake.technology.ui.fx;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Login dialog to allow the user to authenticate with JIRA.
 *
 * @since 1.0.0
 */
public class LoginController implements Initializable
{
    private Parent configurationParent;
    private ConfigurationController configurationController;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorMessageLabel;

    @FXML
    private Button signInButton;

    @FXML
    private TextField jiraUrlField;

    public LoginController() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(LoginController.class.getClassLoader().getResource("fx/configure_reports.fxml"));

        configurationParent = loader.load();
        configurationController = loader.getController();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        String password = System.getenv("NEXUS_PASSWORD");

        if (password != null)
        {
            passwordField.setText(password);
        }
    }

    @FXML
    private void passwordAction(ActionEvent event)
    {
        handleSubmitButtonAction(event);
    }

    @FXML
    private void handleSubmitButtonAction(ActionEvent event)
    {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        setControlsDisabled(true);

        executorService.submit(() -> {
            try
            {
                //TODO: Show a quick loading bar to let the user know that credentials are being validated
                configurationController.setCredentials(jiraUrlField.getText(), usernameField.getText(), passwordField.getText());

                Platform.runLater(() -> {
                    stage.setScene(new Scene(configurationParent));
                    stage.setTitle("Generate a Report");
                });
            } catch (Exception e)
            {
                logger.warn("Invalid login credentials", e);
                errorMessageLabel.setVisible(true);
                setControlsDisabled(false);
            }
        });
    }

    private void setControlsDisabled(boolean disabled)
    {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        signInButton.setDisable(disabled);
    }
}
