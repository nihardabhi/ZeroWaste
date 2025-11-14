package models.Entities;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import models.Enum.MeasurementType;

public class FoodItem extends BaseEntity implements Comparable<FoodItem> {
    private String foodName;
    private String category;
    private MeasurementType measurementType;
    private double quantity;  // For GALLON, OUNCE, etc.
    private int count;        // For COUNT type
    private LocalDate expireDate;
    private LocalDate purchaseDate;
    
    public FoodItem(String foodName, String category, MeasurementType type, LocalDate expireDate) {
        super();
        this.foodName = foodName;
        this.category = category;
        this.measurementType = type;
        this.expireDate = expireDate;
        this.purchaseDate = LocalDate.now();
        
        // Set default quantity based on type
        if (type == MeasurementType.COUNT) {
            this.count = 1;
            this.quantity = 0;
        } else {
            this.count = 0;
            this.quantity = 1.0;
        }
    }
    
    // Check if expired
    public boolean isExpired() {
        return LocalDate.now().isAfter(expireDate);
    }
    
    // Days until expiration
    public long getDaysUntilExpiration() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
    }
    
    // Check if expiring soon (within 3 days)
    public boolean isExpiringSoon() {
        long days = getDaysUntilExpiration();
        return days >= 0 && days <= 3;
    }
    
    // Get display string for quantity
    public String getQuantityDisplay() {
        if (measurementType == MeasurementType.COUNT) {
            return count + " items";
        } else {
            return quantity + " " + measurementType.toString().toLowerCase();
        }
    }
    
    // Update quantity methods
    public void addQuantity(double amount) {
        if (measurementType == MeasurementType.COUNT) {
            this.count += (int) amount;
        } else {
            this.quantity += amount;
        }
    }
    
    public void useQuantity(double amount) {
        if (measurementType == MeasurementType.COUNT) {
            this.count = Math.max(0, count - (int) amount);
        } else {
            this.quantity = Math.max(0, quantity - amount);
        }
    }
    
    public boolean isOutOfStock() {
        return (measurementType == MeasurementType.COUNT ? count <= 0 : quantity <= 0);
    }
    
    // Compare by expiration date for sorting
    @Override
    public int compareTo(FoodItem other) {
        return this.expireDate.compareTo(other.expireDate);
    }
    
    // Getters and Setters
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public MeasurementType getMeasurementType() { return measurementType; }
    public void setMeasurementType(MeasurementType measurementType) { 
        this.measurementType = measurementType; 
    }
    
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
    
    public LocalDate getPurchaseDate() { return purchaseDate; }
    
    @Override
    public String toString() {
        return foodName + " - " + getQuantityDisplay() + " (Expires: " + expireDate + ")";
    }
}