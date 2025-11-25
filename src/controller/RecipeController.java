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
    
    // Valid Spoonacular API values
    private static final Set<String> VALID_DIETS = Set.of(
        "gluten free", "ketogenic", "vegetarian", "lacto-vegetarian",
        "ovo-vegetarian", "vegan", "pescetarian", "paleo",
        "primal", "low fodmap", "whole30"
    );
    
    private static final Set<String> VALID_INTOLERANCES = Set.of(
        "dairy", "egg", "gluten", "grain", "peanut", "seafood",
        "sesame", "shellfish", "soy", "sulfite", "tree nut", "wheat"
    );
    
    private static final Set<String> VALID_CUISINES = Set.of(
        "african", "asian", "american", "british", "cajun", "caribbean",
        "chinese", "eastern european", "european", "french", "german",
        "greek", "indian", "irish", "italian", "japanese", "jewish",
        "korean", "latin american", "mediterranean", "mexican",
        "middle eastern", "nordic", "southern", "spanish", "thai", "vietnamese"
    );
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (searchRecipesBtn != null) {
            searchRecipesBtn.setOnAction(e -> searchRecipes());
        }
        
        if (useExpiringCheckBox != null) {
            useExpiringCheckBox.setSelected(true);
            useExpiringCheckBox.setOnAction(e -> searchRecipes());
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
        
        Label message = new Label("Add items to your inventory to get personalized recipe recommendations.");
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
        
        boolean useOnlyExpiring = useExpiringCheckBox != null && useExpiringCheckBox.isSelected();
        
        String searchStatus = useOnlyExpiring ? 
            "🔍 Searching for recipes using items expiring within 3 days..." :
            "🔍 Searching for recipes using ALL items in your pantry...";
        
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
                    showErrorMessage(e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void showErrorMessage(String errorMsg) {
        recipeContainer.getChildren().clear();
        
        VBox errorBox = new VBox(10);
        errorBox.setPadding(new Insets(20));
        errorBox.setStyle("-fx-background-color: #ffebee; -fx-border-radius: 5;");
        
        Label errorLabel = new Label("❌ " + errorMsg);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        
        errorBox.getChildren().add(errorLabel);
        
        if (errorMsg.contains("No items expiring")) {
            Label suggestionLabel = new Label("💡 Tip: Uncheck 'Prioritize expiring items' to search with all your inventory.");
            suggestionLabel.setStyle("-fx-text-fill: #666;");
            errorBox.getChildren().add(suggestionLabel);
        }
        
        Button retryBtn = new Button("🔄 Retry Search");
        retryBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        retryBtn.setOnAction(ev -> searchRecipes());
        
        errorBox.getChildren().add(retryBtn);
        recipeContainer.getChildren().add(errorBox);
    }
    
    private void displayActiveFilters() {
        User user = getCurrentUser();
        StringBuilder filters = new StringBuilder();
        
        boolean useOnlyExpiring = useExpiringCheckBox != null && useExpiringCheckBox.isSelected();
        
        if (useOnlyExpiring) {
            List<FoodItem> expiringItems = user.getPantry().getExpiringWithinDays(3);
            filters.append("🥘 Using: ").append(expiringItems.size()).append(" expiring item(s) | ");
        } else {
            List<FoodItem> allItems = user.getPantry().getAllItems();
            filters.append("🥘 Using: ALL ").append(allItems.size()).append(" pantry items | ");
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
        
        boolean useOnlyExpiring = useExpiringCheckBox != null && useExpiringCheckBox.isSelected();
        List<FoodItem> items;
        
        if (useOnlyExpiring) {
            items = user.getPantry().getExpiringWithinDays(3);
            System.out.println("========================================");
            System.out.println("MODE: Using ONLY EXPIRING items (within 3 days)");
            System.out.println("Found " + items.size() + " expiring items");
            
            if (items.isEmpty()) {
                throw new Exception("No items expiring within 3 days found. Uncheck 'Prioritize expiring items' to search with all inventory.");
            }
        } else {
            items = user.getPantry().getAllItems();
            System.out.println("========================================");
            System.out.println("MODE: Using ALL PANTRY ITEMS");
            System.out.println("Found " + items.size() + " total items");
        }
        
        System.out.println("Items being sent to API:");
        for (FoodItem item : items) {
            System.out.println("  - " + item.getFoodName());
        }
        System.out.println("========================================");

        // Add ingredients
        if (!items.isEmpty()) {
            StringBuilder ingredientsBuilder = new StringBuilder();
            Set<String> uniqueIngredients = new LinkedHashSet<>();
            
            for (FoodItem item : items) {
                String ingredient = item.getFoodName().toLowerCase().trim();
                if (!ingredient.isEmpty() && uniqueIngredients.add(ingredient)) {
                    if (ingredientsBuilder.length() > 0) {
                        ingredientsBuilder.append(",");
                    }
                    ingredientsBuilder.append(ingredient);
                    
                    if (uniqueIngredients.size() >= 15) break;
                }
            }
            
            if (ingredientsBuilder.length() > 0) {
                String ingredients = URLEncoder.encode(ingredientsBuilder.toString(), "UTF-8");
                urlBuilder.append("&includeIngredients=").append(ingredients);
                System.out.println("Ingredients param: " + ingredientsBuilder);
            }
        }

        // Add cuisines
        if (!user.getCuisines().isEmpty()) {
            List<String> validCuisines = new ArrayList<>();
            for (String cuisine : user.getCuisines()) {
                String normalized = cuisine.toLowerCase().trim();
                if (VALID_CUISINES.contains(normalized)) {
                    validCuisines.add(normalized);
                }
            }
            if (!validCuisines.isEmpty()) {
                String cuisinesParam = String.join(",", validCuisines);
                urlBuilder.append("&cuisine=").append(URLEncoder.encode(cuisinesParam, "UTF-8"));
                System.out.println("Cuisines param: " + cuisinesParam);
            }
        }
        
        // ADD DIETS - THIS WAS MISSING!
        if (!user.getDiets().isEmpty()) {
            List<String> validDiets = new ArrayList<>();
            for (String diet : user.getDiets()) {
                String normalized = diet.toLowerCase().trim();
                if (VALID_DIETS.contains(normalized)) {
                    validDiets.add(normalized);
                }
            }
            if (!validDiets.isEmpty()) {
                // Spoonacular accepts multiple diets separated by comma
                String dietsParam = String.join(",", validDiets);
                urlBuilder.append("&diet=").append(URLEncoder.encode(dietsParam, "UTF-8"));
                System.out.println("Diets param: " + dietsParam);
            }
        }
   
        // Add intolerances
        if (!user.getAllergies().isEmpty()) {
            List<String> validIntolerances = new ArrayList<>();
            for (String allergy : user.getAllergies()) {
                String normalized = allergy.toLowerCase().trim();
                if (VALID_INTOLERANCES.contains(normalized)) {
                    validIntolerances.add(normalized);
                }
            }
            if (!validIntolerances.isEmpty()) {
                String intolerancesParam = String.join(",", validIntolerances);
                urlBuilder.append("&intolerances=").append(URLEncoder.encode(intolerancesParam, "UTF-8"));
                System.out.println("Intolerances param: " + intolerancesParam);
            }
        }
        
        // Add calorie range
        int userMinCal = user.getMinCalories();
        int userMaxCal = user.getMaxCalories();
        
        if (userMaxCal > userMinCal) {
            urlBuilder.append("&minCalories=").append(Math.max(50, userMinCal));
            urlBuilder.append("&maxCalories=").append(Math.min(2000, userMaxCal));
        }
        
        urlBuilder.append("&addRecipeInformation=true");
        urlBuilder.append("&addRecipeNutrition=true");
        urlBuilder.append("&fillIngredients=true");
        urlBuilder.append("&number=10");
        urlBuilder.append("&sort=min-missing-ingredients");
        
        System.out.println("========================================");
        System.out.println("FINAL API URL: " + urlBuilder);
        System.out.println("========================================");
        
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);
        
        if (responseCode == 402) {
            throw new Exception("Daily API limit reached (150 requests/day). Try again tomorrow.");
        }
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorMsg = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorMsg.append(line);
            }
            errorReader.close();
            throw new Exception("API Error: " + responseCode + " - " + errorMsg);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String responseStr = response.toString();
        
        if (responseStr.contains("\"totalResults\":0") || responseStr.contains("\"results\":[]")) {
            System.out.println("No results found, trying fallback search...");
            return fallbackSearch(items, user);
        }
        
        return responseStr;
    }
    
    private String fallbackSearch(List<FoodItem> items, User user) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(API_URL);
        urlBuilder.append("?apiKey=").append(API_KEY);
        
        // Simpler search with fewer ingredients
        if (!items.isEmpty()) {
            StringBuilder ingredientsBuilder = new StringBuilder();
            for (int i = 0; i < Math.min(5, items.size()); i++) {
                if (i > 0) ingredientsBuilder.append(",");
                ingredientsBuilder.append(items.get(i).getFoodName().toLowerCase().trim());
            }
            urlBuilder.append("&includeIngredients=").append(URLEncoder.encode(ingredientsBuilder.toString(), "UTF-8"));
            System.out.println("Fallback ingredients: " + ingredientsBuilder);
        }
        
        // Keep diets for fallback too (important for dietary restrictions)
        if (!user.getDiets().isEmpty()) {
            List<String> validDiets = new ArrayList<>();
            for (String diet : user.getDiets()) {
                String normalized = diet.toLowerCase().trim();
                if (VALID_DIETS.contains(normalized)) {
                    validDiets.add(normalized);
                }
            }
            if (!validDiets.isEmpty()) {
                // Use only first diet in fallback to get more results
                urlBuilder.append("&diet=").append(URLEncoder.encode(validDiets.get(0), "UTF-8"));
            }
        }
        
        // Keep intolerances for safety
        if (!user.getAllergies().isEmpty()) {
            List<String> validIntolerances = new ArrayList<>();
            for (String allergy : user.getAllergies()) {
                String normalized = allergy.toLowerCase().trim();
                if (VALID_INTOLERANCES.contains(normalized)) {
                    validIntolerances.add(normalized);
                }
            }
            if (!validIntolerances.isEmpty()) {
                urlBuilder.append("&intolerances=").append(URLEncoder.encode(String.join(",", validIntolerances), "UTF-8"));
            }
        }
        
        urlBuilder.append("&addRecipeInformation=true");
        urlBuilder.append("&addRecipeNutrition=true");
        urlBuilder.append("&number=10");
        
        System.out.println("Fallback URL: " + urlBuilder);
        
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("No recipes found. Try adjusting your preferences or adding more items.");
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
    
    private void parseAndDisplayRecipes(String jsonResponse) {
        try {
            recipeContainer.getChildren().clear();
            
            List<Map<String, String>> recipes = parseSpoonacularResponse(jsonResponse);
            
            if (recipes.isEmpty()) {
                showNoRecipesMessage();
                return;
            }
            
            boolean useOnlyExpiring = useExpiringCheckBox != null && useExpiringCheckBox.isSelected();
            User user = getCurrentUser();
            
            String headerText = useOnlyExpiring ? 
                "🍳 Found " + recipes.size() + " Recipes Using Expiring Items" :
                "🍳 Found " + recipes.size() + " Recipes Using All " + user.getPantry().getTotalItemCount() + " Pantry Items";
            
            Label headerLabel = new Label(headerText);
            headerLabel.setFont(new Font("System Bold", 18));
            recipeContainer.getChildren().add(headerLabel);
            
            if (useOnlyExpiring) {
                List<FoodItem> expiringItems = user.getPantry().getExpiringWithinDays(3);
                if (!expiringItems.isEmpty()) {
                    HBox itemsBox = new HBox(5);
                    itemsBox.setStyle("-fx-background-color: #ffebee; -fx-padding: 10; -fx-border-radius: 5;");
                    
                    StringBuilder itemsText = new StringBuilder("⚠️ Expiring items: ");
                    for (int i = 0; i < Math.min(expiringItems.size(), 5); i++) {
                        if (i > 0) itemsText.append(", ");
                        FoodItem item = expiringItems.get(i);
                        itemsText.append(item.getFoodName()).append(" (").append(item.getDaysUntilExpiration()).append("d)");
                    }
                    if (expiringItems.size() > 5) {
                        itemsText.append(" +").append(expiringItems.size() - 5).append(" more");
                    }
                    
                    Label textLabel = new Label(itemsText.toString());
                    textLabel.setWrapText(true);
                    itemsBox.getChildren().add(textLabel);
                    recipeContainer.getChildren().add(itemsBox);
                }
            } else {
                List<FoodItem> allItems = user.getPantry().getAllItems();
                HBox itemsBox = new HBox(5);
                itemsBox.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 10; -fx-border-radius: 5;");
                
                StringBuilder itemsText = new StringBuilder("✅ Using all items: ");
                for (int i = 0; i < Math.min(allItems.size(), 8); i++) {
                    if (i > 0) itemsText.append(", ");
                    itemsText.append(allItems.get(i).getFoodName());
                }
                if (allItems.size() > 8) {
                    itemsText.append(" +").append(allItems.size() - 8).append(" more");
                }
                
                Label textLabel = new Label(itemsText.toString());
                textLabel.setWrapText(true);
                itemsBox.getChildren().add(textLabel);
                recipeContainer.getChildren().add(itemsBox);
            }
            
            recipeContainer.getChildren().add(new Separator());
            
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
        
        boolean useOnlyExpiring = useExpiringCheckBox != null && useExpiringCheckBox.isSelected();
        
        String suggestions = useOnlyExpiring ?
            "Try:\n• Unchecking 'Prioritize expiring items' to use ALL inventory\n• Adding more items that expire soon\n• Adjusting your dietary preferences" :
            "Try:\n• Adding more items to your pantry\n• Removing some dietary restrictions\n• Adjusting your calorie range";
        
        Label suggestionLabel = new Label(suggestions);
        suggestionLabel.setStyle("-fx-font-size: 12px;");
        
        HBox buttonBox = new HBox(10);
        
        Button retryBtn = new Button("🔄 Search Again");
        retryBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        retryBtn.setOnAction(e -> searchRecipes());
        
        Button toggleBtn = new Button(useOnlyExpiring ? "📦 Use ALL Items" : "⚠️ Use Only Expiring");
        toggleBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        toggleBtn.setOnAction(e -> {
            useExpiringCheckBox.setSelected(!useExpiringCheckBox.isSelected());
            searchRecipes();
        });
        
        buttonBox.getChildren().addAll(retryBtn, toggleBtn);
        noRecipesBox.getChildren().addAll(noRecipesLabel, suggestionLabel, buttonBox);
        recipeContainer.getChildren().add(noRecipesBox);
    }
    
    private List<Map<String, String>> parseSpoonacularResponse(String json) {
        List<Map<String, String>> recipes = new ArrayList<>();
        
        try {
            int resultsIndex = json.indexOf("\"results\"");
            if (resultsIndex == -1) return recipes;
            
            int arrayStart = json.indexOf("[", resultsIndex);
            if (arrayStart == -1) return recipes;
            
            int arrayEnd = findMatchingBracket(json, arrayStart);
            if (arrayEnd == -1) return recipes;
            
            String resultsArray = json.substring(arrayStart + 1, arrayEnd);
            List<String> recipeBlocks = splitJsonObjects(resultsArray);
            
            for (String recipeBlock : recipeBlocks) {
                Map<String, String> recipe = new HashMap<>();
                
                String id = extractValue(recipeBlock, "\"id\"\\s*:\\s*(\\d+)", 1);
                if (id != null) recipe.put("id", id);
                
                String title = extractStringValue(recipeBlock, "title");
                if (title != null) recipe.put("name", title);
                
                String readyTime = extractValue(recipeBlock, "\"readyInMinutes\"\\s*:\\s*(\\d+)", 1);
                if (readyTime != null) recipe.put("readyTime", readyTime);
                
                String servings = extractValue(recipeBlock, "\"servings\"\\s*:\\s*(\\d+)", 1);
                if (servings != null) recipe.put("servings", servings);
                
                String sourceUrl = extractStringValue(recipeBlock, "sourceUrl");
                if (sourceUrl != null) recipe.put("sourceUrl", sourceUrl);
                
                String image = extractStringValue(recipeBlock, "image");
                if (image != null) recipe.put("image", image);
                
                String summary = extractStringValue(recipeBlock, "summary");
                if (summary != null) {
                    summary = summary.replaceAll("<[^>]+>", "").replaceAll("\\\\", "");
                    if (summary.length() > 200) summary = summary.substring(0, 197) + "...";
                    recipe.put("summary", summary);
                }
                
                if (recipeBlock.contains("\"nutrition\"")) {
                    Pattern nutritionPattern = Pattern.compile("\"nutrition\"\\s*:\\s*\\{[^}]*\"nutrients\"\\s*:\\s*\\[([^\\]]+)\\]");
                    Matcher nutritionMatcher = nutritionPattern.matcher(recipeBlock);
                    if (nutritionMatcher.find()) {
                        String nutrients = nutritionMatcher.group(1);
                        Pattern calPattern = Pattern.compile("\"name\"\\s*:\\s*\"Calories\"[^}]*\"amount\"\\s*:\\s*([\\d.]+)");
                        Matcher calMatcher = calPattern.matcher(nutrients);
                        if (calMatcher.find()) {
                            recipe.put("calories", String.valueOf((int) Double.parseDouble(calMatcher.group(1))));
                        }
                    }
                }
                
                String cuisines = extractArrayValues(recipeBlock, "cuisines");
                if (cuisines != null) recipe.put("cuisines", cuisines);
                
                String diets = extractArrayValues(recipeBlock, "diets");
                if (diets != null) recipe.put("diets", diets);
                
                String missedCount = extractValue(recipeBlock, "\"missedIngredientCount\"\\s*:\\s*(\\d+)", 1);
                if (missedCount != null) recipe.put("missingIngredients", missedCount);
                
                String usedCount = extractValue(recipeBlock, "\"usedIngredientCount\"\\s*:\\s*(\\d+)", 1);
                if (usedCount != null) recipe.put("usedIngredients", usedCount);
                
                if (recipe.containsKey("name") && recipe.containsKey("id")) {
                    recipes.add(recipe);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return recipes;
    }
    
    private int findMatchingBracket(String json, int start) {
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"' && !escaped) { inString = !inString; continue; }
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
    
    private List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inString = false, escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"' && !escaped) { inString = !inString; continue; }
            if (!inString) {
                if (c == '{') { if (depth == 0) start = i; depth++; }
                else if (c == '}') {
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
    
    private String extractValue(String json, String pattern, int group) {
        Matcher m = Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(group) : null;
    }
    
    private String extractStringValue(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private String extractArrayValues(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        return m.find() ? m.group(1).replaceAll("\"", "").trim() : null;
    }
    
    private VBox createRecipeCard(Map<String, String> recipe) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setMaxWidth(700);
        
        Label nameLabel = new Label(recipe.getOrDefault("name", "Recipe"));
        nameLabel.setFont(new Font("System Bold", 16));
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        HBox metaBox = new HBox(15);
        
        String readyTime = recipe.get("readyTime");
        if (readyTime != null) {
            metaBox.getChildren().add(new Label("⏱️ " + readyTime + " min"));
        }
        
        String servings = recipe.get("servings");
        if (servings != null) {
            metaBox.getChildren().add(new Label("👥 " + servings + " servings"));
        }
        
        String calories = recipe.get("calories");
        if (calories != null) {
            Label calLabel = new Label("🔥 " + calories + " cal");
            calLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
            metaBox.getChildren().add(calLabel);
        }
        
        HBox ingredientBox = new HBox(10);
        
        String usedIngredients = recipe.get("usedIngredients");
        if (usedIngredients != null && !usedIngredients.equals("0")) {
            Label usedLabel = new Label("✅ " + usedIngredients + " from pantry");
            usedLabel.setStyle("-fx-text-fill: green; -fx-font-size: 11px; -fx-font-weight: bold;");
            ingredientBox.getChildren().add(usedLabel);
        }
        
        String missingIngredients = recipe.get("missingIngredients");
        if (missingIngredients != null && !missingIngredients.equals("0")) {
            Label missingLabel = new Label("🛒 " + missingIngredients + " needed");
            missingLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 11px;");
            ingredientBox.getChildren().add(missingLabel);
        }
        
        HBox tagsBox = new HBox(6);
        
        String cuisines = recipe.get("cuisines");
        if (cuisines != null && !cuisines.trim().isEmpty()) {
            for (String cuisine : cuisines.split(",")) {
                if (!cuisine.trim().isEmpty()) {
                    Label tag = new Label(cuisine.trim());
                    tag.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10px;");
                    tagsBox.getChildren().add(tag);
                }
            }
        }
        
        String diets = recipe.get("diets");
        if (diets != null && !diets.trim().isEmpty()) {
            for (String diet : diets.split(",")) {
                if (!diet.trim().isEmpty()) {
                    Label tag = new Label(diet.trim());
                    tag.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10px;");
                    tagsBox.getChildren().add(tag);
                }
            }
        }
        
        String summary = recipe.get("summary");
        Label summaryLabel = null;
        if (summary != null && !summary.isEmpty()) {
            summaryLabel = new Label(summary);
            summaryLabel.setWrapText(true);
            summaryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        }
        
        Button viewBtn = new Button("📖 View Recipe");
        viewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        
        String recipeId = recipe.get("id");
        if (recipeId != null) {
            viewBtn.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://spoonacular.com/recipes/recipe-" + recipeId));
                } catch (Exception ex) {
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
        
        card.getChildren().add(nameLabel);
        if (!metaBox.getChildren().isEmpty()) card.getChildren().add(metaBox);
        if (!ingredientBox.getChildren().isEmpty()) card.getChildren().add(ingredientBox);
        if (!tagsBox.getChildren().isEmpty()) card.getChildren().add(tagsBox);
        if (summaryLabel != null) card.getChildren().add(summaryLabel);
        card.getChildren().addAll(new Separator(), viewBtn);
        
        return card;
    }
}