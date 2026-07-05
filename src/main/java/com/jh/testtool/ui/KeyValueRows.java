package com.jh.testtool.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class KeyValueRows {

    private final VBox root = new VBox(6);
    private final VBox rows = new VBox(6);
    private final ScrollPane rowScroll = new ScrollPane(rows);
    private final String keyPrompt;
    private final String valuePrompt;
    private TextField focusedValueField;

    KeyValueRows(String title, String keyPrompt, String valuePrompt, boolean multiValue) {
        this.keyPrompt = keyPrompt;
        this.valuePrompt = valuePrompt;

        Label titleLabel = new Label(title);
        Button addButton = new Button("+");
        addButton.setOnAction(event -> addRow("", ""));

        HBox titleLine = new HBox(8, titleLabel, addButton);
        root.setPadding(new Insets(4, 0, 4, 0));
        rowScroll.setFitToWidth(true);
        rowScroll.setPrefHeight(150);
        rowScroll.setMinHeight(150);
        rowScroll.setMaxHeight(150);
        root.setPrefHeight(188);
        root.setMinHeight(188);
        root.setMaxHeight(188);
        root.getChildren().addAll(titleLine, rowScroll);
        addRow("", "");
    }

    VBox node() {
        return root;
    }

    Map<String, List<String>> toMultiValueMap() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (Row row : getRows()) {
            String key = row.keyField.getText().trim();
            if (!key.isBlank()) {
                values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row.valueField.getText());
            }
        }
        return values;
    }

    Map<String, String> toSingleValueMap() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Row row : getRows()) {
            String key = row.keyField.getText().trim();
            if (!key.isBlank()) {
                values.put(key, row.valueField.getText());
            }
        }
        return values;
    }

    void setMultiValueMap(Map<String, List<String>> values) {
        rows.getChildren().clear();
        if (values == null || values.isEmpty()) {
            addRow("", "");
            return;
        }
        values.forEach((key, rowValues) -> {
            if (rowValues == null || rowValues.isEmpty()) {
                addRow(key, "");
            } else {
                rowValues.forEach(value -> addRow(key, value));
            }
        });
    }

    void setSingleValueMap(Map<String, String> values) {
        rows.getChildren().clear();
        if (values == null || values.isEmpty()) {
            addRow("", "");
            return;
        }
        values.forEach(this::addRow);
    }

    void insertIntoFocusedValue(String text) {
        if (focusedValueField == null) {
            Row lastRow = getRows().getLast();
            focusedValueField = lastRow.valueField;
            focusedValueField.requestFocus();
        }
        focusedValueField.insertText(focusedValueField.getCaretPosition(), text);
    }

    private void addRow(String key, String value) {
        TextField keyField = new TextField(key);
        keyField.setPromptText(keyPrompt);
        HBox.setHgrow(keyField, Priority.ALWAYS);

        TextField valueField = new TextField(value);
        valueField.setPromptText(valuePrompt);
        valueField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                focusedValueField = valueField;
            }
        });
        HBox.setHgrow(valueField, Priority.ALWAYS);

        Button removeButton = new Button("-");
        HBox row = new HBox(6, keyField, valueField, removeButton);
        row.setUserData(new Row(keyField, valueField));
        removeButton.setOnAction(event -> {
            rows.getChildren().remove(row);
            if (rows.getChildren().isEmpty()) {
                addRow("", "");
            }
        });
        rows.getChildren().add(row);
    }

    private List<Row> getRows() {
        return rows.getChildren().stream()
                .map(node -> (Row) node.getUserData())
                .toList();
    }

    private record Row(TextField keyField, TextField valueField) {
    }
}
