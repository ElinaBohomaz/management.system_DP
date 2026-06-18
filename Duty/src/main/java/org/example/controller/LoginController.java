package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private StackPane rootPane;
    @FXML private StackPane helpIcon;

    private Stage infoWindow;
    private static final String VALID_LOGIN = "Богомаз Еліна";
    private static final String VALID_PASSWORD = "160707";

    @FXML
    private void initialize() {
        loadSavedCredentials();
        setupEnterKeyHandler();
        loginButton.setDefaultButton(true);
        setupFieldStyles();
        setupButtonHover();
        setupHelpClick();
        statusLabel.setText("");
    }

    private void setupFieldStyles() {
        String normalStyle = "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1.5; -fx-padding: 14 14; -fx-font-size: 14px; -fx-background-color: #fafbfc;";
        String focusStyle = "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #1565c0; -fx-border-width: 2; -fx-padding: 14 14; -fx-font-size: 14px; -fx-background-color: #fafbfc;";

        loginField.setStyle(normalStyle);
        passwordField.setStyle(normalStyle);

        loginField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            loginField.setStyle(newVal ? focusStyle : normalStyle);
            if (newVal) {
                statusLabel.setText("");
            }
        });

        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            passwordField.setStyle(newVal ? focusStyle : normalStyle);
            if (newVal) {
                statusLabel.setText("");
            }
        });
    }

    private void setupButtonHover() {
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #0d47a1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 12; -fx-padding: 14 0;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 12; -fx-padding: 14 0;"));
    }

    private void setupHelpClick() {
        if (helpIcon != null) {
            helpIcon.setOnMouseClicked(e -> {
                if (infoWindow == null || !infoWindow.isShowing()) {
                    showInfoDialog();
                } else {
                    infoWindow.close();
                    infoWindow = null;
                }
            });
        }
    }

    private void showInfoDialog() {
        infoWindow = new Stage();
        infoWindow.initModality(Modality.NONE);
        infoWindow.setTitle("Довідка");
        infoWindow.initOwner(helpIcon.getScene().getWindow());

        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(25));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-min-width: 380; -fx-min-height: 250;");

        Label titleLabel = new Label("📋 Інформація для входу");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0d47a1;");

        Label loginInfo = new Label("У разі втрати або забуття логіна чи пароля\nзвертайтеся за електронною адресою:");
        loginInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-text-alignment: center;");

        Label emailInfo = new Label("elinabogomaz445@gmail.com");
        emailInfo.setStyle("-fx-font-size: 14px; -fx-text-fill: #1565c0; -fx-font-weight: bold;");

        Button closeButton = new Button("Закрити");
        closeButton.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");
        closeButton.setOnAction(e -> {
            infoWindow.close();
            infoWindow = null;
        });

        vbox.getChildren().addAll(titleLabel, loginInfo, emailInfo, closeButton);

        Scene scene = new Scene(vbox, 380, 250);
        infoWindow.setScene(scene);
        infoWindow.setResizable(false);
        infoWindow.show();

        infoWindow.setOnCloseRequest(e -> infoWindow = null);
    }

    private void loadSavedCredentials() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
            String savedLogin = prefs.get("saved_login", "");
            if (!savedLogin.isEmpty()) {
                loginField.setText(savedLogin);
                passwordField.requestFocus();
            }
        } catch (Exception e) {
            System.err.println("Не вдалося завантажити збережені дані: " + e.getMessage());
        }
    }

    private void saveCredentials() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
            prefs.put("saved_login", loginField.getText().trim());
        } catch (Exception e) {
            System.err.println("Не вдалося зберегти дані: " + e.getMessage());
        }
    }

    private void setupEnterKeyHandler() {
        rootPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        loginField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });
    }

    @FXML
    private void handleLogin() {
        String login = loginField.getText() == null ? "" : loginField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Введіть дані");
            statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");

            if (login.isEmpty()) {
                loginField.requestFocus();
            } else {
                passwordField.requestFocus();
            }

            return;
        }

        if (VALID_LOGIN.equals(login) && VALID_PASSWORD.equals(password)) {
            statusLabel.setText("Вхід виконано успішно! Завантаження...");
            statusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 12px;");
            saveCredentials();
            openMainApplication();
        } else {
            statusLabel.setText("Помилка в логіні або паролі");
            statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    private void openMainApplication() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ui/MainWindow.fxml"));
            Parent root = loader.load();

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1920, 1080);

            try {
                String cssPath = "/org/example/ui/styles.css";
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
            } catch (Exception e) {
                System.err.println("CSS не знайдено: " + e.getMessage());
            }

            currentStage.setTitle("Графік змін працівників - Полтававодоканал");
            currentStage.setScene(scene);
            currentStage.setMaximized(true);
            currentStage.show();

        } catch (Exception e) {
            System.err.println("Помилка відкриття головного додатку: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Помилка відкриття: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
        }
    }
}