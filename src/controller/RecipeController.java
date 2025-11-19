package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import models.Entities.FoodItem;
import models.Entities.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeController extends BaseController implements Initializable {
    
    @FXML private VBox recipeContainer;
    @FXML private CheckBox useExpiringCheckBox;
    @FXML private Button searchRecipesBtn;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label activeFiltersLabel;
    
    // Spoonacular API credentials
    private static final String API_KEY = "b36a3748a339421caa5e5e84c3346b53";
    private static final String API_URL = "https://api.spoonacular.com/recipes/complexSearch";
    private static final String RECIPE_INFO_URL = "https://api.spoonacular.com/recipes/";
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (searchRecipesBtn != null) {
            searchRecipesBtn.setOnAction(e -> searchRecipes());
        }
        
        if (useExpiringCheckBox != null) {
            useExpiringCheckBox.setSelected(true);
            
            // Add listener to automatically refresh when checkbox is toggled
            useExpiringCheckBox.setOnAction(e -> {
                String message = useExpiringCheckBox.isSelected() ? 
                    "🔍 Searching recipes using expiring items only..." : 
                    "🔍 Searching recipes using all inventory items...";
                
                // Show status message
                Platform.runLater(() -> {
                    if (recipeContainer != null && recipeContainer.getChildren().size() > 0) {
                        Label statusLabel = new Label(message);
                        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                        recipeContainer.getChildren().add(0, statusLabel);
                    }
                });
                
                searchRecipes();
            });
        }
        
        checkForEmptyPantry();
    }
    
    private void checkForEmptyPantry() {
        User user = getCurrentUser();
        if (user == null || user.getPantry().getTotalItemCount() == 0) {
            showEmptyPantryMessage();
        } else {
            searchRecipes();
        }
    }
    
    private void showEmptyPantryMessage() {
        if (recipeContainer == null) return;
        
        recipeContainer.getChildren().clear();
        
        VBox emptyMessage = new VBox(10);
        emptyMessage.setPadding(new Insets(20));
        emptyMessage.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label title = new Label("📦 Your Pantry is Empty!");
        title.setFont(new Font("System Bold", 18));
        
        Label message = new Label("Add items to your inventory to get personalized recipe recommendations from Spoonacular.");
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 14px;");
        
        Label instruction = new Label("Steps:\n1. Go to Inventory\n2. Add your food items\n3. Come back here for amazing recipes!");
        instruction.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        emptyMessage.getChildren().addAll(title, message, instruction);
        recipeContainer.getChildren().add(emptyMessage);
    }
    
    private void searchRecipes() {
        User user = getCurrentUser();
        if (user == null || user.getPantry().getTotalItemCount() == 0) {
            showEmptyPantryMessage();
            return;
        }
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        
        recipeContainer.getChildren().clear();
        displayActiveFilters();
        
        // Show search status based on checkbox
        String searchStatus = useExpiringCheckBox.isSelected() ? 
            "🔍 Searching for recipes using items expiring within 3 days..." :
            "🔍 Searching for recipes using all items in your pantry...";
        
        Label searchingLabel = new Label(searchStatus);
        searchingLabel.setFont(new Font(14));
        searchingLabel.setStyle("-fx-text-fill: #2196F3;");
        recipeContainer.getChildren().add(searchingLabel);
        
        new Thread(() -> {
            try {
                String apiResponse = callSpoonacularAPI();
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    parseAndDisplayRecipes(apiResponse);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    recipeContainer.getChildren().clear();
                    
                    VBox errorBox = new VBox(10);
                    errorBox.setPadding(new Insets(20));
                    errorBox.setStyle("-fx-background-color: #ffebee; -fx-border-radius: 5;");
                    
                    Label errorLabel = new Label("❌ " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    errorLabel.setWrapText(true);
                    
                    // If error is about no expiring items, suggest unchecking the box
                    if (e.getMessage().contains("No items expiring")) {
                        Label suggestionLabel = new Label("💡 Tip: Uncheck 'Prioritize expiring items' to search with all your inventory.");
                        suggestionLabel.setStyle("-fx-text-fill: #666;");
                        errorBox.getChildren().add(suggestionLabel);
                    }
                    
                    Button retryBtn = new Button("🔄 Retry Search");
                    retryBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    retryBtn.setOnAction(ev -> searchRecipes());
                    
                    errorBox.getChildren().addAll(errorLabel, retryBtn);
                    recipeContainer.getChildren().add(errorBox);
                    
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void displayActiveFilters() {
        User user = getCurrentUser();
        StringBuilder filters = new StringBuilder();
        
        // Show what items are being used
        if (useExpiringCheckBox != null && useExpiringCheckBox.isSelected()) {
            List<FoodItem> expiringItems = user.getPantry().getExpiringWithinDays(3);
            filters.append("🥘 Using: ");
            if (expiringItems.isEmpty()) {
                filters.append("No expiring items | ");
            } else {
                filters.append(expiringItems.size()).append(" expiring item(s) | ");
            }
        } else {
            filters.append("🥘 Using: All ").append(user.getPantry().getTotalItemCount()).append(" pantry items | ");
        }
        
        if (!user.getCuisines().isEmpty()) {
            filters.append("🍽️ Cuisines: ").append(String.join(", ", user.getCuisines())).append(" | ");
        }
        
        if (!user.getDiets().isEmpty()) {
            filters.append("🥗 Diet: ").append(String.join(", ", user.getDiets())).append(" | ");
        }
        
        if (!user.getAllergies().isEmpty()) {
            filters.append("🚫 Excluding: ").append(String.join(", ", user.getAllergies())).append(" | ");
        }
        
        filters.append("🔥 Calories: ").append(user.getMinCalories()).append("-").append(user.getMaxCalories());
        
        if (activeFiltersLabel != null) {
            activeFiltersLabel.setText(filters.toString());
            activeFiltersLabel.setWrapText(true);
        }
    }
    
    private String callSpoonacularAPI() throws Exception {
        User user = getCurrentUser();
        
        StringBuilder urlBuilder = new StringBuilder(API_URL);
        urlBuilder.append("?apiKey=").append(API_KEY);
        
        // Get items for search based on checkbox state
        List<FoodItem> items;
        if (useExpiringCheckBox != null && useExpiringCheckBox.isSelected()) {
            // CHECKBOX CHECKED: Only use items expiring within 3 days
            items = user.getPantry().getExpiringWithinDays(3);
            
            if (items.isEmpty()) {
                // No expiring items - inform user instead of using all items
                throw new Exception("No items expiring within 3 days found. Uncheck 'Prioritize expiring items' to search with all inventory.");
            }
            
            System.out.println("Using " + items.size() + " expiring items for recipe search");
        } else {
            // CHECKBOX UNCHECKED: Use ALL items in pantry
            items = user.getPantry().getAllItems();
            System.out.println("Using ALL " + items.size() + " items from pantry for recipe search");
        }
        
        // Build ingredients list for API
        if (!items.isEmpty()) {
            StringBuilder ingredientsBuilder = new StringBuilder();
            Set<String> uniqueIngredients = new HashSet<>();
            
            // Add up to 10 unique ingredients (increased from 5 for better results)
            for (FoodItem item : items) {
                String ingredient = item.getFoodName().toLowerCase().trim();
                if (uniqueIngredients.add(ingredient)) {
                    if (ingredientsBuilder.length() > 0) {
                        ingredientsBuilder.append(",");
                    }
                    ingredientsBuilder.append(ingredient);
                }
            }
            
            String ingredients = URLEncoder.encode(ingredientsBuilder.toString(), "UTF-8");
            urlBuilder.append("&includeIngredients=").append(ingredients);
            
            System.out.println("Searching with ingredients: " + ingredientsBuilder.toString());
        }
        
        // Add cuisine preference (only if not too restrictive)
        if (!user.getCuisines().isEmpty() && items.size() > 2) {
            String cuisines = String.join(",", user.getCuisines())
                .toLowerCase().replace(" ", "-");
            urlBuilder.append("&cuisine=").append(URLEncoder.encode(cuisines, "UTF-8"));
        }
        
        // Add dietary preferences
        if (!user.getDiets().isEmpty()) {
            String diet = mapDietToSpoonacular(user.getDiets().iterator().next());
            if (!diet.isEmpty()) {
                urlBuilder.append("&diet=").append(URLEncoder.encode(diet, "UTF-8"));
            }
        }
        
        // Add allergies/intolerances
        if (!user.getAllergies().isEmpty()) {
            StringBuilder intolerances = new StringBuilder();
            for (String allergy : user.getAllergies()) {
                String mapped = mapAllergyToSpoonacular(allergy);
                if (!mapped.isEmpty()) {
                    if (intolerances.length() > 0) intolerances.append(",");
                    intolerances.append(mapped);
                }
            }
            if (intolerances.length() > 0) {
                urlBuilder.append("&intolerances=").append(URLEncoder.encode(intolerances.toString(), "UTF-8"));
            }
        }
        
        // Adjust calorie range (these are PER SERVING, not total)
        int userMinCal = user.getMinCalories();
        int userMaxCal = user.getMaxCalories();
        
        // Only add calorie filter if reasonable
        if (userMinCal < 800 && userMaxCal > userMinCal) {
            int minCal = Math.max(50, userMinCal / 4);
            int maxCal = Math.min(1500, userMaxCal / 2);
            
            urlBuilder.append("&minCalories=").append(minCal);
            urlBuilder.append("&maxCalories=").append(maxCal);
        }
        
        // Add extra information flags
        urlBuilder.append("&addRecipeInformation=true");
        urlBuilder.append("&addRecipeNutrition=true");
        urlBuilder.append("&fillIngredients=true");
        urlBuilder.append("&number=10");
        urlBuilder.append("&sort=min-missing-ingredients");
        
        System.out.println("Spoonacular API URL: " + urlBuilder.toString());
        
        // Make API call
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);
        
        if (responseCode == 402) {
            throw new Exception("Daily API limit reached (150 requests/day). Try again tomorrow.");
        }
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorMsg = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorMsg.append(line);
            }
            errorReader.close();
            throw new Exception("API Error: " + responseCode + " - " + errorMsg.toString());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String responseStr = response.toString();
        System.out.println("API Response (first 500 chars): " + 
            responseStr.substring(0, Math.min(responseStr.length(), 500)));
        
        // If no results, try a simpler search
        if (responseStr.contains("\"totalResults\":0") || responseStr.contains("\"results\":[]")) {
            System.out.println("No results found, trying simpler search...");
            return fallbackSearch(items, user);
        }
        
        return responseStr;
    }
    
    private String fallbackSearch(List<FoodItem> items, User user) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(API_URL);
        urlBuilder.append("?apiKey=").append(API_KEY);
        
        // Simpler search with fewer ingredients
        if (!items.isEmpty()) {
            // Use only first 3 ingredients for fallback
            StringBuilder ingredientsBuilder = new StringBuilder();
            for (int i = 0; i < Math.min(3, items.size()); i++) {
                if (i > 0) ingredientsBuilder.append(",");
                ingredientsBuilder.append(items.get(i).getFoodName().toLowerCase().trim());
            }
            urlBuilder.append("&includeIngredients=").append(URLEncoder.encode(ingredientsBuilder.toString(), "UTF-8"));
            System.out.println("Fallback search with: " + ingredientsBuilder.toString());
        }
        
        // Remove restrictive filters for fallback
        // Only keep allergy restrictions for safety
        if (!user.getAllergies().isEmpty()) {
            String mainAllergy = mapAllergyToSpoonacular(user.getAllergies().iterator().next());
            if (!mainAllergy.isEmpty()) {
                urlBuilder.append("&intolerances=").append(URLEncoder.encode(mainAllergy, "UTF-8"));
            }
        }
        
        urlBuilder.append("&addRecipeInformation=true");
        urlBuilder.append("&addRecipeNutrition=true");
        urlBuilder.append("&number=10");
        
        System.out.println("Fallback URL: " + urlBuilder.toString());
        
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("No recipes found. Try adjusting your preferences or adding more items to your pantry.");
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
    
    private String mapDietToSpoonacular(String diet) {
        diet = diet.toLowerCase().trim();
        
        if (diet.contains("gluten")) return "gluten free";
        if (diet.contains("keto")) return "ketogenic";
        if (diet.contains("vegetarian")) return "vegetarian";
        if (diet.contains("vegan")) return "vegan";
        if (diet.contains("paleo")) return "paleo";
        if (diet.contains("pescetarian") || diet.contains("pescatarian")) return "pescetarian";
        if (diet.contains("low carb")) return "lowcarb";
        if (diet.contains("whole30") || diet.contains("whole 30")) return "whole30";
        if (diet.contains("primal")) return "primal";
        if (diet.contains("lacto")) return "lacto vegetarian";
        if (diet.contains("ovo")) return "ovo vegetarian";
        
        return "";
    }
    
    private String mapAllergyToSpoonacular(String allergy) {
        allergy = allergy.toLowerCase().trim();
        
        if (allergy.contains("dairy") || allergy.contains("milk") || allergy.contains("lactose")) return "dairy";
        if (allergy.contains("egg")) return "egg";
        if (allergy.contains("gluten") || allergy.contains("celiac")) return "gluten";
        if (allergy.contains("grain")) return "grain";
        if (allergy.contains("peanut")) return "peanut";
        if (allergy.contains("seafood") || allergy.contains("fish")) return "seafood";
        if (allergy.contains("sesame")) return "sesame";
        if (allergy.contains("shellfish")) return "shellfish";
        if (allergy.contains("soy") || allergy.contains("soya")) return "soy";
        if (allergy.contains("sulfite")) return "sulfite";
        if (allergy.contains("tree nut") || allergy.contains("nut") || allergy.contains("almond") || 
            allergy.contains("cashew") || allergy.contains("walnut")) return "tree nut";
        if (allergy.contains("wheat")) return "wheat";
        
        return "";
    }
    
    private void parseAndDisplayRecipes(String jsonResponse) {
        try {
            recipeContainer.getChildren().clear();
            
            List<Map<String, String>> recipes = parseSpoonacularResponse(jsonResponse);
            
            if (recipes.isEmpty()) {
                showNoRecipesMessage();
                return;
            }
            
            // Header with checkbox state info
            String headerText = useExpiringCheckBox.isSelected() ? 
                "🍳 Found " + recipes.size() + " Recipes Using Expiring Items" :
                "🍳 Found " + recipes.size() + " Recipes Using All Inventory";
            
            Label headerLabel = new Label(headerText);
            headerLabel.setFont(new Font("System Bold", 18));
            recipeContainer.getChildren().add(headerLabel);
            
            // Show which items are being used
            if (useExpiringCheckBox != null && useExpiringCheckBox.isSelected()) {
                List<FoodItem> expiringItems = getCurrentUser().getPantry().getExpiringWithinDays(3);
                if (!expiringItems.isEmpty()) {
                    HBox expiringBox = new HBox(5);
                    expiringBox.setStyle("-fx-background-color: #ffebee; -fx-padding: 10; -fx-border-radius: 5;");
                    
                    Label iconLabel = new Label("⚠️");
                    Label textLabel = new Label("Using expiring items: ");
                    
                    StringBuilder expText = new StringBuilder();
                    for (int i = 0; i < Math.min(expiringItems.size(), 5); i++) {
                        if (i > 0) expText.append(", ");
                        FoodItem item = expiringItems.get(i);
                        expText.append(item.getFoodName()).append(" (")
                               .append(item.getDaysUntilExpiration()).append(" days)");
                    }
                    if (expiringItems.size() > 5) {
                        expText.append(" and ").append(expiringItems.size() - 5).append(" more");
                    }
                    textLabel.setText(textLabel.getText() + expText.toString());
                    
                    expiringBox.getChildren().addAll(iconLabel, textLabel);
                    recipeContainer.getChildren().add(expiringBox);
                }
            } else {
                // Show total items being used
                HBox totalBox = new HBox(5);
                totalBox.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 10; -fx-border-radius: 5;");
                
                Label iconLabel = new Label("✅");
                int totalItems = getCurrentUser().getPantry().getTotalItemCount();
                Label textLabel = new Label("Using all " + totalItems + " items from your pantry");
                
                totalBox.getChildren().addAll(iconLabel, textLabel);
                recipeContainer.getChildren().add(totalBox);
            }
            
            recipeContainer.getChildren().add(new Separator());
            
            // Recipe cards
            for (Map<String, String> recipe : recipes) {
                VBox recipeCard = createRecipeCard(recipe);
                recipeContainer.getChildren().add(recipeCard);
            }
            
        } catch (Exception e) {
            showErrorAlert("Failed to display recipes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showNoRecipesMessage() {
        VBox noRecipesBox = new VBox(10);
        noRecipesBox.setPadding(new Insets(20));
        noRecipesBox.setStyle("-fx-background-color: #fff3cd; -fx-border-radius: 5;");
        
        Label noRecipesLabel = new Label("No recipes found!");
        noRecipesLabel.setFont(new Font("System Bold", 16));
        
        String suggestions = useExpiringCheckBox.isSelected() ?
            "Try:\n" +
            "• Unchecking 'Prioritize expiring items' to use all inventory\n" +
            "• Adding more items that expire soon\n" +
            "• Adjusting your dietary preferences" :
            "Try:\n" +
            "• Adding more items to your pantry\n" +
            "• Removing some dietary restrictions\n" +
            "• Adjusting your calorie preferences";
        
        Label suggestionLabel = new Label(suggestions);
        suggestionLabel.setStyle("-fx-font-size: 12px;");
        
        Button retryBtn = new Button("🔄 Search Again");
        retryBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        retryBtn.setOnAction(e -> searchRecipes());
        
        // Toggle checkbox button
        Button toggleBtn = new Button(useExpiringCheckBox.isSelected() ? 
            "📦 Use All Items" : "⚠️ Use Only Expiring");
        toggleBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        toggleBtn.setOnAction(e -> {
            useExpiringCheckBox.setSelected(!useExpiringCheckBox.isSelected());
            searchRecipes();
        });
        
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(retryBtn, toggleBtn);
        
        noRecipesBox.getChildren().addAll(noRecipesLabel, suggestionLabel, buttonBox);
        recipeContainer.getChildren().add(noRecipesBox);
    }
    
    private List<Map<String, String>> parseSpoonacularResponse(String json) {
        List<Map<String, String>> recipes = new ArrayList<>();
        
        try {
            System.out.println("Starting to parse JSON response...");
            
            // Find the start of results array
            int resultsIndex = json.indexOf("\"results\"");
            if (resultsIndex == -1) {
                System.out.println("No results field found");
                return recipes;
            }
            
            // Find the opening bracket of the array
            int arrayStart = json.indexOf("[", resultsIndex);
            if (arrayStart == -1) {
                System.out.println("No array bracket found");
                return recipes;
            }
            
            // Find the matching closing bracket
            int arrayEnd = findMatchingBracket(json, arrayStart);
            if (arrayEnd == -1) {
                System.out.println("Could not find matching bracket");
                return recipes;
            }
            
            String resultsArray = json.substring(arrayStart + 1, arrayEnd);
            System.out.println("Results array extracted, length: " + resultsArray.length());
            
            // Split into individual recipe objects
            List<String> recipeBlocks = splitJsonObjects(resultsArray);
            System.out.println("Found " + recipeBlocks.size() + " recipe blocks");
            
            for (String recipeBlock : recipeBlocks) {
                Map<String, String> recipe = new HashMap<>();
                
                // Extract recipe ID
                String id = extractValue(recipeBlock, "\"id\"\\s*:\\s*(\\d+)", 1);
                if (id != null) recipe.put("id", id);
                
                // Extract title
                String title = extractStringValue(recipeBlock, "title");
                if (title != null) recipe.put("name", title);
                
                // Extract ready time
                String readyTime = extractValue(recipeBlock, "\"readyInMinutes\"\\s*:\\s*(\\d+)", 1);
                if (readyTime != null) recipe.put("readyTime", readyTime);
                
                // Extract servings
                String servings = extractValue(recipeBlock, "\"servings\"\\s*:\\s*(\\d+)", 1);
                if (servings != null) recipe.put("servings", servings);
                
                // Extract source URL
                String sourceUrl = extractStringValue(recipeBlock, "sourceUrl");
                if (sourceUrl != null) recipe.put("sourceUrl", sourceUrl);
                
                // Extract image
                String image = extractStringValue(recipeBlock, "image");
                if (image != null) recipe.put("image", image);
                
                // Extract summary
                String summary = extractStringValue(recipeBlock, "summary");
                if (summary != null) {
                    summary = summary.replaceAll("<[^>]+>", "").replaceAll("\\\\", "");
                    if (summary.length() > 200) {
                        summary = summary.substring(0, 197) + "...";
                    }
                    recipe.put("summary", summary);
                }
                
                // Extract nutrition data if present
                if (recipeBlock.contains("\"nutrition\"")) {
                    Pattern nutritionPattern = Pattern.compile("\"nutrition\"\\s*:\\s*\\{[^}]*\"nutrients\"\\s*:\\s*\\[([^\\]]+)\\]");
                    Matcher nutritionMatcher = nutritionPattern.matcher(recipeBlock);
                    if (nutritionMatcher.find()) {
                        String nutrients = nutritionMatcher.group(1);
                        Pattern calPattern = Pattern.compile("\"name\"\\s*:\\s*\"Calories\"[^}]*\"amount\"\\s*:\\s*([\\d.]+)");
                        Matcher calMatcher = calPattern.matcher(nutrients);
                        if (calMatcher.find()) {
                            recipe.put("calories", String.valueOf((int)Double.parseDouble(calMatcher.group(1))));
                        }
                    }
                }
                
                // Extract cuisines
                String cuisines = extractArrayValues(recipeBlock, "cuisines");
                if (cuisines != null) recipe.put("cuisines", cuisines);
                
                // Extract diets
                String diets = extractArrayValues(recipeBlock, "diets");
                if (diets != null) recipe.put("diets", diets);
                
                // Extract health score
                String healthScore = extractValue(recipeBlock, "\"healthScore\"\\s*:\\s*([\\d.]+)", 1);
                if (healthScore != null) recipe.put("healthScore", healthScore);
                
                // Extract ingredient counts if present
                String missedCount = extractValue(recipeBlock, "\"missedIngredientCount\"\\s*:\\s*(\\d+)", 1);
                if (missedCount != null) recipe.put("missingIngredients", missedCount);
                
                String usedCount = extractValue(recipeBlock, "\"usedIngredientCount\"\\s*:\\s*(\\d+)", 1);
                if (usedCount != null) recipe.put("usedIngredients", usedCount);
                
                if (recipe.containsKey("name") && recipe.containsKey("id")) {
                    recipes.add(recipe);
                    System.out.println("Successfully parsed recipe: " + recipe.get("name"));
                }
            }
            
            System.out.println("Total recipes parsed: " + recipes.size());
            
        } catch (Exception e) {
            System.out.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return recipes;
    }
    
    // Helper method to find matching bracket
    private int findMatchingBracket(String json, int start) {
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '[' || c == '{') depth++;
                else if (c == ']' || c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }
    
    // Helper method to split JSON objects
    private List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        objects.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return objects;
    }
    
    // Helper to extract simple values
    private String extractValue(String json, String pattern, int group) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(json);
        return m.find() ? m.group(group) : null;
    }
    
    // Helper to extract string values
    private String extractStringValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    // Helper to extract array values
    private String extractArrayValues(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = p.matcher(json);
        if (m.find()) {
            String values = m.group(1);
            return values.replaceAll("\"", "").trim();
        }
        return null;
    }
    
    private VBox createRecipeCard(Map<String, String> recipe) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                     "-fx-border-radius: 5; -fx-background-radius: 5; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setMaxWidth(700);
        
        // Recipe name
        Label nameLabel = new Label(recipe.getOrDefault("name", "Recipe"));
        nameLabel.setFont(new Font("System Bold", 16));
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // Metadata row
        HBox metaBox = new HBox(15);
        
        // Ready time
        String readyTime = recipe.get("readyTime");
        if (readyTime != null) {
            Label timeLabel = new Label("⏱️ " + readyTime + " minutes");
            timeLabel.setStyle("-fx-font-size: 12px;");
            metaBox.getChildren().add(timeLabel);
        }
        
        // Servings
        String servings = recipe.get("servings");
        if (servings != null) {
            Label servingsLabel = new Label("👥 " + servings + " servings");
            servingsLabel.setStyle("-fx-font-size: 12px;");
            metaBox.getChildren().add(servingsLabel);
        }
        
        // Calories
        String calories = recipe.get("calories");
        if (calories != null) {
            Label calLabel = new Label("🔥 " + calories + " cal/serving");
            User user = getCurrentUser();
            int cal = Integer.parseInt(calories);
            if (cal >= user.getMinCalories()/4 && cal <= user.getMaxCalories()) {
                calLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold; -fx-font-size: 12px;");
            } else {
                calLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
            }
            metaBox.getChildren().add(calLabel);
        }
        
        // Ingredient match info
        HBox ingredientBox = new HBox(10);
        String usedIngredients = recipe.get("usedIngredients");
        String missingIngredients = recipe.get("missingIngredients");
        
        if (usedIngredients != null) {
            Label usedLabel = new Label("✅ " + usedIngredients + " ingredients from pantry");
            usedLabel.setStyle("-fx-text-fill: green; -fx-font-size: 11px; -fx-font-weight: bold;");
            ingredientBox.getChildren().add(usedLabel);
        }
        
        if (missingIngredients != null && !missingIngredients.equals("0")) {
            Label missingLabel = new Label("🛒 " + missingIngredients + " ingredients needed");
            missingLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 11px;");
            ingredientBox.getChildren().add(missingLabel);
        }
        
        // Cuisines and diets
        HBox tagsBox = new HBox(10);
        
        String cuisines = recipe.get("cuisines");
        if (cuisines != null && !cuisines.trim().isEmpty()) {
            for (String cuisine : cuisines.split(",")) {
                if (!cuisine.trim().isEmpty()) {
                    Label cuisineTag = new Label("🍽️ " + cuisine.trim());
                    cuisineTag.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5; " +
                                       "-fx-background-radius: 3; -fx-font-size: 11px;");
                    tagsBox.getChildren().add(cuisineTag);
                }
            }
        }
        
        String diets = recipe.get("diets");
        if (diets != null && !diets.trim().isEmpty()) {
            for (String diet : diets.split(",")) {
                if (!diet.trim().isEmpty()) {
                    Label dietTag = new Label("✅ " + diet.trim());
                    dietTag.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 5; " +
                                    "-fx-background-radius: 3; -fx-font-size: 11px;");
                    tagsBox.getChildren().add(dietTag);
                }
            }
        }
        
        // Summary
        String summary = recipe.get("summary");
        if (summary != null && !summary.isEmpty()) {
            Label summaryLabel = new Label(summary);
            summaryLabel.setWrapText(true);
            summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-padding: 10 0 0 0;");
            card.getChildren().add(summaryLabel);
        }
        
        // Buttons
        HBox buttonBox = new HBox(10);
        
        Button viewBtn = new Button("📖 View Full Recipe");
        viewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 8 16;");
        
        String recipeId = recipe.get("id");
        if (recipeId != null) {
            viewBtn.setOnAction(e -> {
                try {
                    String url = "https://spoonacular.com/recipes/recipe-" + recipeId;
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    // Try source URL as fallback
                    String sourceUrl = recipe.get("sourceUrl");
                    if (sourceUrl != null) {
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(sourceUrl));
                        } catch (Exception ex2) {
                            showErrorAlert("Cannot open recipe link");
                        }
                    }
                }
            });
        }
        
        buttonBox.getChildren().add(viewBtn);
        
        // Add all components
        card.getChildren().addAll(
            nameLabel,
            metaBox
        );
        
        if (!ingredientBox.getChildren().isEmpty()) {
            card.getChildren().add(ingredientBox);
        }
        
        if (!tagsBox.getChildren().isEmpty()) {
            card.getChildren().add(tagsBox);
        }
        
        card.getChildren().addAll(
            new Separator(),
            buttonBox
        );
        
        return card;
    }
}