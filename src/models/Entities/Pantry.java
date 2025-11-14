package models.Entities;

import java.time.LocalDate;
import java.util.*;

public class Pantry implements Iterable<FoodItem> {

    // All items; we keep this list sorted by expire date
    private final List<FoodItem> inventory = new ArrayList<>();

    public Pantry() {
    }

    // helper: keep list sorted so UI table shows "near expire first"
    private void sortByExpireDate() {
        Collections.sort(inventory);  // uses FoodItem.compareTo
    }

    // Add an item; if same id already exists, just increase count
    public void addFoodItem(FoodItem item) {
        // simple merge by id (or you can merge by name if you prefer)
        for (FoodItem f : inventory) {
            if (f.getId() == item.getId()) {
                f.setCount(f.getCount() + item.getCount());
                sortByExpireDate();
                return;
            }
        }

        // new item
        inventory.add(item);
        sortByExpireDate();
    }

    // Remove food item with given name
    public boolean removeByName(String name) {
        for (int i = 0; i < inventory.size(); i++) {
            FoodItem item = inventory.get(i);
            if (item.getFoodName().equalsIgnoreCase(name)) {
                inventory.remove(i);
                return true;
            }
        }
        return false;
    }

    // Items that expire within N days (also sorted by expire date)
    public List<FoodItem> expiringWithin(int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        List<FoodItem> result = new ArrayList<>();

        for (FoodItem item : inventory) {
            LocalDate d = item.getExpireDate();
            if (d != null && !d.isAfter(limit)) { // d <= limit
                result.add(item);
            }
        }

        Collections.sort(result);  // ensure ordered by expireDate
        return result;
    }

    // All items, already sorted by expire date (copy so caller can't break it)
    public List<FoodItem> all() {
        return new ArrayList<>(inventory);
    }

    // Find first item by name
    public FoodItem findByName(String name) {
        for (FoodItem item : inventory) {
            if (item.getFoodName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }


    @Override
    public Iterator<FoodItem> iterator() {
        return inventory.iterator();
    }
}
