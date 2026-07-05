package com.jh.testtool.ui;

import com.jh.testtool.application.http.ExecuteHttpRequestUseCase;
import com.jh.testtool.application.testcase.ExecuteSequentialHttpTestCaseUseCase;
import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;
import com.jh.testtool.domain.testcase.HttpTestCase;
import com.jh.testtool.domain.testcase.HttpTestStep;
import com.jh.testtool.domain.testcase.StepExecutionResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class MainViewFactory {

    private final ExecuteHttpRequestUseCase executeHttpRequestUseCase;
    private final ExecuteSequentialHttpTestCaseUseCase executeSequentialHttpTestCaseUseCase;

    public MainViewFactory(
            ExecuteHttpRequestUseCase executeHttpRequestUseCase,
            ExecuteSequentialHttpTestCaseUseCase executeSequentialHttpTestCaseUseCase
    ) {
        this.executeHttpRequestUseCase = executeHttpRequestUseCase;
        this.executeSequentialHttpTestCaseUseCase = executeSequentialHttpTestCaseUseCase;
    }

    public Parent create() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Request", createRequestTab()));
        tabPane.getTabs().add(new Tab("Test Case", createTestCaseTab()));
        tabPane.getTabs().forEach(tab -> tab.setClosable(false));

        BorderPane root = new BorderPane(tabPane);
        root.setPadding(new Insets(12));
        return root;
    }

    private Parent createRequestTab() {
        ComboBox<String> protocolSelect = createProtocolSelect();
        ComboBox<String> httpMethodSelect = createHttpMethodSelect();

        TextField urlField = new TextField();
        urlField.setPromptText("url");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        Label statusLabel = new Label();

        HBox requestLine = new HBox(8, protocolSelect, httpMethodSelect, urlField, sendButton);

        KeyValueRows headerRows = new KeyValueRows("Headers", "Header name", "Value", true);
        KeyValueRows bodyRows = new KeyValueRows("Body Fields", "Field name", "Value", false);

        ResponseView responseView = new ResponseView("Response", 170);

        sendButton.setOnAction(event -> {
            HttpRequestCommand request = HttpTextMapper.toRequest(
                    httpMethodSelect.getValue(),
                    urlField.getText(),
                    headerRows.toMultiValueMap(),
                    HttpTextMapper.toJsonBody(bodyRows.toSingleValueMap())
            );
            runSingleRequest(request, sendButton, statusLabel, responseView);
        });

        VBox form = new VBox(10, requestLine, headerRows.node(), bodyRows.node(), responseView.node(), statusLabel);
        return form;
    }

    private Parent createTestCaseTab() {
        ObservableList<StepDraft> steps = FXCollections.observableArrayList();
        steps.add(new StepDraft("Step 1", "GET", "", Map.of(), Map.of()));

        StepSelection selection = new StepSelection(steps.getFirst());
        VBox flow = new VBox(8);

        TextField nameField = new TextField();
        nameField.setPromptText("Step name");

        ComboBox<String> protocolSelect = createProtocolSelect();
        ComboBox<String> methodSelect = createHttpMethodSelect();

        TextField urlField = new TextField();
        urlField.setPromptText("url. ex) localhost:8080/users/{{last.json.id}}");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        KeyValueRows headerRows = new KeyValueRows("Headers", "Header name", "Value", true);
        KeyValueRows bodyRows = new KeyValueRows("Body Fields", "Field name", "Value", false);

        ComboBox<String> responseSourceSelect = new ComboBox<>();
        responseSourceSelect.setPrefWidth(120);
        ComboBox<String> responseValueSelect = new ComboBox<>();
        responseValueSelect.getItems().addAll("json", "header", "body", "status");
        responseValueSelect.setValue("json");
        responseValueSelect.setPrefWidth(100);
        TextField responsePathField = new TextField();
        responsePathField.setPromptText("json path or header name. ex) data.id, token, Content-Type");
        HBox.setHgrow(responsePathField, Priority.ALWAYS);
        TextField tokenField = new TextField();
        tokenField.setEditable(false);
        HBox.setHgrow(tokenField, Priority.ALWAYS);

        Runnable refreshToken = () -> tokenField.setText(buildResponseToken(
                responseSourceSelect.getValue(),
                responseValueSelect.getValue(),
                responsePathField.getText()
        ));
        responseSourceSelect.setOnAction(event -> refreshToken.run());
        responseValueSelect.setOnAction(event -> refreshToken.run());
        responsePathField.textProperty().addListener((observable, oldValue, newValue) -> refreshToken.run());

        Button insertUrlButton = new Button("Insert URL");
        insertUrlButton.setOnAction(event -> insertAtCaret(urlField, tokenField.getText()));
        Button insertHeaderButton = new Button("Insert Header Value");
        insertHeaderButton.setOnAction(event -> headerRows.insertIntoFocusedValue(tokenField.getText()));
        Button insertBodyButton = new Button("Insert Body Value");
        insertBodyButton.setOnAction(event -> bodyRows.insertIntoFocusedValue(tokenField.getText()));

        Button applyButton = new Button("Apply");
        Button runButton = new Button("Run Sequence");
        Label statusLabel = new Label();

        ResponseView resultView = new ResponseView("Sequence Result", 190);

        Runnable saveSelected = () -> {
            if (selection.selected != null) {
                saveDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);
                refreshFlow(flow, steps, selection, nameField, methodSelect, urlField, headerRows, bodyRows, responseSourceSelect);
            }
        };

        Runnable loadSelected = () -> loadDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);

        HBox nameLine = new HBox(8, nameField);
        HBox editorTop = new HBox(8, protocolSelect, methodSelect, urlField);
        HBox responseTokenLine = new HBox(8, responseSourceSelect, responseValueSelect, responsePathField);
        HBox tokenActionLine = new HBox(8, tokenField, insertUrlButton, insertHeaderButton, insertBodyButton);
        VBox previousResponseBox = new VBox(
                6,
                new Label("Use Previous Response"),
                new Label("Example: Step 2 can insert Step 1 response as {{last.json.data.id}}."),
                responseTokenLine,
                tokenActionLine
        );
        previousResponseBox.setPadding(new Insets(8, 0, 4, 0));
        HBox actions = new HBox(8, applyButton, runButton, statusLabel);
        VBox editor = new VBox(10, nameLine, editorTop, previousResponseBox, headerRows.node(), bodyRows.node(), actions, resultView.node());

        applyButton.setOnAction(event -> saveSelected.run());

        runButton.setOnAction(event -> {
            saveSelected.run();
            try {
                HttpTestCase testCase = new HttpTestCase(steps.stream().map(StepDraft::toStep).toList());
                runSequence(testCase, runButton, statusLabel, resultView);
            } catch (RuntimeException e) {
                resultView.setText(formatError(e));
            }
        });

        refreshFlow(flow, steps, selection, nameField, methodSelect, urlField, headerRows, bodyRows, responseSourceSelect);
        loadSelected.run();
        refreshToken.run();

        ScrollPane flowScroll = new ScrollPane(flow);
        flowScroll.setFitToWidth(true);
        flowScroll.setPrefWidth(280);

        SplitPane splitPane = new SplitPane(flowScroll, editor);
        splitPane.setDividerPositions(0.32);
        return splitPane;
    }

    private void refreshFlow(
            VBox flow,
            ObservableList<StepDraft> steps,
            StepSelection selection,
            TextField nameField,
            ComboBox<String> methodSelect,
            TextField urlField,
            KeyValueRows headerRows,
            KeyValueRows bodyRows,
            ComboBox<String> responseSourceSelect
    ) {
        flow.getChildren().clear();
        flow.setPadding(new Insets(8));
        flow.getChildren().add(new Label("Test Case Flow"));

        for (int index = 0; index < steps.size(); index++) {
            StepDraft draft = steps.get(index);
            Button stepButton = new Button(formatStepTitle(index + 1, draft));
            stepButton.setMaxWidth(Double.MAX_VALUE);
            stepButton.setStyle(draft == selection.selected
                    ? "-fx-alignment: center-left; -fx-font-weight: bold;"
                    : "-fx-alignment: center-left;");
            int stepIndex = index;
            stepButton.setOnAction(event -> {
                saveDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);
                selection.selected = steps.get(stepIndex);
                loadDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);
                refreshFlow(flow, steps, selection, nameField, methodSelect, urlField, headerRows, bodyRows, responseSourceSelect);
            });
            flow.getChildren().add(stepButton);

            if (index < steps.size() - 1) {
                Label arrow = new Label("|");
                arrow.setMaxWidth(Double.MAX_VALUE);
                arrow.setStyle("-fx-alignment: center;");
                flow.getChildren().add(arrow);
            }
        }

        Button addButton = new Button("+ Add Next Step");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(event -> {
            saveDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);
            StepDraft draft = new StepDraft("Step " + (steps.size() + 1), "GET", "", Map.of(), Map.of());
            steps.add(draft);
            selection.selected = draft;
            loadDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);
            refreshFlow(flow, steps, selection, nameField, methodSelect, urlField, headerRows, bodyRows, responseSourceSelect);
        });
        flow.getChildren().add(addButton);

        Button removeButton = new Button("- Remove Selected");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setDisable(steps.size() == 1);
        removeButton.setOnAction(event -> {
            int selectedIndex = steps.indexOf(selection.selected);
            if (selectedIndex >= 0 && steps.size() > 1) {
                steps.remove(selectedIndex);
                selection.selected = steps.get(Math.min(selectedIndex, steps.size() - 1));
                loadDraft(selection.selected, nameField, methodSelect, urlField, headerRows, bodyRows);
                refreshFlow(flow, steps, selection, nameField, methodSelect, urlField, headerRows, bodyRows, responseSourceSelect);
            }
        });
        flow.getChildren().add(removeButton);
        refreshResponseSources(responseSourceSelect, steps, selection.selected);
    }

    private void refreshResponseSources(
            ComboBox<String> responseSourceSelect,
            ObservableList<StepDraft> steps,
            StepDraft selected
    ) {
        String currentValue = responseSourceSelect.getValue();
        responseSourceSelect.getItems().clear();
        responseSourceSelect.getItems().add("last");
        for (int index = 0; index < steps.indexOf(selected); index++) {
            responseSourceSelect.getItems().add("step." + (index + 1));
        }
        if (currentValue != null && responseSourceSelect.getItems().contains(currentValue)) {
            responseSourceSelect.setValue(currentValue);
        } else {
            responseSourceSelect.setValue("last");
        }
    }

    private String buildResponseToken(String source, String valueType, String path) {
        String normalizedSource = source == null || source.isBlank() ? "last" : source;
        String normalizedType = valueType == null || valueType.isBlank() ? "json" : valueType;
        String normalizedPath = path == null ? "" : path.trim();
        if ("body".equals(normalizedType) || "status".equals(normalizedType)) {
            return "{{" + normalizedSource + "." + normalizedType + "}}";
        }
        if (normalizedPath.isBlank()) {
            return "";
        }
        return "{{" + normalizedSource + "." + normalizedType + "." + normalizedPath + "}}";
    }

    private void insertAtCaret(TextField textField, String text) {
        textField.insertText(textField.getCaretPosition(), text);
        textField.requestFocus();
    }

    private String formatStepTitle(int index, StepDraft draft) {
        String name = draft.name == null || draft.name.isBlank() ? "Step " + index : draft.name;
        String url = draft.url == null || draft.url.isBlank() ? "No URL" : draft.url;
        return index + ". " + name + System.lineSeparator() + draft.method + "  " + url;
    }

    private void runSingleRequest(
            HttpRequestCommand request,
            Button sendButton,
            Label statusLabel,
            ResponseView responseView
    ) {
        Task<HttpResponseResult> task = new Task<>() {
            @Override
            protected HttpResponseResult call() {
                return executeHttpRequestUseCase.execute(request);
            }
        };

        sendButton.setDisable(true);
        statusLabel.setText("Sending...");
        responseView.clear();
        task.setOnSucceeded(event -> {
            responseView.setText(HttpTextMapper.formatResponse(task.getValue()));
            statusLabel.setText("Done");
            sendButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            responseView.setText(formatError(task.getException()));
            statusLabel.setText("Failed");
            sendButton.setDisable(false);
        });
        new Thread(task, "http-request").start();
    }

    private void runSequence(
            HttpTestCase testCase,
            Button runButton,
            Label statusLabel,
            ResponseView resultView
    ) {
        Task<List<StepExecutionResult>> task = new Task<>() {
            @Override
            protected List<StepExecutionResult> call() {
                return executeSequentialHttpTestCaseUseCase.execute(testCase);
            }
        };

        runButton.setDisable(true);
        statusLabel.setText("Running...");
        resultView.clear();
        task.setOnSucceeded(event -> {
            resultView.setText(formatStepResults(task.getValue()));
            statusLabel.setText("Done");
            runButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            resultView.setText(formatError(task.getException()));
            statusLabel.setText("Failed");
            runButton.setDisable(false);
        });
        new Thread(task, "http-sequence").start();
    }

    private String formatStepResults(List<StepExecutionResult> results) {
        StringBuilder builder = new StringBuilder();
        for (StepExecutionResult result : results) {
            builder.append("### ")
                    .append(result.stepNumber())
                    .append(". ")
                    .append(result.stepName())
                    .append(System.lineSeparator());
            builder.append(result.resolvedRequest().method())
                    .append(' ')
                    .append(result.resolvedRequest().url())
                    .append(System.lineSeparator());
            if (!result.resolvedRequest().headers().isEmpty()) {
                builder.append(HttpTextMapper.formatHeaders(result.resolvedRequest().headers()));
            }
            builder.append(HttpTextMapper.formatResponse(result.response()))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private ComboBox<String> createProtocolSelect() {
        ComboBox<String> protocolSelect = new ComboBox<>();
        protocolSelect.getItems().add("http");
        protocolSelect.setValue("http");
        protocolSelect.setPrefWidth(100);
        return protocolSelect;
    }

    private ComboBox<String> createHttpMethodSelect() {
        ComboBox<String> httpMethodSelect = new ComboBox<>();
        httpMethodSelect.getItems().addAll("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
        httpMethodSelect.setValue("GET");
        httpMethodSelect.setPrefWidth(110);
        return httpMethodSelect;
    }

    private void saveDraft(
            StepDraft draft,
            TextField nameField,
            ComboBox<String> methodSelect,
            TextField urlField,
            KeyValueRows headerRows,
            KeyValueRows bodyRows
    ) {
        if (draft == null) {
            return;
        }
        draft.name = nameField.getText();
        draft.method = methodSelect.getValue();
        draft.url = urlField.getText();
        draft.headers = new LinkedHashMap<>(headerRows.toMultiValueMap());
        draft.bodyFields = new LinkedHashMap<>(bodyRows.toSingleValueMap());
    }

    private void loadDraft(
            StepDraft draft,
            TextField nameField,
            ComboBox<String> methodSelect,
            TextField urlField,
            KeyValueRows headerRows,
            KeyValueRows bodyRows
    ) {
        nameField.setText(draft.name);
        methodSelect.setValue(draft.method);
        urlField.setText(draft.url);
        headerRows.setMultiValueMap(draft.headers);
        bodyRows.setSingleValueMap(draft.bodyFields);
    }

    private String formatError(Throwable throwable) {
        Throwable target = throwable;
        if (target.getCause() != null) {
            target = target.getCause();
        }
        StringWriter stringWriter = new StringWriter();
        target.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private static final class StepSelection {
        private StepDraft selected;

        private StepSelection(StepDraft selected) {
            this.selected = selected;
        }
    }

    private static final class StepDraft {
        private String name;
        private String method;
        private String url;
        private Map<String, List<String>> headers;
        private Map<String, String> bodyFields;

        private StepDraft(
                String name,
                String method,
                String url,
                Map<String, List<String>> headers,
                Map<String, String> bodyFields
        ) {
            this.name = name;
            this.method = method;
            this.url = url;
            this.headers = new LinkedHashMap<>(headers);
            this.bodyFields = new LinkedHashMap<>(bodyFields);
        }

        private HttpTestStep toStep() {
            return new HttpTestStep(
                    name,
                    HttpTextMapper.toRequest(method, url, headers, HttpTextMapper.toJsonBody(bodyFields))
            );
        }
    }
}
