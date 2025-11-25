package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import models.Entities.User;
import java.net.URL;
import java.util.*;

public class PreferencesController extends BaseController implements Initializable {
    
    // FlowPanes for checkboxes
    @FXML private FlowPane dietsFlowPane;
    @FXML private FlowPane intolerancesFlowPane;
    @FXML private FlowPane cuisinesFlowPane;
    
    // Calorie fields
    @FXML private TextField minCaloriesField;
    @FXML private TextField maxCaloriesField;
    
    // Buttons and labels
    @FXML private Button savePreferencesBtn;
    @FXML private Button resetBtn;
    @FXML private Label statusLabel;
    
    // Maps to store checkbox references
    private final Map<String, CheckBox> dietCheckboxes = new LinkedHashMap<>();
    private final Map<String, CheckBox> intoleranceCheckboxes = new LinkedHashMap<>();
    private final Map<String, CheckBox> cuisineCheckboxes = new LinkedHashMap<>();
    
    private boolean isFirstTimeSetup = false;
    
    // Spoonacular API supported values
    // Source: https://spoonacular.com/food-api/docs#Diets
    private static final String[] DIETS = {
        "Gluten Free", "Ketogenic", "Vegetarian", "Lacto-Vegetarian", 
        "Ovo-Vegetarian", "Vegan", "Pescetarian", "Paleo", 
        "Primal", "Low FODMAP", "Whole30"
    };
    
    // Source: https://spoonacular.com/food-api/docs#Intolerances
    private static final String[] INTOLERANCES = {
        "Dairy", "Egg", "Gluten", "Grain", "Peanut", "Seafood", 
        "Sesame", "Shellfish", "Soy", "Sulfite", "Tree Nut", "Wheat"
    };
    
    // Source: https://spoonacular.com/food-api/docs#Cuisines
    private static final String[] CUISINES = {
        "African", "Asian", "American", "British", "Cajun", "Caribbean",
        "Chinese", "Eastern European", "European", "French", "German", 
        "Greek", "Indian", "Irish", "Italian", "Japanese", "Jewish", 
        "Korean", "Latin American", "Mediterranean", "Mexican", 
        "Middle Eastern", "Nordic", "Southern", "Spanish", "Thai", "Vietnamese"
    };
    
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
        
        // Create checkboxes dynamically
        createDietCheckboxes();
        createIntoleranceCheckboxes();
        createCuisineCheckboxes();
        
        // Setup button handlers
        if (savePreferencesBtn != null) {
            savePreferencesBtn.setOnAction(e -> savePreferences());
        }
        
        if (resetBtn != null) {
            resetBtn.setOnAction(e -> resetPreferences());
        }
        
        // Add calorie validation listeners
        addCalorieValidationListeners();
        
        // Add tooltips
        addTooltips();
        
        // Load existing preferences
        loadUserPreferences();
    }
    
    private void createDietCheckboxes() {
        if (dietsFlowPane == null) return;
        
        for (String diet : DIETS) {
            CheckBox cb = createStyledCheckBox(diet);
            dietCheckboxes.put(diet.toLowerCase(), cb);
            dietsFlowPane.getChildren().add(cb);
        }
    }
    
    private void createIntoleranceCheckboxes() {
        if (intolerancesFlowPane == null) return;
        
        for (String intolerance : INTOLERANCES) {
            CheckBox cb = createStyledCheckBox(intolerance);
            intoleranceCheckboxes.put(intolerance.toLowerCase(), cb);
            intolerancesFlowPane.getChildren().add(cb);
        }
    }
    
    private void createCuisineCheckboxes() {
        if (cuisinesFlowPane == null) return;
        
        for (String cuisine : CUISINES) {
            CheckBox cb = createStyledCheckBox(cuisine);
            cuisineCheckboxes.put(cuisine.toLowerCase(), cb);
            cuisinesFlowPane.getChildren().add(cb);
        }
    }
    
    private CheckBox createStyledCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-font-size: 12px;");
        cb.setPadding(new Insets(2, 8, 2, 0));
        return cb;
    }
    
    private void addCalorieValidationListeners() {
        if (minCaloriesField != null) {
            minCaloriesField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    minCaloriesField.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });
        }
        
        if (maxCaloriesField != null) {
            maxCaloriesField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    maxCaloriesField.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });
        }
    }
    
    private void addTooltips() {
        if (minCaloriesField != null) {
            Tooltip.install(minCaloriesField, new Tooltip("Minimum calories per serving (e.g., 50)"));
        }
        if (maxCaloriesField != null) {
            Tooltip.install(maxCaloriesField, new Tooltip("Maximum calories per serving (e.g., 800)"));
        }
    }
    
    private void loadUserPreferences() {
        User user = getCurrentUser();
        if (user == null) return;
        
        // Load diets - check matching checkboxes
        for (String diet : user.getDiets()) {
            CheckBox cb = dietCheckboxes.get(diet.toLowerCase());
            if (cb != null) {
                cb.setSelected(true);
            }
        }
        
        // Load allergies/intolerances
        for (String allergy : user.getAllergies()) {
            CheckBox cb = intoleranceCheckboxes.get(allergy.toLowerCase());
            if (cb != null) {
                cb.setSelected(true);
            }
        }
        
        // Load cuisines
        for (String cuisine : user.getCuisines()) {
            CheckBox cb = cuisineCheckboxes.get(cuisine.toLowerCase());
            if (cb != null) {
                cb.setSelected(true);
            }
        }
        
        // Load calorie values
        if (minCaloriesField != null) {
            minCaloriesField.setText(String.valueOf(user.getMinCalories()));
        }
        if (maxCaloriesField != null) {
            maxCaloriesField.setText(String.valueOf(user.getMaxCalories()));
        }
    }
    
    private void clearAllUserPreferences(User user) {
        // Clear all diets
        new HashSet<>(user.getDiets()).forEach(user::removeDiet);
        
        // Clear all allergies
        new HashSet<>(user.getAllergies()).forEach(user::removeAllergy);
        
        // Clear all cuisines
        new HashSet<>(user.getCuisines()).forEach(user::removeCuisine);
    }
    
    private Set<String> getSelectedDiets() {
        Set<String> selected = new LinkedHashSet<>();
        for (Map.Entry<String, CheckBox> entry : dietCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getValue().getText());
            }
        }
        return selected;
    }
    
    private Set<String> getSelectedIntolerances() {
        Set<String> selected = new LinkedHashSet<>();
        for (Map.Entry<String, CheckBox> entry : intoleranceCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getValue().getText());
            }
        }
        return selected;
    }
    
    private Set<String> getSelectedCuisines() {
        Set<String> selected = new LinkedHashSet<>();
        for (Map.Entry<String, CheckBox> entry : cuisineCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getValue().getText());
            }
        }
        return selected;
    }
    
    private boolean validateCalorieInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            showErrorAlert(fieldName + " cannot be empty.\nPlease enter a valid number.");
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
            showErrorAlert("Invalid input for " + fieldName + ".\nPlease enter only numbers.");
            return false;
        }
    }
    
    private void savePreferences() {
        User user = getCurrentUser();
        if (user == null) return;
        
        String minCalText = minCaloriesField.getText().trim();
        String maxCalText = maxCaloriesField.getText().trim();
        
        // Validate calorie fields
        if (!validateCalorieInput(minCalText, "Minimum Calories")) {
            minCaloriesField.requestFocus();
            return;
        }
        
        if (!validateCalorieInput(maxCalText, "Maximum Calories")) {
            maxCaloriesField.requestFocus();
            return;
        }
        
        // Get selected values
        Set<String> selectedDiets = getSelectedDiets();
        Set<String> selectedIntolerances = getSelectedIntolerances();
        Set<String> selectedCuisines = getSelectedCuisines();
        
        // First time setup - confirm if skipping preferences
        if (isFirstTimeSetup && selectedDiets.isEmpty() && 
            selectedIntolerances.isEmpty() && selectedCuisines.isEmpty()) {
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Skip Preferences?");
            confirm.setHeaderText("No preferences selected");
            confirm.setContentText("You can get better recipe recommendations by setting your preferences.\n\n" +
                                  "Skip for now and use default settings?");
            
            ButtonType skipBtn = new ButtonType("Skip");
            ButtonType fillBtn = new ButtonType("Select Preferences");
            confirm.getButtonTypes().setAll(skipBtn, fillBtn);
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.orElse(fillBtn) == fillBtn) {
                return;
            }
        }
        
        try {
            int minCal = Integer.parseInt(minCalText);
            int maxCal = Integer.parseInt(maxCalText);
            
            // Validate calorie range
            if (minCal > maxCal) {
                showErrorAlert("Minimum calories cannot exceed maximum calories.\n\n" +
                             "Please ensure minimum is less than or equal to maximum.");
                minCaloriesField.requestFocus();
                return;
            }
            
            // Adjust extreme values
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
            
            // Clear existing preferences
            clearAllUserPreferences(user);
            
            // Add selected diets
            for (String diet : selectedDiets) {
                user.addDiet(diet);
            }
            
            // Add selected intolerances as allergies
            for (String intolerance : selectedIntolerances) {
                user.addAllergy(intolerance);
            }
            
            // Add selected cuisines
            for (String cuisine : selectedCuisines) {
                user.addCuisine(cuisine);
            }
            
            // Save calorie preferences
            user.setMinCalories(minCal);
            user.setMaxCalories(maxCal);
            
            // Mark profile as completed
            user.setProfileCompleted(true);
            
            // Build success message
            StringBuilder summary = new StringBuilder();
            summary.append("Your Current Preferences:\n\n");
            
            HashSet<String> currentDiets = user.getDiets();
            HashSet<String> currentAllergies = user.getAllergies();
            HashSet<String> currentCuisines = user.getCuisines();
            
            if (!currentDiets.isEmpty()) {
                summary.append("Diets: ").append(String.join(", ", currentDiets)).append("\n\n");
            } else {
                summary.append("Diets: None specified\n\n");
            }
            
            if (!currentAllergies.isEmpty()) {
                summary.append("Intolerances: ").append(String.join(", ", currentAllergies)).append("\n\n");
            } else {
                summary.append("Intolerances: None specified\n\n");
            }
            
            if (!currentCuisines.isEmpty()) {
                summary.append("Cuisines: ").append(String.join(", ", currentCuisines)).append("\n\n");
            } else {
                summary.append("Cuisines: All cuisines\n\n");
            }
            
            summary.append("Calorie Range: ").append(minCal).append(" - ").append(maxCal).append(" per serving");
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Preferences Saved");
            successAlert.setHeaderText("Your preferences have been updated!");
            successAlert.setContentText(summary.toString());
            successAlert.showAndWait();
            
            if (statusLabel != null) {
                statusLabel.setText("✓ Preferences saved successfully!");
                statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
            }
            
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
        confirm.setContentText("This will clear all your dietary requirements, intolerances, and cuisine preferences.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            User user = getCurrentUser();
            if (user != null) {
                clearAllUserPreferences(user);
                user.setMinCalories(50);
                user.setMaxCalories(5000);
                
                // Uncheck all checkboxes
                dietCheckboxes.values().forEach(cb -> cb.setSelected(false));
                intoleranceCheckboxes.values().forEach(cb -> cb.setSelected(false));
                cuisineCheckboxes.values().forEach(cb -> cb.setSelected(false));
                
                // Reset calorie fields
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