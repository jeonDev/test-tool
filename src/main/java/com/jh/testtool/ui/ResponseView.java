package com.jh.testtool.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

final class ResponseView {

    private final VBox root = new VBox(6);
    private final Label content = new Label();

    ResponseView(String title, double height) {
        Label titleLabel = new Label(title);
        content.setText("");
        content.setWrapText(false);
        content.setStyle("-fx-font-family: monospace;");
        content.setPadding(new Insets(8));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setPrefHeight(height);
        scrollPane.setMinHeight(height);
        scrollPane.setMaxHeight(height);
        scrollPane.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border;");

        root.getChildren().addAll(titleLabel, scrollPane);
    }

    VBox node() {
        return root;
    }

    void setText(String text) {
        content.setText(text == null ? "" : text);
    }

    void clear() {
        content.setText("");
    }
}
