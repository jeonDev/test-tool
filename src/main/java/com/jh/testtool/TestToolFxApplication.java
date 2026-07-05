package com.jh.testtool;

import com.jh.testtool.ui.MainViewFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class TestToolFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        applicationContext = SpringApplication.run(TestToolApplication.class, args);
    }

    @Override
    public void start(Stage stage) {
        MainViewFactory mainViewFactory = applicationContext.getBean(MainViewFactory.class);
        stage.setTitle("test-tool");
        stage.setScene(new Scene(mainViewFactory.create(), 1000, 720));
        stage.show();
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        Platform.exit();
    }
}
