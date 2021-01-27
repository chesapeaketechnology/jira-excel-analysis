package com.chesapeake.technology.ui.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Generates an excel report providing summaries of elements across JIRA as well as metrics across an individual
 * developer's work history.
 *
 * @since 1.0.0
 */
public class LoginApplication extends Application
{
    @Override
    public void start(Stage stage) throws Exception
    {
        Parent root = FXMLLoader.load(LoginApplication.class.getClassLoader().getResource("fx/sign_in.fxml"));

        stage.setTitle("Login to JIRA");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
