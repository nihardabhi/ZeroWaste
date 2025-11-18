package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Entities.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController extends BaseController implements Initializable {
    
    // Dashboard Stats Labels
    @FXML private Label totalItems;
    @FXML private Label nearExpiry;
    @FXML private Label expiredItems;
    @FXML private Label todayExpiring;
    @FXML private Label lowStock;
    
    // Navigation Buttons
    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button recipeBtn;
    @FXML private Button preferencesBtn;
    @FXML private Button logoutBtn;
    
    @FXML private StackPane contentArea;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!isUserLoggedIn()) {
            // Create test user if needed
            User testUser = new User("test@example.com", "password", "test");
            testUser.setupProfile("Test", "User");
            setCurrentUser(testUser);
        }
        
        setupNavigationButtons();
        loadDashboardStats();
    }
    
    private void setupNavigationButtons() {
        dashboardBtn.setOnAction(e -> refreshDashboard());
        inventoryBtn.setOnAction(e -> loadView("/views/inventory.fxml"));
        recipeBtn.setOnAction(e -> loadView("/views/recipes.fxml"));
        preferencesBtn.setOnAction(e -> loadView("/views/user-preferences.fxml"));
        logoutBtn.setOnAction(e -> handleLogout());
    }
    
    private void loadDashboardStats() {
        User user = getCurrentUser();
        if (user == null) return;
        
        Pantry pantry = user.getPantry();
        
        if (totalItems != null) {
            totalItems.setText(String.valueOf(pantry.getTotalItemCount()));
        }
        
        if (nearExpiry != null) {
            nearExpiry.setText(String.valueOf(pantry.getExpiringWithinDays(3).size()));
        }
        
        if (expiredItems != null) {
            expiredItems.setText(String.valueOf(pantry.getExpiredItemCount()));
        }
        
        if (todayExpiring != null) {
            int todayCount = 0;
            for (FoodItem item : pantry.getAllItems()) {
                if (item.getExpireDate().equals(LocalDate.now())) {
                    todayCount++;
                }
            }
            todayExpiring.setText(String.valueOf(todayCount));
        }
        
        if (lowStock != null) {
            lowStock.setText(String.valueOf(pantry.getLowStockCount()));
        }
    }
    
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to load view: " + e.getMessage());
        }
    }
    
    private void refreshDashboard() {
        // Don't try to reload the entire FXML, just refresh the stats
        loadDashboardStats();
        
        // If content area has been changed, restore dashboard view
        if (contentArea.getChildren().isEmpty() || 
            !(contentArea.getChildren().get(0) instanceof VBox)) {
            
            contentArea.getChildren().clear();
            
            // Recreate the dashboard stats view
            VBox dashboardView = createDashboardView();
            contentArea.getChildren().add(dashboardView);
        }
    }
    
    private VBox createDashboardView() {
        VBox dashboard = new VBox(15);
        dashboard.setStyle("-fx-padding: 20;");
        
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(10);
        
        // Recreate stat boxes
        Pantry pantry = getCurrentUser().getPantry();
        
        VBox totalBox = createStatBox("Total Food Items", 
            String.valueOf(pantry.getTotalItemCount()), "#e0e0e0");
        statsGrid.add(totalBox, 0, 0);
        
        VBox nearBox = createStatBox("Near Expiry (3 days)", 
            String.valueOf(pantry.getExpiringWithinDays(3).size()), "#ffe0b2");
        statsGrid.add(nearBox, 1, 0);
        
        VBox expiredBox = createStatBox("Expired Items", 
            String.valueOf(pantry.getExpiredItemCount()), "#ffcdd2");
        statsGrid.add(expiredBox, 0, 1);
        
        VBox todayBox = createStatBox("Today's Expiring", 
            String.valueOf(getItemsExpiringToday()), "#fff9c4");
        statsGrid.add(todayBox, 1, 1);
        
        VBox lowBox = createStatBox("Low Stock Items", 
            String.valueOf(pantry.getLowStockCount()), "#c5e1a5");
        statsGrid.add(lowBox, 0, 2);
        GridPane.setColumnSpan(lowBox, 2);
        
        dashboard.getChildren().addAll(title, statsGrid);
        return dashboard;
    }
    
    private VBox createStatBox(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: " + color + "; -fx-padding: 15; " +
                    "-fx-min-width: 200; -fx-pref-width: 250;");
        
        Label labelText = new Label(label);
        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        
        box.getChildren().addAll(labelText, valueText);
        return box;
    }
    
    private int getItemsExpiringToday() {
        int count = 0;
        for (FoodItem item : getCurrentUser().getPantry().getAllItems()) {
            if (item.getExpireDate().equals(LocalDate.now())) {
                count++;
            }
        }
        return count;
    }
    
    private void handleLogout() {
        clearSession();
        Stage currentStage = getCurrentStage(logoutBtn);
        currentStage.close();
        navigateToPage("/views/login.fxml", null, "ZeroWaste - Login");
    }
}