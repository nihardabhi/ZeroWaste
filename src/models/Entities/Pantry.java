package models.Entities;
import java.time.LocalDate;
import java.util.*;

import models.Enum.MeasurementType;

public class Pantry implements Iterable<FoodItem> {
    private final List<FoodItem> inventory = new ArrayList<>();
    private final User owner;
    private final Set<String> shoppingList = new HashSet<>();
    
    // Thresholds for auto-adding to shopping list
    private static final int MIN_COUNT_THRESHOLD = 2;  // For COUNT items
    private static final double MIN_QUANTITY_THRESHOLD = 0.5;  // For measured items (gallon, oz, etc)
    
    public Pantry(User owner) {
        this.owner = owner;
    }
    
    // Add food item to pantry
    public void addFoodItem(FoodItem item) {
        inventory.add(item);
        sortByExpireDate();
        // Remove from shopping list if we just bought it
        removeFromShoppingList(item.getFoodName());
    }
    
    // Remove food item by name
    public boolean removeFoodItem(String foodName) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getFoodName().equalsIgnoreCase(foodName)) {
                FoodItem removed = inventory.remove(i);
                // Add to shopping list when completely removed
                addToShoppingList(removed.getFoodName());
                return true;
            }
        }
        return false;
    }
    
    // Check if item is running low
    private boolean isRunningLow(FoodItem item) {
        if (item.getMeasurementType() == MeasurementType.COUNT) {
            return item.getCount() < MIN_COUNT_THRESHOLD;
        } else {
            return item.getQuantity() < MIN_QUANTITY_THRESHOLD;
        }
    }
    
    // Get all items that are running low
    public List<FoodItem> getLowStockItems() {
        List<FoodItem> lowStock = new ArrayList<>();
        
        for (FoodItem item : inventory) {
            if (isRunningLow(item)) {
                lowStock.add(item);
            }
        }
        return lowStock;
    }
    
    // Get all items
    public List<FoodItem> getAllItems() {
        return new ArrayList<>(inventory);
    }
    
    // Get items expiring within N days
    public List<FoodItem> getExpiringWithinDays(int days) {
        List<FoodItem> expiringItems = new ArrayList<>();
        LocalDate limitDate = LocalDate.now().plusDays(days);
        
        for (FoodItem item : inventory) {
            if (!item.isExpired() && item.getExpireDate().isBefore(limitDate.plusDays(1))) {
                expiringItems.add(item);
            }
        }
        
        Collections.sort(expiringItems);
        return expiringItems;
    }
    
    // Get expired items
    public List<FoodItem> getExpiredItems() {
        List<FoodItem> expiredItems = new ArrayList<>();
        
        for (FoodItem item : inventory) {
            if (item.isExpired()) {
                expiredItems.add(item);
            }
        }
        return expiredItems;
    }
    
    // Get items by category
    public List<FoodItem> getItemsByCategory(String category) {
        List<FoodItem> categoryItems = new ArrayList<>();
        
        for (FoodItem item : inventory) {
            if (item.getCategory().equalsIgnoreCase(category)) {
                categoryItems.add(item);
            }
        }
        return categoryItems;
    }
    
    // Find item by name
    public FoodItem findByName(String foodName) {
        for (FoodItem item : inventory) {
            if (item.getFoodName().equalsIgnoreCase(foodName)) {
                return item;
            }
        }
        return null;
    }
    
    // Dashboard statistics
    public int getTotalItemCount() {
        return inventory.size();
    }
    
    public int getExpiringItemCount() {
        int count = 0;
        for (FoodItem item : inventory) {
            if (item.isExpiringSoon()) {
                count++;
            }
        }
        return count;
    }
    
    public int getExpiredItemCount() {
        int count = 0;
        for (FoodItem item : inventory) {
            if (item.isExpired()) {
                count++;
            }
        }
        return count;
    }
    
    public int getLowStockCount() {
        int count = 0;
        for (FoodItem item : inventory) {
            if (isRunningLow(item)) {
                count++;
            }
        }
        return count;
    }
    
    // Shopping list management
    public void addToShoppingList(String item) {
        if (item != null && !item.isEmpty()) {
            shoppingList.add(item);
        }
    }
    
    public void removeFromShoppingList(String item) {
        shoppingList.remove(item);
    }
    
    public Set<String> getShoppingList() {
        return new HashSet<>(shoppingList);
    }
    
    public void clearShoppingList() {
        shoppingList.clear();
    }
    
    public int getShoppingListCount() {
        return shoppingList.size();
    }
    
    // Clear expired items
    public int clearExpiredItems() {
        int count = 0;
        Iterator<FoodItem> it = inventory.iterator();
        
        while (it.hasNext()) {
            FoodItem item = it.next();
            if (item.isExpired()) {
                it.remove();
                count++;
                // Add expired item to shopping list (optional)
                addToShoppingList(item.getFoodName());
            }
        }
        return count;
    }
    
    // Private helper methods
    private void sortByExpireDate() {
        Collections.sort(inventory);
    }
        
    @Override
    public Iterator<FoodItem> iterator() {
        return inventory.iterator();
    }
}