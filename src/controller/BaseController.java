package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import models.Entities.User;
import java.io.IOException;

public class BaseController {
    // Shared user instance across all controllers
    protected static User currentUser = null;
    
    // Common alert methods
    protected void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    protected void showSuccessAlert(String message) {
        showAlert(AlertType.INFORMATION, "Success", message);
    }
    
    protected void showErrorAlert(String message) {
        showAlert(AlertType.ERROR, "Error", message);
    }
    
    protected void showWarningAlert(String message) {
        showAlert(AlertType.WARNING, "Warning", message);
    }
    
    // Navigation helper method
    protected void navigateToPage(String fxmlPath, Stage currentStage, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = currentStage != null ? currentStage : new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showErrorAlert("Failed to load page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Get current stage from any node
    protected Stage getCurrentStage(javafx.scene.Node node) {
        return (Stage) node.getScene().getWindow();
    }
    
    // User session management
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    public static User getCurrentUser() {
        return currentUser;
    }
    
    public static void clearSession() {
        currentUser = null;
    }
    
    public static boolean isUserLoggedIn() {
        return currentUser != null;
    }
}