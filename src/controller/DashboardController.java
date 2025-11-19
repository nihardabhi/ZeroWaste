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
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController extends BaseController implements Initializable {
    
    // Dashboard Stats Labels
    @FXML private Label totalItems;
    @FXML private Label nearExpiry;
    @FXML private Label expiredItems;
    @FXML private Label todayExpiring;
    @FXML private Label lowStock;
    
    // User Greeting Labels
    @FXML private Label usernameLabel;
    @FXML private Label greetingLabel;
    @FXML private Label userGreetingLabel;
    
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
        
        // Set user greeting
        setUserGreeting();
        
        setupNavigationButtons();
        loadDashboardStats();
    }
    
    private void setUserGreeting() {
        User user = getCurrentUser();
        if (user != null) {
            // Set sidebar username - use getDisplayName() for better presentation
            if (usernameLabel != null) {
                usernameLabel.setText(user.getDisplayName());
            }
            
            // Set main greeting with time-based message
            if (greetingLabel != null && userGreetingLabel != null) {
                String timeGreeting = getTimeBasedGreeting();
                greetingLabel.setText(timeGreeting + ",");
                userGreetingLabel.setText(user.getDisplayName() + "!");
            }
        }
    }
    
    private String getTimeBasedGreeting() {
        int hour = LocalTime.now().getHour();
        
        if (hour >= 5 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            return "Good Evening";
        } else {
            return "Good Night";
        }
    }
    
    private void setupNavigationButtons() {
        if (dashboardBtn != null) {
            dashboardBtn.setOnAction(e -> refreshDashboard());
            dashboardBtn.setStyle("-fx-font-weight: bold;");
        }
        
        if (inventoryBtn != null) {
            inventoryBtn.setOnAction(e -> loadView("/views/inventory.fxml"));
        }
        
        if (recipeBtn != null) {
            recipeBtn.setOnAction(e -> loadView("/views/recipes.fxml"));
        }
        
        if (preferencesBtn != null) {
            preferencesBtn.setOnAction(e -> loadView("/views/user-preferences.fxml"));
        }
        
        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> handleLogout());
            logoutBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        }
    }
    
    private void loadDashboardStats() {
        User user = getCurrentUser();
        if (user == null) return;
        
        Pantry pantry = user.getPantry();
        
        if (totalItems != null) {
            totalItems.setText(String.valueOf(pantry.getTotalItemCount()));
        }
        
        if (nearExpiry != null) {
            List<FoodItem> expiringItems = pantry.getExpiringWithinDays(3);
            nearExpiry.setText(String.valueOf(expiringItems.size()));
            
            // Add style if items are expiring
            if (!expiringItems.isEmpty()) {
                nearExpiry.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 24;");
            }
        }
        
        if (expiredItems != null) {
            int expiredCount = pantry.getExpiredItemCount();
            expiredItems.setText(String.valueOf(expiredCount));
            
            // Add warning style if there are expired items
            if (expiredCount > 0) {
                expiredItems.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold; -fx-font-size: 24;");
            }
        }
        
        if (todayExpiring != null) {
            int todayCount = 0;
            for (FoodItem item : pantry.getAllItems()) {
                if (item.getExpireDate().equals(LocalDate.now())) {
                    todayCount++;
                }
            }
            todayExpiring.setText(String.valueOf(todayCount));
            
            if (todayCount > 0) {
                todayExpiring.setStyle("-fx-text-fill: #ffa500; -fx-font-weight: bold; -fx-font-size: 24;");
            }
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
            
            // Update button styles to show active view
            resetButtonStyles();
            if (fxmlPath.contains("inventory")) {
                inventoryBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
            } else if (fxmlPath.contains("recipes")) {
                recipeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
            } else if (fxmlPath.contains("preferences")) {
                preferencesBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to load view: " + e.getMessage());
        }
    }
    
    private void resetButtonStyles() {
        if (dashboardBtn != null) dashboardBtn.setStyle("");
        if (inventoryBtn != null) inventoryBtn.setStyle("");
        if (recipeBtn != null) recipeBtn.setStyle("");
        if (preferencesBtn != null) preferencesBtn.setStyle("");
    }
    
    private void refreshDashboard() {
        // Reset button styles and highlight dashboard
        resetButtonStyles();
        if (dashboardBtn != null) {
            dashboardBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
        }
        
        // Refresh user greeting
        setUserGreeting();
        
        // Refresh the stats
        loadDashboardStats();
        
        // If content area has been changed, restore dashboard view
        if (contentArea.getChildren().isEmpty() || 
            !(contentArea.getChildren().get(0) instanceof VBox)) {
            
            contentArea.getChildren().clear();
            
            // Recreate the dashboard view
            VBox dashboardView = createDashboardView();
            contentArea.getChildren().add(dashboardView);
        }
    }
    
    private VBox createDashboardView() {
        VBox dashboard = new VBox(15);
        dashboard.setStyle("-fx-padding: 20;");
        
        // Add greeting at the top
        HBox greetingBox = new HBox(10);
        greetingBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label greeting = new Label(getTimeBasedGreeting() + ",");
        greeting.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        User user = getCurrentUser();
        Label username = new Label(user != null ? user.getDisplayName() + "!" : "User!");
        username.setStyle("-fx-font-size: 24; -fx-text-fill: #4CAF50;");
        
        greetingBox.getChildren().addAll(greeting, username);
        
        Label title = new Label("Dashboard Overview");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Separator separator = new Separator();
        separator.setPrefWidth(200);
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(10);
        statsGrid.setPrefWidth(600);
        
        // Get current pantry for stats
        Pantry pantry = getCurrentUser().getPantry();
        
        // Create stat boxes with current data
        VBox totalBox = createStatBox("Total Food Items", 
            String.valueOf(pantry.getTotalItemCount()), "#e0e0e0");
        statsGrid.add(totalBox, 0, 0);
        
        List<FoodItem> expiringItems = pantry.getExpiringWithinDays(3);
        VBox nearBox = createStatBox("Near Expiry (3 days)", 
            String.valueOf(expiringItems.size()), "#ffe0b2");
        if (!expiringItems.isEmpty()) {
            nearBox.setStyle(nearBox.getStyle() + " -fx-border-color: #ff6b6b; -fx-border-width: 2;");
        }
        statsGrid.add(nearBox, 1, 0);
        
        int expiredCount = pantry.getExpiredItemCount();
        VBox expiredBox = createStatBox("Expired Items", 
            String.valueOf(expiredCount), "#ffcdd2");
        if (expiredCount > 0) {
            expiredBox.setStyle(expiredBox.getStyle() + " -fx-border-color: #ff0000; -fx-border-width: 2;");
        }
        statsGrid.add(expiredBox, 0, 1);
        
        VBox todayBox = createStatBox("Today's Expiring", 
            String.valueOf(getItemsExpiringToday()), "#fff9c4");
        statsGrid.add(todayBox, 1, 1);
        
        VBox lowBox = createStatBox("Low Stock Items", 
            String.valueOf(pantry.getLowStockCount()), "#c5e1a5");
        statsGrid.add(lowBox, 0, 2);
        GridPane.setColumnSpan(lowBox, 2);
        
        // Add all components to dashboard
        dashboard.getChildren().addAll(greetingBox, title, separator, statsGrid);
        
        // Add warning message if there are critical items
        if (expiredCount > 0 || !expiringItems.isEmpty()) {
            Separator warningSep = new Separator();
            Label warningLabel = new Label("Attention needed: You have items that need immediate attention!");
            warningLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 12;");
            dashboard.getChildren().addAll(warningSep, warningLabel);
        }
        
        return dashboard;
    }
    
    private VBox createStatBox(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: " + color + "; -fx-padding: 15; " +
                    "-fx-min-width: 200; -fx-pref-width: 250; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5;");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 12; -fx-text-fill: #333;");
        
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout Confirmation");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.setContentText("You will need to login again to access your account.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            clearSession();
            Stage currentStage = getCurrentStage(logoutBtn);
            currentStage.close();
            navigateToPage("/views/login.fxml", null, "ZeroWaste - Login");
        }
    }
}