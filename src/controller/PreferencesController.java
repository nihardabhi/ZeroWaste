package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Entities.User;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class PreferencesController extends BaseController implements Initializable {
    
    @FXML private TextArea dietaryRequirementsArea;
    @FXML private TextArea allergiesArea;
    @FXML private TextArea cuisineTypesArea;
    @FXML private TextField minCaloriesField;
    @FXML private TextField maxCaloriesField;
    @FXML private Button savePreferencesBtn;
    @FXML private Button resetBtn;
    @FXML private Label statusLabel;
    
    private boolean isFirstTimeSetup = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = getCurrentUser();
        
        // Check if this is first time setup
        if (user != null && !user.isProfileCompleted()) {
            isFirstTimeSetup = true;
            if (statusLabel != null) {
                statusLabel.setText("Please complete your profile to get personalized recipes");
                statusLabel.setStyle("-fx-text-fill: #ff5722;");
            }
        }
        
        if (savePreferencesBtn != null) {
            savePreferencesBtn.setOnAction(e -> savePreferences());
        }
        
        if (resetBtn != null) {
            resetBtn.setOnAction(e -> resetPreferences());
        }
        
        // Add tooltips for guidance
        addTooltips();
        
        loadUserPreferences();
    }
    
    private void addTooltips() {
        if (dietaryRequirementsArea != null) {
            Tooltip dietTooltip = new Tooltip("Enter dietary requirements separated by commas.\n" +
                "Examples: Vegetarian, Vegan, Keto, Paleo, Gluten-Free");
            Tooltip.install(dietaryRequirementsArea, dietTooltip);
        }
        
        if (allergiesArea != null) {
            Tooltip allergyTooltip = new Tooltip("Enter allergies separated by commas.\n" +
                "These ingredients will be excluded from recipes.\n" +
                "Examples: Nuts, Dairy, Eggs, Soy, Gluten, Shellfish");
            Tooltip.install(allergiesArea, allergyTooltip);
        }
        
        if (cuisineTypesArea != null) {
            Tooltip cuisineTooltip = new Tooltip("Enter preferred cuisines separated by commas.\n" +
                "Examples: Italian, Mexican, Chinese, Indian, Japanese, Thai");
            Tooltip.install(cuisineTypesArea, cuisineTooltip);
        }
        
        if (minCaloriesField != null) {
            Tooltip minCalTooltip = new Tooltip("Minimum calories per serving for recipes");
            Tooltip.install(minCaloriesField, minCalTooltip);
        }
        
        if (maxCaloriesField != null) {
            Tooltip maxCalTooltip = new Tooltip("Maximum calories per serving for recipes");
            Tooltip.install(maxCaloriesField, maxCalTooltip);
        }
    }
    
    private void loadUserPreferences() {
        User user = getCurrentUser();
        if (user == null) return;
        
        // Load existing preferences
        if (!user.getDiets().isEmpty()) {
            dietaryRequirementsArea.setText(String.join(", ", user.getDiets()));
        } else {
            dietaryRequirementsArea.setPromptText("e.g., Vegetarian, Vegan, Keto, Paleo");
        }
        
        if (!user.getAllergies().isEmpty()) {
            allergiesArea.setText(String.join(", ", user.getAllergies()));
        } else {
            allergiesArea.setPromptText("e.g., Nuts, Dairy, Eggs, Soy");
        }
        
        if (!user.getCuisines().isEmpty()) {
            cuisineTypesArea.setText(String.join(", ", user.getCuisines()));
        } else {
            cuisineTypesArea.setPromptText("e.g., Italian, Mexican, Chinese, Indian");
        }
        
        minCaloriesField.setText(String.valueOf(user.getMinCalories()));
        maxCaloriesField.setText(String.valueOf(user.getMaxCalories()));
    }
    
    private void savePreferences() {
        User user = getCurrentUser();
        if (user == null) return;
        
        // If first time setup, check if they're skipping
        if (isFirstTimeSetup) {
            String diets = dietaryRequirementsArea.getText().trim();
            String allergies = allergiesArea.getText().trim();
            String cuisines = cuisineTypesArea.getText().trim();
            
            if (diets.isEmpty() && allergies.isEmpty() && cuisines.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Skip Preferences?");
                confirm.setHeaderText("No preferences entered");
                confirm.setContentText("You can get better recipe recommendations by setting your preferences.\n\n" +
                                      "Skip for now and use default settings?");
                
                ButtonType skipBtn = new ButtonType("Skip");
                ButtonType fillBtn = new ButtonType("Fill Preferences");
                confirm.getButtonTypes().setAll(skipBtn, fillBtn);
                
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.orElse(fillBtn) == fillBtn) {
                    return;
                }
            }
        }
        
        try {
            // Clear and save dietary requirements
            user.getDiets().clear();
            String dietsText = dietaryRequirementsArea.getText().trim();
            if (!dietsText.isEmpty()) {
                String[] diets = dietsText.split(",");
                for (String diet : diets) {
                    String trimmedDiet = diet.trim();
                    if (!trimmedDiet.isEmpty()) {
                        user.addDiet(trimmedDiet);
                    }
                }
            }
            
            // Clear and save allergies
            user.getAllergies().clear();
            String allergiesText = allergiesArea.getText().trim();
            if (!allergiesText.isEmpty()) {
                String[] allergies = allergiesText.split(",");
                for (String allergy : allergies) {
                    String trimmedAllergy = allergy.trim();
                    if (!trimmedAllergy.isEmpty()) {
                        user.addAllergy(trimmedAllergy);
                    }
                }
            }
            
            // Clear and save cuisine types
            user.getCuisines().clear();
            String cuisinesText = cuisineTypesArea.getText().trim();
            if (!cuisinesText.isEmpty()) {
                String[] cuisines = cuisinesText.split(",");
                for (String cuisine : cuisines) {
                    String trimmedCuisine = cuisine.trim();
                    if (!trimmedCuisine.isEmpty()) {
                        user.addCuisine(trimmedCuisine);
                    }
                }
            }
            
            // Validate and save calorie preferences
            int minCal = Integer.parseInt(minCaloriesField.getText().trim());
            int maxCal = Integer.parseInt(maxCaloriesField.getText().trim());
            
            if (minCal <= 0 || maxCal <= 0) {
                showErrorAlert("Calories must be positive numbers");
                return;
            }
            
            if (minCal > maxCal) {
                showErrorAlert("Minimum calories cannot exceed maximum calories");
                return;
            }
            
            if (minCal < 50) {
                showWarningAlert("Minimum calories seems too low. Setting to 50.");
                minCal = 50;
                minCaloriesField.setText("50");
            }
            
            if (maxCal > 5000) {
                showWarningAlert("Maximum calories seems too high. Setting to 5000.");
                maxCal = 5000;
                maxCaloriesField.setText("5000");
            }
            
            user.setMinCalories(minCal);
            user.setMaxCalories(maxCal);
            
            // Mark profile as completed
            user.setProfileCompleted(true);
            
            // Build detailed success message with CURRENT VALUES
            StringBuilder summary = new StringBuilder("✅ Preferences Updated Successfully!\n");
            summary.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            summary.append("📋 Current Preferences:\n\n");
            
            if (!user.getDiets().isEmpty()) {
                summary.append("🥗 Diet: ").append(String.join(", ", user.getDiets())).append("\n\n");
            } else {
                summary.append("🥗 Diet: None specified\n\n");
            }
            
            if (!user.getAllergies().isEmpty()) {
                summary.append("⚠️ Allergies: ").append(String.join(", ", user.getAllergies())).append("\n\n");
            } else {
                summary.append("⚠️ Allergies: None specified\n\n");
            }
            
            if (!user.getCuisines().isEmpty()) {
                summary.append("🍽️ Cuisines: ").append(String.join(", ", user.getCuisines())).append("\n\n");
            } else {
                summary.append("🍽️ Cuisines: All cuisines\n\n");
            }
            
            summary.append("🔥 Calorie Range: ").append(minCal).append(" - ").append(maxCal).append(" calories\n");
            
            // Create custom alert with detailed information
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Preferences Saved");
            successAlert.setHeaderText("Your preferences have been updated!");
            successAlert.setContentText(summary.toString());
            successAlert.showAndWait();
            
            // Update status label
            if (statusLabel != null) {
                statusLabel.setText("Preferences saved successfully!");
                statusLabel.setStyle("-fx-text-fill: #4caf50;");
            }
            
            // Navigate to dashboard if first time setup
            if (isFirstTimeSetup) {
                navigateToDashboard();
            }
            
        } catch (NumberFormatException e) {
            showErrorAlert("Please enter valid numbers for calorie values");
        } catch (Exception e) {
            showErrorAlert("Error saving preferences: " + e.getMessage());
        }
    }
    
    private void resetPreferences() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Preferences");
        confirm.setHeaderText("Reset all preferences to default?");
        confirm.setContentText("This will clear all your dietary requirements, allergies, and cuisine preferences.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            User user = getCurrentUser();
            if (user != null) {
                user.getDiets().clear();
                user.getAllergies().clear();
                user.getCuisines().clear();
                user.setMinCalories(50);
                user.setMaxCalories(5000);
                
                // Reload the form
                dietaryRequirementsArea.clear();
                allergiesArea.clear();
                cuisineTypesArea.clear();
                minCaloriesField.setText("50");
                maxCaloriesField.setText("5000");
                
                showSuccessAlert("Preferences reset to default!");
                
                if (statusLabel != null) {
                    statusLabel.setText("Preferences reset to default");
                    statusLabel.setStyle("-fx-text-fill: #ff9800;");
                }
            }
        }
    }
    
    private void navigateToDashboard() {
        Stage currentStage = getCurrentStage(savePreferencesBtn);
        currentStage.close();
        navigateToPage("/views/dashboard.fxml", null, "ZeroWaste - Dashboard");
    }
}