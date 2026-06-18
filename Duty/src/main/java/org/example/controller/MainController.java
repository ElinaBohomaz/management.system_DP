package org.example.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.example.model.Employee;
import org.example.model.Shift;
import org.example.service.DatabaseInitializer;
import org.example.service.EmployeeService;
import org.example.service.ScheduleService;
import org.example.util.ExcelExporter;

import java.io.File;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

public class MainController {

    @FXML private BorderPane rootPane;

    @FXML private VBox homeView;
    @FXML private VBox scheduleFullView;
    @FXML private VBox employeesFullView;
    @FXML private ScrollPane infoFullView;

    @FXML private Button homeMenuItem;
    @FXML private Button scheduleMenuItem;
    @FXML private Button employeesMenuItem;
    @FXML private Button infoMenuItem;

    @FXML private Text pageTitle;

    @FXML private Text totalShiftsCount;
    @FXML private Text totalEmployeesCount;
    @FXML private Text scheduleFilledPercent;

    @FXML private TableView<EmployeeScheduleRow> scheduleTableView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentComboBox;
    @FXML private ComboBox<String> dayFilterComboBox;
    @FXML private ComboBox<String> dayStatusComboBox;
    @FXML private Label currentMonthLabel;
    @FXML private Button prevMonthButton;
    @FXML private Button nextMonthButton;

    @FXML private TableView<Employee> employeesTableView;
    @FXML private TextField employeeSearchField;
    @FXML private ComboBox<String> employeeDepartmentFilter;
    @FXML private ComboBox<String> employeeStatusFilter;
    @FXML private Label employeesCountLabel;

    private final ScheduleService scheduleService = new ScheduleService();
    private final EmployeeService employeeService = new EmployeeService();
    private final org.example.dao.ShiftDAO shiftDAO = new org.example.dao.ShiftDAO();

    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);

    private YearMonth currentMonth;

    private final ObservableList<EmployeeScheduleRow> allScheduleRows = FXCollections.observableArrayList();
    private final FilteredList<EmployeeScheduleRow> filteredScheduleRows = new FilteredList<>(allScheduleRows);

    private final ObservableList<Employee> allEmployees = FXCollections.observableArrayList();
    private final FilteredList<Employee> filteredEmployees = new FilteredList<>(allEmployees);

    private final List<Shift> pendingShiftsToSave = new ArrayList<>();

    private boolean hasUnsavedChanges;
    private String copiedCellValue = "";

    private static final String AUTO_PATTERN_825_700 = "AUTO_PATTERN_825_700";
    private static final String AUTO_PATTERN_1_3 = "AUTO_PATTERN_1_3";
    private static final String SCHEDULE_825_700_LABEL = "8.25-7.00";
    private static final String SCHEDULE_1_3_LABEL = "1 через 3";
    private static final String SCHEDULE_NONE_LABEL = "Без автоматичного графіка";

    private boolean autoSaveEnabled = true;
    private boolean confirmBeforeReset = true;
    private String selectedTheme = "Стандартна тема";
    private double selectedFontSize = 14;

    private final DateTimeFormatter monthFormatter =
            DateTimeFormatter.ofPattern("LLLL", Locale.forLanguageTag("uk"));

    @FXML
    private void initialize() {
        DatabaseInitializer.initializeDatabase();
        loadAppSettings();
        setupComboBoxes();
        setupTable();
        setupEmployeesTable();
        loadCurrentMonth();
        loadEmployees();
        updateStatistics();
        applyAppSettings();
        openHomeView();
        setupAutoSave();
    }

    private void setupAutoSave() {
        Thread autoSaveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000);

                    if (hasUnsavedChanges && autoSaveEnabled) {
                        Platform.runLater(this::saveScheduleSilent);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        autoSaveThread.setDaemon(true);
        autoSaveThread.start();
    }

    private void setupComboBoxes() {
        departmentComboBox.setItems(
                FXCollections.observableArrayList("Всі підрозділи")
        );
        departmentComboBox.getSelectionModel().selectFirst();
        departmentComboBox.setOnAction(event -> filterTable());

        dayFilterComboBox.setItems(
                FXCollections.observableArrayList("Всі дні")
        );

        for (int day = 1; day <= 31; day++) {
            dayFilterComboBox.getItems().add(String.valueOf(day));
        }

        dayFilterComboBox.setValue("Всі дні");
        dayFilterComboBox.setOnAction(event -> {
            filterTable();
            scheduleTableView.refresh();
        });

        if (dayStatusComboBox != null) {
            dayStatusComboBox.setVisible(false);
            dayStatusComboBox.setManaged(false);
        }

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterTable()
        );
    }

    private void setupTable() {
        scheduleTableView.setItems(filteredScheduleRows);
        scheduleTableView.setEditable(true);

        setupTableEnterKey();
        setupCellFocusHandling();
        setupContextMenu();
        setupHotkeys();
    }

    private void setupEmployeesTable() {
        employeesTableView.setItems(filteredEmployees);
        employeesTableView.setEditable(false);

        TableColumn<Employee, String> nameColumn =
                new TableColumn<>("ПІБ");

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getFullName())
                )
        );
        nameColumn.setPrefWidth(220);
        nameColumn.setStyle("-fx-font-weight: bold;");

        TableColumn<Employee, String> positionColumn =
                new TableColumn<>("Посада");

        positionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getPosition())
                )
        );
        positionColumn.setPrefWidth(160);

        TableColumn<Employee, String> departmentColumn =
                new TableColumn<>("Підрозділ");

        departmentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getDepartment())
                )
        );
        departmentColumn.setPrefWidth(160);

        TableColumn<Employee, String> educationColumn =
                new TableColumn<>("Освіта");

        educationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getEducation())
                )
        );
        educationColumn.setPrefWidth(130);

        TableColumn<Employee, String> phoneColumn =
                new TableColumn<>("Телефон");

        phoneColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getPhone())
                )
        );
        phoneColumn.setPrefWidth(135);

        TableColumn<Employee, String> birthDateColumn =
                new TableColumn<>("Дата народження");

        birthDateColumn.setCellValueFactory(cellData -> {
            LocalDate birthDate = cellData.getValue().getBirthDate();

            return new SimpleStringProperty(
                    birthDate == null
                            ? ""
                            : birthDate.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    )
            );
        });
        birthDateColumn.setPrefWidth(130);

        TableColumn<Employee, String> hireDateColumn =
                new TableColumn<>("Дата прийому");

        hireDateColumn.setCellValueFactory(cellData -> {
            LocalDate hireDate = cellData.getValue().getHireDate();

            return new SimpleStringProperty(
                    hireDate == null
                            ? ""
                            : hireDate.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    )
            );
        });
        hireDateColumn.setPrefWidth(130);

        TableColumn<Employee, String> statusColumn =
                new TableColumn<>("Статус");

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getStatus())
                )
        );
        statusColumn.setPrefWidth(105);
        statusColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<Employee, String> profkomColumn =
                new TableColumn<>("Профком");

        profkomColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getProfkom())
                )
        );
        profkomColumn.setPrefWidth(100);

        TableColumn<Employee, String> childrenColumn =
                new TableColumn<>("Діти");

        childrenColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(cellData.getValue().getChildren())
                )
        );
        childrenColumn.setPrefWidth(90);

        employeesTableView.getColumns().clear();
        employeesTableView.getColumns().addAll(
                nameColumn,
                positionColumn,
                departmentColumn,
                educationColumn,
                phoneColumn,
                birthDateColumn,
                hireDateColumn,
                statusColumn,
                profkomColumn,
                childrenColumn
        );

        employeesTableView.setRowFactory(tableView -> {
            TableRow<Employee> row = new TableRow<>();

            MenuItem openCardItem =
                    new MenuItem("📄 Відкрити картку");

            openCardItem.setOnAction(event -> {
                if (!row.isEmpty()) {
                    showEmployeeCard(row.getItem());
                }
            });

            MenuItem editItem =
                    new MenuItem("✏️ Редагувати");

            editItem.setOnAction(event -> {
                if (!row.isEmpty()) {
                    employeesTableView
                            .getSelectionModel()
                            .select(row.getItem());

                    showEmployeeDialog(row.getItem());
                }
            });

            MenuItem deleteItem =
                    new MenuItem("🗑️ Видалити працівника");

            deleteItem.setOnAction(event -> {
                if (!row.isEmpty()) {
                    deleteEmployeeFromContext(row.getItem());
                }
            });

            ContextMenu contextMenu =
                    new ContextMenu(
                            openCardItem,
                            editItem,
                            new SeparatorMenuItem(),
                            deleteItem
                    );

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    employeesTableView
                            .getSelectionModel()
                            .select(row.getItem());

                    showEmployeeCard(row.getItem());
                }
            });

            return row;
        });

        employeeSearchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterEmployees()
        );

        employeeDepartmentFilter.valueProperty().addListener(
                (observable, oldValue, newValue) -> filterEmployees()
        );

        employeeStatusFilter.valueProperty().addListener(
                (observable, oldValue, newValue) -> filterEmployees()
        );
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyItem = new MenuItem("Копіювати");
        copyItem.setOnAction(event -> copySelectedCell());

        MenuItem pasteItem = new MenuItem("Вставити");
        pasteItem.setOnAction(event -> pasteToSelectedCell());

        MenuItem clearItem = new MenuItem("Очистити клітинку");
        clearItem.setOnAction(event -> clearSelectedCell());

        contextMenu.getItems().addAll(
                copyItem,
                pasteItem,
                clearItem
        );

        scheduleTableView.setContextMenu(contextMenu);
    }

    private void setupTableEnterKey() {
        scheduleTableView.setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }

            TablePosition<EmployeeScheduleRow, ?> focusedCell =
                    scheduleTableView
                            .getFocusModel()
                            .getFocusedCell();

            if (focusedCell != null
                    && focusedCell.getColumn() >= 3
                    && focusedCell.getColumn()
                    < scheduleTableView.getColumns().size() - 1) {

                scheduleTableView.edit(
                        focusedCell.getRow(),
                        focusedCell.getTableColumn()
                );
            }

            event.consume();
        });
    }

    private void setupHotkeys() {
        scheduleTableView.addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.isControlDown()
                            && event.getCode() == KeyCode.S) {

                        event.consume();
                        saveSchedule();

                    } else if (event.isControlDown()
                            && event.getCode() == KeyCode.C) {

                        event.consume();
                        copySelectedCell();

                    } else if (event.isControlDown()
                            && event.getCode() == KeyCode.V) {

                        event.consume();
                        pasteToSelectedCell();

                    } else if (event.getCode() == KeyCode.DELETE) {

                        event.consume();
                        clearSelectedCell();
                    }
                }
        );
    }

    private void setupCellFocusHandling() {
        scheduleTableView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (oldValue != null
                            && hasUnsavedChanges
                            && autoSaveEnabled) {

                        saveScheduleSilent();
                    }
                });
    }

    @FXML
    private void loadSchedule() {
        try {
            if (currentMonth == null) {
                return;
            }

            pendingShiftsToSave.clear();
            hasUnsavedChanges = false;

            String monthName =
                    currentMonth.format(monthFormatter);

            currentMonthLabel.setText(
                    monthName.substring(0, 1).toUpperCase()
                            + monthName.substring(1)
            );

            List<Employee> employees =
                    employeeService.getAllEmployees();

            Map<Integer, List<Shift>> shiftsMap =
                    shiftDAO.findShiftsForMonth(currentMonth);

            List<EmployeeScheduleRow> rows =
                    new ArrayList<>();

            for (Employee employee : employees) {
                if (!employee.isCurrentlyWorking()) {
                    continue;
                }

                List<Shift> employeeShifts =
                        shiftsMap.getOrDefault(
                                employee.getId(),
                                new ArrayList<>()
                        );

                rows.add(
                        new EmployeeScheduleRow(
                                employee,
                                employeeShifts,
                                currentMonth
                        )
                );
            }

            rows.sort((firstRow, secondRow) -> {
                String firstDepartment =
                        safeText(
                                firstRow
                                        .getEmployee()
                                        .getDepartment()
                        );

                String secondDepartment =
                        safeText(
                                secondRow
                                        .getEmployee()
                                        .getDepartment()
                        );

                int departmentComparison =
                        firstDepartment.compareToIgnoreCase(
                                secondDepartment
                        );

                if (departmentComparison != 0) {
                    return departmentComparison;
                }

                return safeText(
                        firstRow.getEmployee().getFullName()
                ).compareToIgnoreCase(
                        safeText(
                                secondRow
                                        .getEmployee()
                                        .getFullName()
                        )
                );
            });

            allScheduleRows.setAll(rows);
            updateDepartmentComboBox();
            addDayColumns();
            addTotalHoursColumn();
            filterTable();
            updateScheduleStatistics();

        } catch (SQLException e) {
            showError(
                    "Помилка завантаження",
                    e.getMessage()
            );
        }
    }

    private void addDayColumns() {
        scheduleTableView.getColumns().clear();

        if (currentMonth == null) {
            return;
        }

        TableColumn<EmployeeScheduleRow, String> nameColumn =
                new TableColumn<>("ПІБ");

        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(
                                cellData
                                        .getValue()
                                        .getEmployee()
                                        .getFullName()
                        )
                )
        );
        nameColumn.setStyle(
                "-fx-font-weight: bold; "
                        + "-fx-alignment: CENTER_LEFT;"
        );
        nameColumn.setEditable(false);

        scheduleTableView.getColumns().add(nameColumn);

        TableColumn<EmployeeScheduleRow, String> departmentColumn =
                new TableColumn<>("Підрозділ");

        departmentColumn.setPrefWidth(160);
        departmentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(
                                cellData
                                        .getValue()
                                        .getEmployee()
                                        .getDepartment()
                        )
                )
        );
        departmentColumn.setStyle(
                "-fx-alignment: CENTER_LEFT;"
        );
        departmentColumn.setEditable(false);

        scheduleTableView.getColumns().add(departmentColumn);

        TableColumn<EmployeeScheduleRow, String> positionColumn =
                new TableColumn<>("Посада");

        positionColumn.setPrefWidth(160);
        positionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        safeText(
                                cellData
                                        .getValue()
                                        .getEmployee()
                                        .getPosition()
                        )
                )
        );
        positionColumn.setStyle(
                "-fx-alignment: CENTER_LEFT;"
        );
        positionColumn.setEditable(false);

        scheduleTableView.getColumns().add(positionColumn);

        for (int day = 1;
             day <= currentMonth.lengthOfMonth();
             day++) {

            int dayNumber = day;

            LocalDate date =
                    currentMonth.atDay(dayNumber);

            boolean weekend =
                    date.getDayOfWeek() == DayOfWeek.SATURDAY
                            || date.getDayOfWeek()
                            == DayOfWeek.SUNDAY;

            TableColumn<EmployeeScheduleRow, String> dayColumn =
                    new TableColumn<>(
                            String.valueOf(dayNumber)
                    );

            dayColumn.setPrefWidth(65);
            dayColumn.setMinWidth(65);
            dayColumn.setEditable(true);

            if (weekend) {
                dayColumn.setStyle(
                        "-fx-alignment: CENTER; "
                                + "-fx-font-weight: bold; "
                                + "-fx-text-fill: #b71c1c; "
                                + "-fx-background-color: #cfd8dc;"
                );
            } else {
                dayColumn.setStyle(
                        "-fx-alignment: CENTER; "
                                + "-fx-font-weight: bold;"
                );
            }

            dayColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData
                                    .getValue()
                                    .getShiftCodeForDay(dayNumber)
                    )
            );

            dayColumn.setCellFactory(column ->
                    createShiftCell(dayNumber)
            );

            scheduleTableView
                    .getColumns()
                    .add(dayColumn);
        }
    }

    private TableCell<EmployeeScheduleRow, String> createShiftCell(
            int dayNumber
    ) {
        return new TableCell<>() {

            private final TextField textField =
                    new TextField();

            {
                textField.setAlignment(Pos.CENTER);

                textField.setOnAction(event ->
                        commitCurrentValue()
                );

                textField
                        .focusedProperty()
                        .addListener(
                                (observable, oldValue, newValue) -> {
                                    if (!newValue && isEditing()) {
                                        commitCurrentValue();
                                    }
                                }
                        );
            }

            @Override
            public void startEdit() {
                if (isEmpty()) {
                    return;
                }

                super.startEdit();

                textField.setText(
                        getItem() == null
                                ? ""
                                : getItem()
                );

                setText(null);
                setGraphic(textField);

                textField.requestFocus();
                textField.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();

                setGraphic(null);
                setText(getItem());

                applyShiftCellStyle(
                        this,
                        getItem(),
                        dayNumber
                );
            }

            @Override
            public void commitEdit(String newValue) {
                String code =
                        normalizeShiftCode(newValue);

                if (!isValidShiftCode(code)) {
                    cancelEdit();

                    showError(
                            "Помилка",
                            "Допустимі коди: 1, 7.00, "
                                    + "8.25, X, 0, В, ТН, Л, К"
                    );

                    return;
                }

                if (code.isEmpty()) {
                    code = "X";
                }

                super.commitEdit(code);

                EmployeeScheduleRow row =
                        getTableRow().getItem();

                if (row == null || currentMonth == null) {
                    return;
                }

                row.setShiftForDay(
                        dayNumber,
                        code
                );

                pendingShiftsToSave.add(
                        new Shift(
                                row.getEmployee().getId(),
                                currentMonth.atDay(dayNumber),
                                code
                        )
                );

                hasUnsavedChanges = true;

                saveScheduleSilent();
                scheduleTableView.refresh();
                updateScheduleStatistics();
            }

            @Override
            protected void updateItem(
                    String item,
                    boolean empty
            ) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                if (isEditing()) {
                    textField.setText(
                            item == null ? "" : item
                    );

                    setText(null);
                    setGraphic(textField);
                } else {
                    setGraphic(null);
                    setText(item);

                    applyShiftCellStyle(
                            this,
                            item,
                            dayNumber
                    );
                }
            }

            private void commitCurrentValue() {
                commitEdit(textField.getText());
            }
        };
    }

    private void applyShiftCellStyle(
            TableCell<EmployeeScheduleRow, String> cell,
            String code,
            int day
    ) {
        String selectedDay =
                dayFilterComboBox.getValue();

        boolean selectedColumn =
                selectedDay != null
                        && selectedDay.equals(
                        String.valueOf(day)
                );

        boolean weekendColumn =
                currentMonth != null
                        && currentMonth
                        .atDay(day)
                        .getDayOfWeek()
                        .getValue() >= 6;

        String style =
                "-fx-alignment: CENTER; "
                        + "-fx-font-weight: bold; "
                        + "-fx-border-color: #d0d7de; "
                        + "-fx-border-width: 0.5;";

        if (selectedColumn) {
            style +=
                    "-fx-background-color: #fff59d; "
                            + "-fx-border-color: #f9a825; "
                            + "-fx-border-width: 2;";

        } else if (weekendColumn) {
            style +=
                    "-fx-background-color: #cfd8dc; "
                            + "-fx-text-fill: #263238;";

        } else if ("X".equals(code)) {
            style +=
                    "-fx-background-color: #e3f2fd; "
                            + "-fx-text-fill: #1565c0;";

        } else if ("1".equals(code)) {
            style +=
                    "-fx-background-color: #e8f5e9; "
                            + "-fx-text-fill: #2e7d32;";

        } else if ("7.00".equals(code)
                || "8.25".equals(code)) {

            style +=
                    "-fx-background-color: #fff3e0; "
                            + "-fx-text-fill: #ef6c00;";

        } else if ("В".equals(code)
                || "0".equals(code)
                || "ТН".equals(code)
                || "Л".equals(code)
                || "К".equals(code)) {

            style +=
                    "-fx-background-color: #ffebee; "
                            + "-fx-text-fill: #c62828;";

        } else {
            style +=
                    "-fx-background-color: #ffffff; "
                            + "-fx-text-fill: #000000;";
        }

        cell.setStyle(style);
    }

    private void addTotalHoursColumn() {
        TableColumn<EmployeeScheduleRow, String> hoursColumn =
                new TableColumn<>("Години");

        hoursColumn.setPrefWidth(95);
        hoursColumn.setStyle(
                "-fx-alignment: CENTER; "
                        + "-fx-font-weight: bold;"
        );

        hoursColumn.setCellValueFactory(cellData -> {
            double hours =
                    calculateTotalHours(
                            cellData.getValue()
                    );

            if (hours == Math.floor(hours)) {
                return new SimpleStringProperty(
                        String.valueOf((int) hours)
                );
            }

            return new SimpleStringProperty(
                    String.format(
                            Locale.US,
                            "%.2f",
                            hours
                    )
            );
        });

        hoursColumn.setCellFactory(column ->
                new TableCell<>() {

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }

                        setText(item);

                        double hours;

                        try {
                            hours = Double.parseDouble(
                                    item.replace(",", ".")
                            );
                        } catch (NumberFormatException e) {
                            hours = 0;
                        }

                        if (hours > 176) {
                            setStyle(
                                    "-fx-alignment: CENTER; "
                                            + "-fx-font-weight: bold; "
                                            + "-fx-background-color: #ffcdd2; "
                                            + "-fx-text-fill: #b71c1c; "
                                            + "-fx-border-color: #ef9a9a; "
                                            + "-fx-border-width: 1;"
                            );

                            setTooltip(
                                    new Tooltip(
                                            "Перевищено місячну "
                                                    + "норму 176 годин"
                                    )
                            );
                        } else {
                            setStyle(
                                    "-fx-alignment: CENTER; "
                                            + "-fx-font-weight: bold; "
                                            + "-fx-background-color: #e8f5e9; "
                                            + "-fx-text-fill: #1b5e20;"
                            );

                            setTooltip(
                                    new Tooltip(
                                            "Місячна норма: 176 годин"
                                    )
                            );
                        }
                    }
                }
        );

        hoursColumn.setEditable(false);

        scheduleTableView
                .getColumns()
                .add(hoursColumn);
    }

    private double calculateTotalHours(
            EmployeeScheduleRow row
    ) {
        if (currentMonth == null || row == null) {
            return 0;
        }

        double total = 0;

        for (int day = 1;
             day <= currentMonth.lengthOfMonth();
             day++) {

            String code =
                    normalizeShiftCode(
                            row.getShiftCodeForDay(day)
                    );

            switch (code) {
                case "1" -> total += 24;
                case "7.00" -> total += 7;
                case "8.25" -> total += 8.25;
                default -> {
                }
            }
        }

        return total;
    }

    @FXML
    private void createSchedule() {
        try {
            if (currentMonth == null) {
                return;
            }

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Створення графіка");
            dialog.setHeaderText("Оберіть тип графіка для нових працівників");

            ButtonType createButton = new ButtonType("Створити", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

            VBox box = new VBox(12);
            box.setPadding(new Insets(20));
            box.setPrefWidth(420);

            Text title = new Text("Автоматичний графік буде створено тільки для працівників, яких ви додали вручну та яким обрали цей тип графіка.");
            title.setWrappingWidth(390);
            title.setStyle("-fx-fill: #455a64; -fx-font-size: 13px;");

            ComboBox<String> patternComboBox = new ComboBox<>(FXCollections.observableArrayList(
                    SCHEDULE_825_700_LABEL,
                    SCHEDULE_1_3_LABEL
            ));
            patternComboBox.setValue(SCHEDULE_825_700_LABEL);
            patternComboBox.setPrefWidth(260);

            box.getChildren().addAll(new Label("Тип графіка:"), patternComboBox, title);
            dialog.getDialogPane().setContent(box);

            dialog.setResultConverter(buttonType -> {
                if (buttonType == createButton) {
                    return patternComboBox.getValue();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();

            if (result.isEmpty()) {
                return;
            }

            String selectedPattern = getPatternCodeFromChoice(result.get());

            List<Employee> employees = employeeService
                    .getAllEmployees()
                    .stream()
                    .filter(Employee::isCurrentlyWorking)
                    .filter(employee -> selectedPattern.equals(getEmployeeAutoPattern(employee)))
                    .sorted(
                            Comparator
                                    .comparing(
                                            Employee::getDepartment,
                                            Comparator.nullsFirst(String::compareToIgnoreCase)
                                    )
                                    .thenComparing(
                                            Employee::getFullName,
                                            Comparator.nullsFirst(String::compareToIgnoreCase)
                                    )
                    )
                    .toList();

            if (employees.isEmpty()) {
                showError(
                        "Немає працівників",
                        "Для цього типу графіка немає нових працівників. Працівники з початкової бази не змінюються автоматично."
                );
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Створення графіка");
            confirmation.setHeaderText("Створити графік " + result.get() + "?");
            confirmation.setContentText("Буде заповнено графік тільки для нових працівників з обраним типом: " + employees.size());

            Optional<ButtonType> confirmResult = confirmation.showAndWait();

            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
                return;
            }

            List<Shift> shifts = new ArrayList<>();

            for (Employee employee : employees) {
                for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
                    LocalDate date = currentMonth.atDay(day);
                    String code = generateCodeForPattern(selectedPattern, date, day);
                    shifts.add(new Shift(employee.getId(), date, code));
                }
            }

            shiftDAO.saveBatch(shifts);

            hasUnsavedChanges = false;
            pendingShiftsToSave.clear();

            loadSchedule();

            Platform.runLater(() -> {
                scheduleTableView.scrollToColumnIndex(3);
                scheduleTableView.refresh();
            });

            showStatus("Автоматичний графік створено для нових працівників: " + result.get());

        } catch (SQLException e) {
            showError(
                    "Помилка створення графіка",
                    e.getMessage()
            );
        }
    }


    private String getPatternCodeFromChoice(String choice) {
        if (SCHEDULE_825_700_LABEL.equals(choice)) {
            return AUTO_PATTERN_825_700;
        }
        if (SCHEDULE_1_3_LABEL.equals(choice)) {
            return AUTO_PATTERN_1_3;
        }
        return "";
    }

    private String getChoiceFromPattern(String pattern) {
        if (AUTO_PATTERN_825_700.equals(pattern)) {
            return SCHEDULE_825_700_LABEL;
        }
        if (AUTO_PATTERN_1_3.equals(pattern)) {
            return SCHEDULE_1_3_LABEL;
        }
        return SCHEDULE_NONE_LABEL;
    }

    private String getEmployeeAutoPattern(Employee employee) {
        if (employee == null) {
            return "";
        }

        String data = safeText(employee.getData());

        if (AUTO_PATTERN_825_700.equals(data) || AUTO_PATTERN_1_3.equals(data)) {
            return data;
        }

        return "";
    }

    private String generateCodeForPattern(String pattern, LocalDate date, int day) {
        if (AUTO_PATTERN_825_700.equals(pattern)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            if (dayOfWeek == DayOfWeek.MONDAY
                    || dayOfWeek == DayOfWeek.TUESDAY
                    || dayOfWeek == DayOfWeek.WEDNESDAY
                    || dayOfWeek == DayOfWeek.THURSDAY) {

                return "8.25";
            }

            if (dayOfWeek == DayOfWeek.FRIDAY) {
                return "7.00";
            }

            return "X";
        }

        if (AUTO_PATTERN_1_3.equals(pattern)) {
            int cycle = (day - 1) % 4;
            return cycle == 0 ? "1" : "X";
        }

        return "X";
    }

    private void createScheduleForSingleEmployee(Employee employee, String pattern) {
        try {
            if (employee == null || employee.getId() == null || currentMonth == null || pattern == null || pattern.isBlank()) {
                return;
            }

            List<Shift> shifts = new ArrayList<>();

            for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
                LocalDate date = currentMonth.atDay(day);
                String code = generateCodeForPattern(pattern, date, day);
                shifts.add(new Shift(employee.getId(), date, code));
            }

            shiftDAO.saveBatch(shifts);
            pendingShiftsToSave.clear();
            hasUnsavedChanges = false;
        } catch (SQLException e) {
            showError("Помилка створення графіка", e.getMessage());
        }
    }

    @FXML
    private void resetFilters() {
        try {
            if (currentMonth == null) {
                return;
            }

            if (confirmBeforeReset) {
                Alert confirmation =
                        new Alert(
                                Alert.AlertType.CONFIRMATION
                        );

                confirmation.setTitle(
                        "Скидання табеля"
                );
                confirmation.setHeaderText(
                        "Очистити табель за поточний місяць?"
                );
                confirmation.setContentText(
                        "Усі значення за цей місяць "
                                + "будуть замінені на X."
                );

                Optional<ButtonType> result =
                        confirmation.showAndWait();

                if (result.isEmpty()
                        || result.get() != ButtonType.OK) {

                    return;
                }
            }

            List<Shift> shifts =
                    new ArrayList<>();

            for (EmployeeScheduleRow row
                    : allScheduleRows) {

                for (int day = 1;
                     day <= currentMonth.lengthOfMonth();
                     day++) {

                    row.setShiftForDay(day, "X");

                    shifts.add(
                            new Shift(
                                    row.getEmployee().getId(),
                                    currentMonth.atDay(day),
                                    "X"
                            )
                    );
                }
            }

            shiftDAO.saveBatch(shifts);

            pendingShiftsToSave.clear();
            hasUnsavedChanges = false;

            searchField.clear();
            departmentComboBox
                    .getSelectionModel()
                    .selectFirst();

            dayFilterComboBox.setValue("Всі дні");

            loadSchedule();

            showStatus("Табель очищено");

        } catch (SQLException e) {
            showError(
                    "Помилка скидання",
                    e.getMessage()
            );
        }
    }

    @FXML
    private void saveSchedule() {
        saveScheduleSilent();
        showStatus("Графік збережено");
    }

    private void saveScheduleSilent() {
        try {
            if (currentMonth == null) {
                return;
            }

            List<Shift> shiftsToSave =
                    new ArrayList<>();

            for (EmployeeScheduleRow row
                    : allScheduleRows) {

                for (int day = 1;
                     day <= currentMonth.lengthOfMonth();
                     day++) {

                    String code =
                            normalizeShiftCode(
                                    row.getShiftCodeForDay(day)
                            );

                    if (code.isEmpty()) {
                        code = "X";
                    }

                    shiftsToSave.add(
                            new Shift(
                                    row.getEmployee().getId(),
                                    currentMonth.atDay(day),
                                    code
                            )
                    );
                }
            }

            if (!pendingShiftsToSave.isEmpty()) {
                shiftsToSave.addAll(
                        pendingShiftsToSave
                );

                pendingShiftsToSave.clear();
            }

            if (!shiftsToSave.isEmpty()) {
                shiftDAO.saveBatch(shiftsToSave);
            }

            hasUnsavedChanges = false;

        } catch (SQLException e) {
            showError(
                    "Помилка збереження",
                    e.getMessage()
            );
        }
    }

    private void copySelectedCell() {
        try {
            ObservableList<TablePosition> selectedCells =
                    scheduleTableView
                            .getSelectionModel()
                            .getSelectedCells();

            if (selectedCells.isEmpty()) {
                return;
            }

            TablePosition position =
                    selectedCells.get(0);

            if (position.getColumn() < 3
                    || position.getColumn()
                    >= scheduleTableView
                    .getColumns()
                    .size() - 1) {

                return;
            }

            EmployeeScheduleRow row =
                    scheduleTableView
                            .getItems()
                            .get(position.getRow());

            int day =
                    position.getColumn() - 2;

            copiedCellValue =
                    row.getShiftCodeForDay(day);

            showStatus(
                    "Скопійовано: "
                            + copiedCellValue
            );

        } catch (Exception e) {
            showError(
                    "Помилка копіювання",
                    e.getMessage()
            );
        }
    }

    private void pasteToSelectedCell() {
        if (copiedCellValue == null
                || copiedCellValue.isEmpty()) {

            showStatus(
                    "Немає скопійованого значення"
            );
            return;
        }

        try {
            ObservableList<TablePosition> selectedCells =
                    scheduleTableView
                            .getSelectionModel()
                            .getSelectedCells();

            if (selectedCells.isEmpty()) {
                return;
            }

            TablePosition position =
                    selectedCells.get(0);

            if (position.getColumn() < 3
                    || position.getColumn()
                    >= scheduleTableView
                    .getColumns()
                    .size() - 1) {

                return;
            }

            EmployeeScheduleRow row =
                    scheduleTableView
                            .getItems()
                            .get(position.getRow());

            int day =
                    position.getColumn() - 2;

            String code =
                    normalizeShiftCode(
                            copiedCellValue
                    );

            if (!isValidShiftCode(code)) {
                showError(
                        "Помилка",
                        "Недійсний код"
                );
                return;
            }

            row.setShiftForDay(day, code);

            pendingShiftsToSave.add(
                    new Shift(
                            row.getEmployee().getId(),
                            currentMonth.atDay(day),
                            code
                    )
            );

            hasUnsavedChanges = true;

            saveScheduleSilent();
            scheduleTableView.refresh();
            updateScheduleStatistics();

            showStatus("Вставлено: " + code);

        } catch (Exception e) {
            showError(
                    "Помилка вставки",
                    e.getMessage()
            );
        }
    }

    private void clearSelectedCell() {
        try {
            ObservableList<TablePosition> selectedCells =
                    scheduleTableView
                            .getSelectionModel()
                            .getSelectedCells();

            if (selectedCells.isEmpty()) {
                return;
            }

            TablePosition position =
                    selectedCells.get(0);

            if (position.getColumn() < 3
                    || position.getColumn()
                    >= scheduleTableView
                    .getColumns()
                    .size() - 1) {

                return;
            }

            EmployeeScheduleRow row =
                    scheduleTableView
                            .getItems()
                            .get(position.getRow());

            int day =
                    position.getColumn() - 2;

            row.setShiftForDay(day, "X");

            pendingShiftsToSave.add(
                    new Shift(
                            row.getEmployee().getId(),
                            currentMonth.atDay(day),
                            "X"
                    )
            );

            hasUnsavedChanges = true;

            saveScheduleSilent();
            scheduleTableView.refresh();
            updateScheduleStatistics();

            showStatus("Клітинку очищено");

        } catch (Exception e) {
            showError(
                    "Помилка очищення",
                    e.getMessage()
            );
        }
    }

    private String normalizeShiftCode(
            String code
    ) {
        if (code == null) {
            return "";
        }

        String value =
                code.trim()
                        .toUpperCase()
                        .replace(",", ".");

        if ("7".equals(value)
                || "7.0".equals(value)) {

            return "7.00";
        }

        return value;
    }

    private boolean isValidShiftCode(
            String code
    ) {
        if (code == null
                || code.trim().isEmpty()) {

            return true;
        }

        String value =
                normalizeShiftCode(code);

        return value.equals("1")
                || value.equals("7.00")
                || value.equals("8.25")
                || value.equals("X")
                || value.equals("0")
                || value.equals("В")
                || value.equals("ТН")
                || value.equals("Л")
                || value.equals("К");
    }

    private void updateDepartmentComboBox() {
        try {
            String currentValue =
                    departmentComboBox.getValue();

            List<String> departments =
                    employeeService.getAllDepartments();

            ObservableList<String> values =
                    FXCollections.observableArrayList(
                            departments
                    );

            values.add(0, "Всі підрозділи");

            departmentComboBox.setItems(values);

            if (currentValue != null
                    && values.contains(currentValue)) {

                departmentComboBox.setValue(
                        currentValue
                );
            } else {
                departmentComboBox
                        .getSelectionModel()
                        .selectFirst();
            }

        } catch (SQLException e) {
            departmentComboBox.setItems(
                    FXCollections.observableArrayList(
                            "Всі підрозділи"
                    )
            );

            departmentComboBox
                    .getSelectionModel()
                    .selectFirst();
        }
    }

    private void filterTable() {
        String searchText =
                searchField.getText() == null
                        ? ""
                        : searchField
                        .getText()
                        .trim()
                        .toLowerCase();

        String selectedDepartment =
                departmentComboBox.getValue();

        filteredScheduleRows.setPredicate(row -> {
            Employee employee =
                    row.getEmployee();

            String fullName =
                    safeText(
                            employee.getFullName()
                    ).toLowerCase();

            String department =
                    safeText(
                            employee.getDepartment()
                    ).toLowerCase();

            String position =
                    safeText(
                            employee.getPosition()
                    ).toLowerCase();

            if (!searchText.isEmpty()
                    && !fullName.contains(searchText)
                    && !department.contains(searchText)
                    && !position.contains(searchText)) {

                return false;
            }

            if (selectedDepartment != null
                    && !"Всі підрозділи".equals(
                    selectedDepartment
            )) {

                return selectedDepartment.equals(
                        employee.getDepartment()
                );
            }

            return true;
        });

        scheduleTableView.refresh();
    }

    @FXML
    private void previousMonth() {
        saveScheduleSilent();

        if (currentMonth == null) {
            return;
        }

        YearMonth newMonth =
                currentMonth.minusMonths(1);

        if (newMonth.getYear() < 2024
                || newMonth.getYear() > 2027) {

            showStatus(
                    "Доступні місяці з 2024 по 2027 рік"
            );
            return;
        }

        currentMonth = newMonth;
        loadSchedule();
    }

    @FXML
    private void nextMonth() {
        saveScheduleSilent();

        if (currentMonth == null) {
            return;
        }

        YearMonth newMonth =
                currentMonth.plusMonths(1);

        if (newMonth.getYear() < 2024
                || newMonth.getYear() > 2027) {

            showStatus(
                    "Доступні місяці з 2024 по 2027 рік"
            );
            return;
        }

        currentMonth = newMonth;
        loadSchedule();
    }

    @FXML
    private void goToToday() {
        saveScheduleSilent();
        currentMonth = YearMonth.now();
        loadSchedule();

        showStatus(
                "Відкрито поточний місяць"
        );
    }

    private void loadCurrentMonth() {
        currentMonth = YearMonth.now();
        loadSchedule();
    }

    private void updateStatistics() {
        try {
            long activeEmployees =
                    employeeService
                            .getAllEmployees()
                            .stream()
                            .filter(
                                    Employee::isCurrentlyWorking
                            )
                            .count();

            totalEmployeesCount.setText(
                    String.valueOf(activeEmployees)
            );

        } catch (SQLException e) {
            totalEmployeesCount.setText("0");
        }

        updateScheduleStatistics();
    }

    private void updateScheduleStatistics() {
        int workShifts = 0;
        int filledCells = 0;
        int allCells = 0;

        if (currentMonth != null) {
            for (EmployeeScheduleRow row
                    : allScheduleRows) {

                for (int day = 1;
                     day <= currentMonth.lengthOfMonth();
                     day++) {

                    String code =
                            normalizeShiftCode(
                                    row.getShiftCodeForDay(day)
                            );

                    allCells++;

                    if (!code.isEmpty()) {
                        filledCells++;
                    }

                    if ("1".equals(code)
                            || "7.00".equals(code)
                            || "8.25".equals(code)) {

                        workShifts++;
                    }
                }
            }
        }

        totalShiftsCount.setText(
                String.valueOf(workShifts)
        );

        int percent =
                allCells == 0
                        ? 0
                        : (int) Math.round(
                        filledCells * 100.0
                                / allCells
                );

        scheduleFilledPercent.setText(
                percent + "%"
        );
    }

    private void filterEmployees() {
        String searchText =
                employeeSearchField.getText() == null
                        ? ""
                        : employeeSearchField
                        .getText()
                        .trim()
                        .toLowerCase();

        String selectedDepartment =
                employeeDepartmentFilter.getValue();

        String selectedStatus =
                employeeStatusFilter.getValue();

        filteredEmployees.setPredicate(employee -> {
            String fullName =
                    safeText(
                            employee.getFullName()
                    ).toLowerCase();

            String department =
                    safeText(
                            employee.getDepartment()
                    ).toLowerCase();

            String position =
                    safeText(
                            employee.getPosition()
                    ).toLowerCase();

            if (!searchText.isEmpty()
                    && !fullName.contains(searchText)
                    && !department.contains(searchText)
                    && !position.contains(searchText)) {

                return false;
            }

            if (selectedDepartment != null
                    && !"Всі підрозділи".equals(
                    selectedDepartment
            )
                    && !selectedDepartment.equals(
                    employee.getDepartment()
            )) {

                return false;
            }

            return selectedStatus == null
                    || "Всі".equals(selectedStatus)
                    || selectedStatus.equals(
                    employee.getStatus()
            );
        });

        employeesCountLabel.setText(
                "Всього: "
                        + filteredEmployees.size()
        );
    }

    private void loadEmployees() {
        try {
            List<Employee> employees =
                    employeeService.getAllEmployees();

            allEmployees.setAll(employees);

            Set<String> departments =
                    new TreeSet<>();

            for (Employee employee : employees) {
                String department =
                        safeText(
                                employee.getDepartment()
                        );

                if (!department.isEmpty()) {
                    departments.add(department);
                }
            }

            String selectedDepartment =
                    employeeDepartmentFilter.getValue();

            employeeDepartmentFilter.setItems(
                    FXCollections.observableArrayList(
                            "Всі підрозділи"
                    )
            );

            employeeDepartmentFilter
                    .getItems()
                    .addAll(departments);

            if (selectedDepartment != null
                    && employeeDepartmentFilter
                    .getItems()
                    .contains(selectedDepartment)) {

                employeeDepartmentFilter.setValue(
                        selectedDepartment
                );
            } else {
                employeeDepartmentFilter
                        .getSelectionModel()
                        .selectFirst();
            }

            String selectedStatus =
                    employeeStatusFilter.getValue();

            employeeStatusFilter.setItems(
                    FXCollections.observableArrayList(
                            "Всі",
                            "працює",
                            "звільнений"
                    )
            );

            if (selectedStatus != null
                    && employeeStatusFilter
                    .getItems()
                    .contains(selectedStatus)) {

                employeeStatusFilter.setValue(
                        selectedStatus
                );
            } else {
                employeeStatusFilter
                        .getSelectionModel()
                        .selectFirst();
            }

            filterEmployees();

        } catch (SQLException e) {
            showError(
                    "Помилка завантаження",
                    "Не вдалося завантажити працівників: "
                            + e.getMessage()
            );
        }
    }

    @FXML
    private void addEmployee() {
        showEmployeeDialog(null);
    }

    @FXML
    private void editEmployee() {
        Employee selectedEmployee =
                employeesTableView
                        .getSelectionModel()
                        .getSelectedItem();

        if (selectedEmployee == null) {
            showError(
                    "Помилка",
                    "Виберіть працівника"
            );
            return;
        }

        showEmployeeDialog(selectedEmployee);
    }

    private void deleteEmployeeFromContext(
            Employee employee
    ) {
        if (employee == null) {
            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Видалення працівника"
        );
        confirmation.setHeaderText(
                "Видалити працівника?"
        );
        confirmation.setContentText(
                employee.getFullName()
                        + " буде видалений зі списку "
                        + "працівників і табеля."
        );

        confirmation
                .showAndWait()
                .ifPresent(response -> {
                    if (response != ButtonType.OK) {
                        return;
                    }

                    try {
                        employeeService.deleteEmployee(
                                employee.getId()
                        );

                        loadEmployees();
                        loadSchedule();
                        updateStatistics();

                        showStatus(
                                "Працівника видалено"
                        );

                    } catch (SQLException e) {
                        showError(
                                "Помилка",
                                "Не вдалося видалити працівника: "
                                        + e.getMessage()
                        );
                    }
                });
    }

    private void showEmployeeDialog(
            Employee employee
    ) {
        Dialog<Employee> dialog =
                new Dialog<>();

        dialog.setTitle(
                employee == null
                        ? "Додати працівника"
                        : "Редагувати працівника"
        );
        dialog.setHeaderText(null);

        ButtonType saveButton =
                new ButtonType(
                        "Зберегти",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        saveButton,
                        ButtonType.CANCEL
                );

        GridPane grid =
                new GridPane();

        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField fullNameField =
                new TextField();

        TextField positionField =
                new TextField();

        TextField departmentField =
                new TextField();

        TextField educationField =
                new TextField();

        TextField phoneField =
                new TextField();

        DatePicker birthDatePicker =
                new DatePicker();

        DatePicker hireDatePicker =
                new DatePicker();

        ComboBox<String> statusComboBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                "працює",
                                "звільнений"
                        )
                );

        TextField profkomField =
                new TextField();

        TextField childrenField =
                new TextField();

        ComboBox<String> scheduleTypeComboBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                SCHEDULE_NONE_LABEL,
                                SCHEDULE_825_700_LABEL,
                                SCHEDULE_1_3_LABEL
                        )
                );

        fullNameField.setPrefWidth(270);
        positionField.setPrefWidth(270);
        departmentField.setPrefWidth(270);
        educationField.setPrefWidth(270);
        phoneField.setPrefWidth(270);
        birthDatePicker.setPrefWidth(270);
        hireDatePicker.setPrefWidth(270);
        statusComboBox.setPrefWidth(270);
        profkomField.setPrefWidth(270);
        childrenField.setPrefWidth(270);
        scheduleTypeComboBox.setPrefWidth(270);

        fullNameField.setPromptText(
                "Прізвище Ім’я По батькові"
        );

        phoneField.setPromptText(
                "+380XXXXXXXXX або 0XXXXXXXXX"
        );

        phoneField.setTextFormatter(
                new TextFormatter<String>(change -> {
                    String newText =
                            change.getControlNewText();

                    if (newText.matches("\\+?\\d*")
                            && newText.length() <= 13) {

                        return change;
                    }

                    return null;
                })
        );

        childrenField.setTextFormatter(
                new TextFormatter<String>(change -> {
                    String newText =
                            change.getControlNewText();

                    if (newText.matches("\\d*")
                            && newText.length() <= 2) {

                        return change;
                    }

                    return null;
                })
        );

        birthDatePicker.setEditable(false);
        hireDatePicker.setEditable(false);

        birthDatePicker.setDayCellFactory(
                picker -> new DateCell() {
                    @Override
                    public void updateItem(
                            LocalDate date,
                            boolean empty
                    ) {
                        super.updateItem(date, empty);

                        setDisable(
                                empty
                                        || date.isAfter(
                                        LocalDate.now()
                                                .minusYears(16)
                                )
                                        || date.isBefore(
                                        LocalDate.now()
                                                .minusYears(80)
                                )
                        );
                    }
                }
        );

        hireDatePicker.setDayCellFactory(
                picker -> new DateCell() {
                    @Override
                    public void updateItem(
                            LocalDate date,
                            boolean empty
                    ) {
                        super.updateItem(date, empty);

                        setDisable(
                                empty
                                        || date.isAfter(
                                        LocalDate.now()
                                )
                                        || date.isBefore(
                                        LocalDate.of(
                                                1980,
                                                1,
                                                1
                                        )
                                )
                        );
                    }
                }
        );

        if (employee != null) {
            fullNameField.setText(
                    safeText(
                            employee.getFullName()
                    )
            );

            positionField.setText(
                    safeText(
                            employee.getPosition()
                    )
            );

            departmentField.setText(
                    safeText(
                            employee.getDepartment()
                    )
            );

            educationField.setText(
                    safeText(
                            employee.getEducation()
                    )
            );

            phoneField.setText(
                    safeText(
                            employee.getPhone()
                    )
            );

            birthDatePicker.setValue(
                    employee.getBirthDate()
            );

            hireDatePicker.setValue(
                    employee.getHireDate()
            );

            statusComboBox.setValue(
                    employee.getStatus()
            );

            profkomField.setText(
                    safeText(
                            employee.getProfkom()
                    )
            );

            childrenField.setText(
                    safeText(
                            employee.getChildren()
                    )
            );

            scheduleTypeComboBox.setValue(
                    getChoiceFromPattern(
                            getEmployeeAutoPattern(employee)
                    )
            );
        } else {
            statusComboBox.setValue("працює");
            hireDatePicker.setValue(LocalDate.now());
            scheduleTypeComboBox.setValue(SCHEDULE_NONE_LABEL);
        }

        grid.add(
                new Label("ПІБ:*"),
                0,
                0
        );
        grid.add(
                fullNameField,
                1,
                0
        );

        grid.add(
                new Label("Посада:*"),
                0,
                1
        );
        grid.add(
                positionField,
                1,
                1
        );

        grid.add(
                new Label("Підрозділ:*"),
                0,
                2
        );
        grid.add(
                departmentField,
                1,
                2
        );

        grid.add(
                new Label("Освіта:"),
                0,
                3
        );
        grid.add(
                educationField,
                1,
                3
        );

        grid.add(
                new Label("Телефон:"),
                0,
                4
        );
        grid.add(
                phoneField,
                1,
                4
        );

        grid.add(
                new Label("Дата народження:"),
                0,
                5
        );
        grid.add(
                birthDatePicker,
                1,
                5
        );

        grid.add(
                new Label("Дата прийому:"),
                0,
                6
        );
        grid.add(
                hireDatePicker,
                1,
                6
        );

        grid.add(
                new Label("Статус:"),
                0,
                7
        );
        grid.add(
                statusComboBox,
                1,
                7
        );

        grid.add(
                new Label("Профком:"),
                0,
                8
        );
        grid.add(
                profkomField,
                1,
                8
        );

        grid.add(
                new Label("Кількість дітей:"),
                0,
                9
        );
        grid.add(
                childrenField,
                1,
                9
        );

        grid.add(
                new Label("Тип графіка:"),
                0,
                10
        );
        grid.add(
                scheduleTypeComboBox,
                1,
                10
        );

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButton) {
                return null;
            }

            String fullName =
                    safeText(
                            fullNameField.getText()
                    ).trim();

            String position =
                    safeText(
                            positionField.getText()
                    ).trim();

            String department =
                    safeText(
                            departmentField.getText()
                    ).trim();

            String phone =
                    safeText(
                            phoneField.getText()
                    ).trim();

            if (fullName.length() < 5
                    || !fullName.matches(
                    "[А-Яа-яІіЇїЄєҐґ'’\\- ]+"
            )) {

                showError(
                        "Помилка",
                        "Введіть коректне ПІБ українськими літерами"
                );
                return null;
            }

            if (position.isEmpty()) {
                showError(
                        "Помилка",
                        "Введіть посаду"
                );
                return null;
            }

            if (department.isEmpty()) {
                showError(
                        "Помилка",
                        "Введіть підрозділ"
                );
                return null;
            }

            if (!phone.isEmpty()
                    && !phone.matches(
                    "^(\\+380|0)\\d{9}$"
            )) {

                showError(
                        "Помилка",
                        "Телефон має бути у форматі "
                                + "+380XXXXXXXXX або 0XXXXXXXXX"
                );
                return null;
            }

            LocalDate birthDate =
                    birthDatePicker.getValue();

            if (birthDate != null
                    && birthDate.isAfter(
                    LocalDate.now().minusYears(16)
            )) {

                showError(
                        "Помилка",
                        "Працівнику має бути не менше 16 років"
                );
                return null;
            }

            LocalDate hireDate =
                    hireDatePicker.getValue();

            if (hireDate != null
                    && hireDate.isAfter(
                    LocalDate.now()
            )) {

                showError(
                        "Помилка",
                        "Дата прийому не може бути у майбутньому"
                );
                return null;
            }

            if (birthDate != null
                    && hireDate != null
                    && hireDate.isBefore(
                    birthDate.plusYears(16)
            )) {

                showError(
                        "Помилка",
                        "Дата прийому не може бути раніше "
                                + "досягнення працівником 16 років"
                );
                return null;
            }

            Employee resultEmployee =
                    employee == null
                            ? new Employee()
                            : employee;

            resultEmployee.setFullName(fullName);
            resultEmployee.setPosition(position);
            resultEmployee.setDepartment(department);
            resultEmployee.setEducation(
                    safeText(
                            educationField.getText()
                    ).trim()
            );
            resultEmployee.setPhone(phone);
            resultEmployee.setBirthDate(birthDate);
            resultEmployee.setHireDate(hireDate);
            resultEmployee.setStatus(
                    statusComboBox.getValue()
            );
            resultEmployee.setProfkom(
                    safeText(
                            profkomField.getText()
                    ).trim()
            );
            resultEmployee.setChildren(
                    safeText(
                            childrenField.getText()
                    ).trim()
            );

            String selectedSchedulePattern = getPatternCodeFromChoice(scheduleTypeComboBox.getValue());
            resultEmployee.setData(selectedSchedulePattern);
            resultEmployee.setPatternType(selectedSchedulePattern.isEmpty() ? "manual" : selectedSchedulePattern);

            return resultEmployee;
        });

        Optional<Employee> result =
                dialog.showAndWait();

        result.ifPresent(savedEmployee -> {
            try {
                employeeService.saveEmployee(
                        savedEmployee
                );

                String savedPattern = getEmployeeAutoPattern(savedEmployee);
                if (!savedPattern.isEmpty()) {
                    createScheduleForSingleEmployee(savedEmployee, savedPattern);
                }

                loadEmployees();
                loadSchedule();
                updateStatistics();

                showStatus(
                        employee == null
                                ? "Працівника додано"
                                : "Працівника оновлено"
                );

            } catch (SQLException e) {
                showError(
                        "Помилка",
                        "Не вдалося зберегти працівника: "
                                + e.getMessage()
                );
            }
        });
    }

    private void showEmployeeCard(
            Employee employee
    ) {
        if (employee == null) {
            return;
        }

        Dialog<ButtonType> dialog =
                new Dialog<>();

        dialog.setTitle(
                "Картка працівника"
        );
        dialog.setHeaderText(null);

        ButtonType editButton =
                new ButtonType(
                        "Редагувати",
                        ButtonBar.ButtonData.OK_DONE
                );

        ButtonType closeButton =
                new ButtonType(
                        "Закрити",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        editButton,
                        closeButton
                );

        VBox cardRoot =
                new VBox(18);

        cardRoot.setPadding(
                new Insets(25)
        );
        cardRoot.setPrefWidth(520);
        cardRoot.setStyle(
                "-fx-background-color: #f8fafc;"
        );

        VBox header =
                new VBox(8);

        header.setAlignment(Pos.CENTER);
        header.setStyle(
                "-fx-background-color: "
                        + "linear-gradient(to bottom right, "
                        + "#1565c0, #0d47a1); "
                        + "-fx-background-radius: 18; "
                        + "-fx-padding: 25;"
        );

        Text photo =
                new Text("👤");

        photo.setStyle(
                "-fx-font-size: 58px; "
                        + "-fx-fill: white;"
        );

        Text name =
                new Text(
                        safeText(
                                employee.getFullName()
                        )
                );

        name.setStyle(
                "-fx-fill: white; "
                        + "-fx-font-size: 22px; "
                        + "-fx-font-weight: bold;"
        );

        Text position =
                new Text(
                        safeText(
                                employee.getPosition()
                        )
                );

        position.setStyle(
                "-fx-fill: #e3f2fd; "
                        + "-fx-font-size: 14px;"
        );

        Text department =
                new Text(
                        safeText(
                                employee.getDepartment()
                        )
                );

        department.setStyle(
                "-fx-fill: #bbdefb; "
                        + "-fx-font-size: 12px;"
        );

        header.getChildren().addAll(
                photo,
                name,
                position,
                department
        );

        String birthDateText =
                employee.getBirthDate() == null
                        ? "Не вказано"
                        : employee
                        .getBirthDate()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "dd.MM.yyyy"
                                )
                        );

        String hireDateText =
                employee.getHireDate() == null
                        ? "Не вказано"
                        : employee
                        .getHireDate()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "dd.MM.yyyy"
                                )
                        );

        String experienceText =
                calculateExperience(
                        employee.getHireDate()
                );

        VBox informationBox =
                new VBox(10);

        informationBox.setStyle(
                "-fx-background-color: white; "
                        + "-fx-background-radius: 16; "
                        + "-fx-padding: 20; "
                        + "-fx-effect: dropshadow("
                        + "gaussian, rgba(0,0,0,0.06), "
                        + "10, 0, 0, 2);"
        );

        informationBox.getChildren().addAll(
                createCardLine(
                        "Освіта",
                        employee.getEducation()
                ),
                createCardLine(
                        "Телефон",
                        employee.getPhone()
                ),
                createCardLine(
                        "Дата народження",
                        birthDateText
                ),
                createCardLine(
                        "Дата прийому",
                        hireDateText
                ),
                createCardLine(
                        "Стаж роботи",
                        experienceText
                ),
                createCardLine(
                        "Статус",
                        employee.getStatus()
                ),
                createCardLine(
                        "Профком",
                        employee.getProfkom()
                ),
                createCardLine(
                        "Діти",
                        employee.getChildren()
                )
        );

        EmployeeMonthlyStatistics statistics =
                calculateEmployeeStatistics(
                        employee
                );

        HBox statisticsBox =
                new HBox(12);

        statisticsBox.setAlignment(Pos.CENTER);

        statisticsBox.getChildren().addAll(
                createStatBox(
                        "⏱",
                        formatHours(
                                statistics.hours()
                        ),
                        "Годин"
                ),
                createStatBox(
                        "📅",
                        String.valueOf(
                                statistics.workDays()
                        ),
                        "Робочих днів"
                ),
                createStatBox(
                        statistics.hours() > 176
                                ? "⚠"
                                : "✓",
                        statistics.hours() > 176
                                ? "Перевищено"
                                : "Норма",
                        "Норма 176 год"
                )
        );

        cardRoot.getChildren().addAll(
                header,
                informationBox,
                statisticsBox
        );

        dialog.getDialogPane()
                .setContent(cardRoot);

        Optional<ButtonType> result =
                dialog.showAndWait();

        if (result.isPresent()
                && result.get() == editButton) {

            showEmployeeDialog(employee);
        }
    }

    private HBox createCardLine(
            String title,
            String value
    ) {
        Label titleLabel =
                new Label(title + ":");

        titleLabel.setPrefWidth(150);
        titleLabel.setStyle(
                "-fx-text-fill: #607d8b; "
                        + "-fx-font-weight: bold;"
        );

        Label valueLabel =
                new Label(
                        value == null
                                || value.isBlank()
                                ? "Не вказано"
                                : value
                );

        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(300);
        valueLabel.setStyle(
                "-fx-text-fill: #1a2c3e;"
        );

        HBox row =
                new HBox(10);

        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                titleLabel,
                valueLabel
        );

        return row;
    }

    private VBox createStatBox(
            String icon,
            String value,
            String title
    ) {
        VBox box =
                new VBox(5);

        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(145);
        box.setMinHeight(105);
        box.setStyle(
                "-fx-background-color: #e3f2fd; "
                        + "-fx-background-radius: 14; "
                        + "-fx-padding: 15;"
        );

        Text iconText =
                new Text(icon);

        iconText.setStyle(
                "-fx-font-size: 23px;"
        );

        Text valueText =
                new Text(value);

        valueText.setStyle(
                "-fx-fill: #1565c0; "
                        + "-fx-font-size: 17px; "
                        + "-fx-font-weight: bold;"
        );

        Text titleText =
                new Text(title);

        titleText.setStyle(
                "-fx-fill: #607d8b; "
                        + "-fx-font-size: 11px;"
        );

        box.getChildren().addAll(
                iconText,
                valueText,
                titleText
        );

        return box;
    }

    private EmployeeMonthlyStatistics calculateEmployeeStatistics(
            Employee employee
    ) {
        if (employee == null
                || currentMonth == null) {

            return new EmployeeMonthlyStatistics(
                    0,
                    0
            );
        }

        for (EmployeeScheduleRow row
                : allScheduleRows) {

            if (!Objects.equals(row.getEmployee().getId(), employee.getId())) {

                continue;
            }

            double hours =
                    calculateTotalHours(row);

            int workDays = 0;

            for (int day = 1;
                 day <= currentMonth.lengthOfMonth();
                 day++) {

                String code =
                        normalizeShiftCode(
                                row.getShiftCodeForDay(day)
                        );

                if ("1".equals(code)
                        || "7.00".equals(code)
                        || "8.25".equals(code)) {

                    workDays++;
                }
            }

            return new EmployeeMonthlyStatistics(
                    hours,
                    workDays
            );
        }

        try {
            Map<Integer, List<Shift>> shiftMap =
                    shiftDAO.findShiftsForMonth(
                            currentMonth
                    );

            List<Shift> shifts =
                    shiftMap.getOrDefault(
                            employee.getId(),
                            new ArrayList<>()
                    );

            double hours = 0;
            int workDays = 0;

            for (Shift shift : shifts) {
                String code =
                        normalizeShiftCode(
                                shift.getCode()
                        );

                switch (code) {
                    case "1" -> {
                        hours += 24;
                        workDays++;
                    }
                    case "7.00" -> {
                        hours += 7;
                        workDays++;
                    }
                    case "8.25" -> {
                        hours += 8.25;
                        workDays++;
                    }
                    default -> {
                    }
                }
            }

            return new EmployeeMonthlyStatistics(
                    hours,
                    workDays
            );

        } catch (SQLException e) {
            return new EmployeeMonthlyStatistics(
                    0,
                    0
            );
        }
    }

    private String calculateExperience(
            LocalDate hireDate
    ) {
        if (hireDate == null) {
            return "Не вказано";
        }

        Period period =
                Period.between(
                        hireDate,
                        LocalDate.now()
                );

        if (period.isNegative()) {
            return "Некоректна дата";
        }

        return period.getYears()
                + " р. "
                + period.getMonths()
                + " міс.";
    }

    @FXML
    private void exportToExcel() {
        try {
            saveScheduleSilent();

            if (currentMonth == null) {
                showError(
                        "Помилка",
                        "Не вибрано місяць"
                );
                return;
            }

            FileChooser fileChooser =
                    new FileChooser();

            fileChooser.setTitle(
                    "Зберегти Excel-файл"
            );

            fileChooser.setInitialFileName(
                    "Табель_"
                            + currentMonth.getMonthValue()
                            + "_"
                            + currentMonth.getYear()
                            + ".xlsx"
            );

            fileChooser
                    .getExtensionFilters()
                    .add(
                            new FileChooser.ExtensionFilter(
                                    "Excel-файли",
                                    "*.xlsx"
                            )
                    );

            File file =
                    fileChooser.showSaveDialog(
                            scheduleTableView
                                    .getScene()
                                    .getWindow()
                    );

            if (file == null) {
                return;
            }

            ExcelExporter.exportSchedule(
                    scheduleService,
                    currentMonth,
                    file
            );

            showStatus(
                    "Excel-файл створено: "
                            + file.getAbsolutePath()
            );

        } catch (Exception e) {
            showError(
                    "Помилка експорту",
                    e.getMessage()
            );
        }
    }

    private void loadAppSettings() {
        selectedTheme =
                preferences.get(
                        "theme",
                        "Стандартна тема"
                );

        selectedFontSize =
                preferences.getDouble(
                        "fontSize",
                        14
                );

        autoSaveEnabled =
                preferences.getBoolean(
                        "autoSave",
                        true
                );

        confirmBeforeReset =
                preferences.getBoolean(
                        "confirmReset",
                        true
                );
    }

    private void saveAppSettings() {
        preferences.put(
                "theme",
                selectedTheme
        );

        preferences.putDouble(
                "fontSize",
                selectedFontSize
        );

        preferences.putBoolean(
                "autoSave",
                autoSaveEnabled
        );

        preferences.putBoolean(
                "confirmReset",
                confirmBeforeReset
        );
    }

    private void applyAppSettings() {
        if (rootPane == null) {
            return;
        }

        String rootColor;
        String contentColor;

        switch (selectedTheme) {
            case "Світла тема" -> {
                rootColor = "#ffffff";
                contentColor = "#f8fafc";
            }
            case "Синя тема" -> {
                rootColor = "#dbeafe";
                contentColor = "#e3f2fd";
            }
            default -> {
                rootColor = "#f0f4f8";
                contentColor = "#f0f4f8";
            }
        }

        rootPane.setStyle(
                "-fx-background-color: "
                        + rootColor
                        + "; -fx-font-size: "
                        + selectedFontSize
                        + "px;"
        );

        homeView.setStyle(
                "-fx-background-color: "
                        + contentColor
                        + "; -fx-padding: 30 25 30 25;"
        );

        scheduleFullView.setStyle(
                "-fx-background-color: "
                        + contentColor
                        + "; -fx-padding: 20 25 25 25;"
        );

        employeesFullView.setStyle(
                "-fx-background-color: "
                        + contentColor
                        + "; -fx-padding: 20 25 25 25;"
        );

        infoFullView.setStyle(
                "-fx-background: "
                        + contentColor
                        + "; -fx-background-color: "
                        + contentColor
                        + ";"
        );

        scheduleTableView.setStyle(
                "-fx-font-size: "
                        + selectedFontSize
                        + "px; "
                        + "-fx-background-color: white; "
                        + "-fx-border-color: #e0e0e0; "
                        + "-fx-border-radius: 12; "
                        + "-fx-background-radius: 12;"
        );

        employeesTableView.setStyle(
                "-fx-font-size: "
                        + selectedFontSize
                        + "px; "
                        + "-fx-background-color: white; "
                        + "-fx-border-color: #e0e0e0; "
                        + "-fx-border-radius: 12; "
                        + "-fx-background-radius: 12;"
        );
    }

    @FXML
    private void openSettings() {
        Dialog<ButtonType> dialog =
                new Dialog<>();

        dialog.setTitle("Налаштування");
        dialog.setHeaderText(null);

        ButtonType saveButton =
                new ButtonType(
                        "Зберегти",
                        ButtonBar.ButtonData.OK_DONE
                );

        ButtonType closeButton =
                new ButtonType(
                        "Закрити",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        saveButton,
                        closeButton
                );

        VBox settingsRoot =
                new VBox(18);

        settingsRoot.setPadding(
                new Insets(25)
        );
        settingsRoot.setPrefWidth(540);
        settingsRoot.setStyle(
                "-fx-background-color: #f8fafc;"
        );

        Text title =
                new Text(
                        "⚙️ Налаштування системи"
                );

        title.setStyle(
                "-fx-fill: #1a2c3e; "
                        + "-fx-font-size: 24px; "
                        + "-fx-font-weight: bold;"
        );

        Text subtitle =
                new Text(
                        "Оберіть параметри та натисніть «Зберегти»"
                );

        subtitle.setStyle(
                "-fx-fill: #607d8b; "
                        + "-fx-font-size: 13px;"
        );

        VBox appearanceBox =
                new VBox(12);

        appearanceBox.setStyle(
                "-fx-background-color: white; "
                        + "-fx-background-radius: 16; "
                        + "-fx-padding: 18; "
                        + "-fx-effect: dropshadow("
                        + "gaussian, rgba(0,0,0,0.06), "
                        + "10, 0, 0, 2);"
        );

        Text appearanceTitle =
                new Text("🎨 Вигляд");

        appearanceTitle.setStyle(
                "-fx-fill: #1565c0; "
                        + "-fx-font-size: 18px; "
                        + "-fx-font-weight: bold;"
        );

        ComboBox<String> themeComboBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                "Стандартна тема",
                                "Світла тема",
                                "Синя тема"
                        )
                );

        themeComboBox.setValue(
                selectedTheme
        );
        themeComboBox.setPrefWidth(250);

        Slider fontSizeSlider =
                new Slider(
                        12,
                        18,
                        selectedFontSize
                );

        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(true);
        fontSizeSlider.setMajorTickUnit(2);
        fontSizeSlider.setMinorTickCount(1);
        fontSizeSlider.setSnapToTicks(true);
        fontSizeSlider.setPrefWidth(250);

        Label fontValueLabel =
                new Label(
                        String.valueOf(
                                (int) selectedFontSize
                        )
                );

        fontSizeSlider
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                fontValueLabel.setText(
                                        String.valueOf(
                                                newValue.intValue()
                                        )
                                )
                );

        HBox themeRow =
                new HBox(15);

        themeRow.setAlignment(
                Pos.CENTER_LEFT
        );

        Label themeLabel =
                new Label(
                        "Тема інтерфейсу:"
                );

        themeLabel.setPrefWidth(160);

        themeRow.getChildren().addAll(
                themeLabel,
                themeComboBox
        );

        HBox fontRow =
                new HBox(15);

        fontRow.setAlignment(
                Pos.CENTER_LEFT
        );

        Label fontLabel =
                new Label(
                        "Розмір тексту:"
                );

        fontLabel.setPrefWidth(160);

        fontRow.getChildren().addAll(
                fontLabel,
                fontSizeSlider,
                fontValueLabel
        );

        appearanceBox.getChildren().addAll(
                appearanceTitle,
                themeRow,
                fontRow
        );

        VBox dataBox =
                new VBox(12);

        dataBox.setStyle(
                "-fx-background-color: white; "
                        + "-fx-background-radius: 16; "
                        + "-fx-padding: 18; "
                        + "-fx-effect: dropshadow("
                        + "gaussian, rgba(0,0,0,0.06), "
                        + "10, 0, 0, 2);"
        );

        Text dataTitle =
                new Text(
                        "💾 Робота з даними"
                );

        dataTitle.setStyle(
                "-fx-fill: #1565c0; "
                        + "-fx-font-size: 18px; "
                        + "-fx-font-weight: bold;"
        );

        CheckBox autoSaveCheckBox =
                new CheckBox(
                        "Автоматично зберігати зміни "
                                + "кожні 30 секунд"
                );

        autoSaveCheckBox.setSelected(
                autoSaveEnabled
        );

        CheckBox confirmationCheckBox =
                new CheckBox(
                        "Запитувати підтвердження "
                                + "перед очищенням табеля"
                );

        confirmationCheckBox.setSelected(
                confirmBeforeReset
        );

        Button saveNowButton =
                new Button(
                        "💾 Зберегти табель зараз"
                );

        saveNowButton.setStyle(
                "-fx-background-color: #1565c0; "
                        + "-fx-text-fill: white; "
                        + "-fx-font-weight: bold; "
                        + "-fx-padding: 9 18; "
                        + "-fx-background-radius: 8; "
                        + "-fx-cursor: hand;"
        );

        saveNowButton.setOnAction(event -> {
            saveScheduleSilent();

            showStatus(
                    "Табель збережено"
            );
        });

        Button resetSettingsButton =
                new Button(
                        "↩ Стандартні налаштування"
                );

        resetSettingsButton.setStyle(
                "-fx-background-color: #e3f2fd; "
                        + "-fx-text-fill: #1565c0; "
                        + "-fx-font-weight: bold; "
                        + "-fx-padding: 9 18; "
                        + "-fx-background-radius: 8; "
                        + "-fx-cursor: hand;"
        );

        resetSettingsButton.setOnAction(event -> {
            themeComboBox.setValue(
                    "Стандартна тема"
            );

            fontSizeSlider.setValue(14);
            autoSaveCheckBox.setSelected(true);
            confirmationCheckBox.setSelected(true);
        });

        HBox actionButtons =
                new HBox(12);

        actionButtons.getChildren().addAll(
                saveNowButton,
                resetSettingsButton
        );

        dataBox.getChildren().addAll(
                dataTitle,
                autoSaveCheckBox,
                confirmationCheckBox,
                actionButtons
        );

        VBox aboutBox =
                new VBox(10);

        aboutBox.setStyle(
                "-fx-background-color: #e3f2fd; "
                        + "-fx-background-radius: 16; "
                        + "-fx-padding: 18; "
                        + "-fx-border-color: #bbdef5; "
                        + "-fx-border-radius: 16; "
                        + "-fx-border-width: 1.5;"
        );

        Text aboutTitle =
                new Text("ℹ️ Про систему");

        aboutTitle.setStyle(
                "-fx-fill: #1565c0; "
                        + "-fx-font-size: 18px; "
                        + "-fx-font-weight: bold;"
        );

        Text aboutText =
                new Text(
                        "Інформаційна система управління "
                                + "графіками та змінами персоналу. "
                                + "Розробник: Богомаз Еліна. "
                                + "Версія 1.0."
                );

        aboutText.setWrappingWidth(480);
        aboutText.setStyle(
                "-fx-fill: #455a64; "
                        + "-fx-font-size: 13px; "
                        + "-fx-line-spacing: 4;"
        );

        aboutBox.getChildren().addAll(
                aboutTitle,
                aboutText
        );

        settingsRoot.getChildren().addAll(
                title,
                subtitle,
                appearanceBox,
                dataBox,
                aboutBox
        );

        dialog.getDialogPane()
                .setContent(settingsRoot);

        Optional<ButtonType> result =
                dialog.showAndWait();

        if (result.isPresent()
                && result.get() == saveButton) {

            selectedTheme =
                    themeComboBox.getValue();

            selectedFontSize =
                    fontSizeSlider.getValue();

            autoSaveEnabled =
                    autoSaveCheckBox.isSelected();

            confirmBeforeReset =
                    confirmationCheckBox.isSelected();

            saveAppSettings();
            applyAppSettings();

            showStatus(
                    "Налаштування збережено"
            );
        }
    }

    @FXML
    private void openHomeView() {
        saveScheduleSilent();

        homeView.setVisible(true);
        homeView.setManaged(true);

        scheduleFullView.setVisible(false);
        scheduleFullView.setManaged(false);

        employeesFullView.setVisible(false);
        employeesFullView.setManaged(false);

        infoFullView.setVisible(false);
        infoFullView.setManaged(false);

        setActiveMenuItem(homeMenuItem);
        pageTitle.setText("Головна");
    }

    @FXML
    private void openScheduleView() {
        saveScheduleSilent();

        homeView.setVisible(false);
        homeView.setManaged(false);

        scheduleFullView.setVisible(true);
        scheduleFullView.setManaged(true);

        employeesFullView.setVisible(false);
        employeesFullView.setManaged(false);

        infoFullView.setVisible(false);
        infoFullView.setManaged(false);

        setActiveMenuItem(scheduleMenuItem);
        pageTitle.setText("Табель");

        loadSchedule();
    }

    @FXML
    private void openEmployeesView() {
        saveScheduleSilent();

        homeView.setVisible(false);
        homeView.setManaged(false);

        scheduleFullView.setVisible(false);
        scheduleFullView.setManaged(false);

        employeesFullView.setVisible(true);
        employeesFullView.setManaged(true);

        infoFullView.setVisible(false);
        infoFullView.setManaged(false);

        setActiveMenuItem(employeesMenuItem);
        pageTitle.setText("Працівники");

        loadEmployees();
    }

    @FXML
    private void openInfoView() {
        saveScheduleSilent();

        homeView.setVisible(false);
        homeView.setManaged(false);

        scheduleFullView.setVisible(false);
        scheduleFullView.setManaged(false);

        employeesFullView.setVisible(false);
        employeesFullView.setManaged(false);

        infoFullView.setVisible(true);
        infoFullView.setManaged(true);

        setActiveMenuItem(infoMenuItem);
        pageTitle.setText("Інформація");
    }

    @FXML
    private void logout() {
        saveScheduleSilent();

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Вихід із системи"
        );
        confirmation.setHeaderText(
                "Ви впевнені, що хочете вийти?"
        );
        confirmation.setContentText(
                "Усі зміни збережено."
        );

        confirmation
                .showAndWait()
                .ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        Platform.exit();
                    }
                });
    }

    @FXML
    private void exitApplication() {
        saveScheduleSilent();
        Platform.exit();
    }

    private void setActiveMenuItem(
            Button activeButton
    ) {
        String activeStyle =
                "-fx-background-color: #e3f2fd; "
                        + "-fx-background-radius: 12; "
                        + "-fx-padding: 12 18; "
                        + "-fx-min-width: 180; "
                        + "-fx-alignment: CENTER_LEFT; "
                        + "-fx-font-size: 14px; "
                        + "-fx-text-fill: #1565c0; "
                        + "-fx-font-weight: bold;";

        String inactiveStyle =
                "-fx-background-color: transparent; "
                        + "-fx-background-radius: 12; "
                        + "-fx-padding: 12 18; "
                        + "-fx-min-width: 180; "
                        + "-fx-alignment: CENTER_LEFT; "
                        + "-fx-font-size: 14px; "
                        + "-fx-text-fill: #6c7f8f;";

        homeMenuItem.setStyle(inactiveStyle);
        scheduleMenuItem.setStyle(inactiveStyle);
        employeesMenuItem.setStyle(inactiveStyle);
        infoMenuItem.setStyle(inactiveStyle);

        activeButton.setStyle(activeStyle);
    }

    private void showError(
            String title,
            String message
    ) {
        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                message == null
                        ? "Невідома помилка"
                        : message
        );

        alert.showAndWait();
    }

    private void showStatus(
            String message
    ) {
        System.out.println(
                "Статус: " + message
        );
    }

    private String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    private String formatHours(
            double hours
    ) {
        if (hours == Math.floor(hours)) {
            return String.valueOf(
                    (int) hours
            );
        }

        return String.format(
                Locale.US,
                "%.2f",
                hours
        );
    }

    private record EmployeeMonthlyStatistics(
            double hours,
            int workDays
    ) {
    }

    public static class EmployeeScheduleRow {

        private final Employee employee;
        private final Map<Integer, String> shiftCodes =
                new HashMap<>();

        public EmployeeScheduleRow(
                Employee employee,
                List<Shift> shifts,
                YearMonth month
        ) {
            this.employee = employee;

            for (int day = 1;
                 day <= month.lengthOfMonth();
                 day++) {

                shiftCodes.put(day, "X");
            }

            for (Shift shift : shifts) {
                if (shift.getCode() == null
                        || shift.getCode().isEmpty()) {

                    continue;
                }

                shiftCodes.put(
                        shift.getDate().getDayOfMonth(),
                        shift.getCode()
                );
            }
        }

        public Employee getEmployee() {
            return employee;
        }

        public String getShiftCodeForDay(
                int day
        ) {
            return shiftCodes.getOrDefault(
                    day,
                    "X"
            );
        }

        public void setShiftForDay(
                int day,
                String code
        ) {
            shiftCodes.put(day, code);
        }
    }
}