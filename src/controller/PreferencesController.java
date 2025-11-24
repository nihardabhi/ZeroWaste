package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Entities.User;
import java.net.URL;
import java.util.HashSet;
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
        
        // Add real-time validation listeners
        addInputValidationListeners();
        
        // Add tooltips for guidance
        addTooltips();
        
        loadUserPreferences();
    }
    
    private void addInputValidationListeners() {
        // Add listener for calorie fields to allow only numbers
        if (minCaloriesField != null) {
            minCaloriesField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    minCaloriesField.setText(newValue.replaceAll("[^\\d]", ""));
                    showWarningAlert("Please enter only numbers for minimum calories");
                }
            });
        }
        
        if (maxCaloriesField != null) {
            maxCaloriesField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    maxCaloriesField.setText(newValue.replaceAll("[^\\d]", ""));
                    showWarningAlert("Please enter only numbers for maximum calories");
                }
            });
        }
    }
    
    private void addTooltips() {
        if (dietaryRequirementsArea != null) {
            Tooltip dietTooltip = new Tooltip("Enter dietary requirements separated by commas.\n" +
                "Examples: Vegetarian, Vegan, Keto, Paleo, Gluten-Free\n" +
                "Note: Numbers are not allowed");
            Tooltip.install(dietaryRequirementsArea, dietTooltip);
        }
        
        if (allergiesArea != null) {
            Tooltip allergyTooltip = new Tooltip("Enter allergies separated by commas.\n" +
                "These ingredients will be excluded from recipes.\n" +
                "Examples: Nuts, Dairy, Eggs, Soy, Gluten, Shellfish\n" +
                "Note: Numbers are not allowed");
            Tooltip.install(allergiesArea, allergyTooltip);
        }
        
        if (cuisineTypesArea != null) {
            Tooltip cuisineTooltip = new Tooltip("Enter preferred cuisines separated by commas.\n" +
                "Examples: Italian, Mexican, Chinese, Indian, Japanese, Thai\n" +
                "Note: Numbers are not allowed");
            Tooltip.install(cuisineTypesArea, cuisineTooltip);
        }
        
        if (minCaloriesField != null) {
            Tooltip minCalTooltip = new Tooltip("Minimum calories per serving for recipes\n" +
                "Only numbers allowed");
            Tooltip.install(minCaloriesField, minCalTooltip);
        }
        
        if (maxCaloriesField != null) {
            Tooltip maxCalTooltip = new Tooltip("Maximum calories per serving for recipes\n" +
                "Only numbers allowed");
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
    
    private void clearAllUserPreferences(User user) {
        // Clear all diets
        HashSet<String> dietsToRemove = new HashSet<>(user.getDiets());
        for (String diet : dietsToRemove) {
            user.removeDiet(diet);
        }
        
        // Clear all allergies
        HashSet<String> allergiesToRemove = new HashSet<>(user.getAllergies());
        for (String allergy : allergiesToRemove) {
            user.removeAllergy(allergy);
        }
        
        // Clear all cuisines
        HashSet<String> cuisinesToRemove = new HashSet<>(user.getCuisines());
        for (String cuisine : cuisinesToRemove) {
            user.removeCuisine(cuisine);
        }
    }
    
    // Helper method to check if a string contains only letters and allowed characters
    private boolean isValidTextInput(String input) {
        // Allow letters, spaces, hyphens, and apostrophes (for names like "Gluten-Free" or "McDonald's")
        return input.matches("[a-zA-Z\\s\\-']+");
    }
    
    // Helper method to validate all items in a comma-separated list
    private boolean validateTextAreaInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return true; // Empty is valid (optional fields)
        }
        
        String[] items = input.split(",");
        for (String item : items) {
            String trimmedItem = item.trim();
            if (!trimmedItem.isEmpty() && !isValidTextInput(trimmedItem)) {
                showErrorAlert("Invalid input in " + fieldName + ": '" + trimmedItem + "'\n\n" +
                             "Please enter only text without numbers.\n" +
                             "Valid examples: Vegetarian, Gluten-Free, Italian");
                return false;
            }
        }
        return true;
    }
    
    // Helper method to validate calorie fields
    private boolean validateCalorieInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            showErrorAlert(fieldName + " cannot be empty.\n" +
                         "Please enter a valid number.");
            return false;
        }
        
        try {
            int value = Integer.parseInt(input.trim());
            if (value <= 0) {
                showErrorAlert(fieldName + " must be a positive number.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid input for " + fieldName + ".\n\n" +
                         "Please enter only numbers (no letters or special characters).\n" +
                         "Example: 500");
            return false;
        }
    }
    
    private void savePreferences() {
        User user = getCurrentUser();
        if (user == null) return;
        
        // Get input values
        String dietsText = dietaryRequirementsArea.getText().trim();
        String allergiesText = allergiesArea.getText().trim();
        String cuisinesText = cuisineTypesArea.getText().trim();
        String minCalText = minCaloriesField.getText().trim();
        String maxCalText = maxCaloriesField.getText().trim();
        
        // Validate text areas (no numbers allowed)
        if (!validateTextAreaInput(dietsText, "Dietary Requirements")) {
            dietaryRequirementsArea.requestFocus();
            return;
        }
        
        if (!validateTextAreaInput(allergiesText, "Allergies")) {
            allergiesArea.requestFocus();
            return;
        }
        
        if (!validateTextAreaInput(cuisinesText, "Cuisine Types")) {
            cuisineTypesArea.requestFocus();
            return;
        }
        
        // Validate calorie fields (only numbers allowed)
        if (!validateCalorieInput(minCalText, "Minimum Calories")) {
            minCaloriesField.requestFocus();
            return;
        }
        
        if (!validateCalorieInput(maxCalText, "Maximum Calories")) {
            maxCaloriesField.requestFocus();
            return;
        }
        
        // If first time setup, check if they're skipping
        if (isFirstTimeSetup) {
            if (dietsText.isEmpty() && allergiesText.isEmpty() && cuisinesText.isEmpty()) {
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
            // Parse and validate calorie values
            int minCal = Integer.parseInt(minCalText);
            int maxCal = Integer.parseInt(maxCalText);
            
            // Additional calorie validation
            if (minCal > maxCal) {
                showErrorAlert("Minimum calories cannot exceed maximum calories.\n\n" +
                             "Please ensure minimum is less than or equal to maximum.");
                minCaloriesField.requestFocus();
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
            
            // IMPORTANT: Clear ALL existing preferences first
            clearAllUserPreferences(user);
            
            // Now add the new dietary requirements
            if (!dietsText.isEmpty()) {
                String[] diets = dietsText.split(",");
                for (String diet : diets) {
                    String trimmedDiet = diet.trim();
                    if (!trimmedDiet.isEmpty()) {
                        user.addDiet(trimmedDiet);
                    }
                }
            }
            
            // Add new allergies
            if (!allergiesText.isEmpty()) {
                String[] allergies = allergiesText.split(",");
                for (String allergy : allergies) {
                    String trimmedAllergy = allergy.trim();
                    if (!trimmedAllergy.isEmpty()) {
                        user.addAllergy(trimmedAllergy);
                    }
                }
            }
            
            // Add new cuisine types
            if (!cuisinesText.isEmpty()) {
                String[] cuisines = cuisinesText.split(",");
                for (String cuisine : cuisines) {
                    String trimmedCuisine = cuisine.trim();
                    if (!trimmedCuisine.isEmpty()) {
                        user.addCuisine(trimmedCuisine);
                    }
                }
            }
            
            // Save calorie preferences
            user.setMinCalories(minCal);
            user.setMaxCalories(maxCal);
            
            // Mark profile as completed
            user.setProfileCompleted(true);
            
            // Build success message with ONLY CURRENT VALUES
            StringBuilder summary = new StringBuilder("Preferences Updated Successfully!\n");
            summary.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            summary.append("Your Current Preferences:\n\n");
            
            // Get fresh copies of the sets to ensure we show current data
            HashSet<String> currentDiets = user.getDiets();
            HashSet<String> currentAllergies = user.getAllergies();
            HashSet<String> currentCuisines = user.getCuisines();
            
            if (!currentDiets.isEmpty()) {
                summary.append("Diet: ").append(String.join(", ", currentDiets)).append("\n\n");
            } else {
                summary.append("Diet: None specified\n\n");
            }
            
            if (!currentAllergies.isEmpty()) {
                summary.append("Allergies: ").append(String.join(", ", currentAllergies)).append("\n\n");
            } else {
                summary.append("Allergies: None specified\n\n");
            }
            
            if (!currentCuisines.isEmpty()) {
                summary.append("Cuisines: ").append(String.join(", ", currentCuisines)).append("\n\n");
            } else {
                summary.append("Cuisines: All cuisines\n\n");
            }
            
            summary.append("Calorie Range: ").append(minCal).append(" - ").append(maxCal).append(" calories per serving");
            
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
                // Properly clear all preferences
                clearAllUserPreferences(user);
                
                // Set default calorie values
                user.setMinCalories(50);
                user.setMaxCalories(5000);
                
                // Clear and reload the form
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