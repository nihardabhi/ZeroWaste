package models.Entities;
import java.util.*;

public class User extends BaseEntity {
    // Authentication fields
    private String email;
    private String password;
    private String userName;
    
    // Profile fields (can be null initially)
    private String firstName;
    private String lastName;
    private final HashSet<String> cuisines = new HashSet<>();
    private final HashSet<String> diets = new HashSet<>();
    private final HashSet<String> allergies = new HashSet<>();
    private int maxCalories = 3000;
    private int minCalories = 1200;
    private boolean profileCompleted = false;
    
    // Relationships
    private Pantry pantry;
    
    // Constructor for signup (no profile yet)
    public User(String email, String password, String userName) {
        super();
        this.email = email;
        this.password = password;
        this.userName = userName;
        this.pantry = new Pantry(this);
    }
    
    // Setup profile after signup
    public void setupProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Check if profile is complete
    public boolean hasCompletedProfile() {
        return firstName != null && lastName != null;
    }
    
    // Login validation
    public boolean validateLogin(String inputPassword) {
        return this.password.equals(inputPassword);
    }
    
    // Preference management methods
    public void addCuisine(String cuisine) { 
        if (cuisine != null && !cuisine.isEmpty()) {
            cuisines.add(cuisine);
        }
    }
    
    // Add these methods
    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
    
    public void removeCuisine(String cuisine) { 
        cuisines.remove(cuisine);
    }
    
    public void addDiet(String diet) { 
        if (diet != null && !diet.isEmpty()) {
            diets.add(diet);
        }
    }
    
    public void removeDiet(String diet) { 
        diets.remove(diet);
    }
    
    public void addAllergy(String allergy) { 
        if (allergy != null && !allergy.isEmpty()) {
            allergies.add(allergy);
        }
    }
    
    public void removeAllergy(String allergy) { 
        allergies.remove(allergy);
    }
    
    // Display methods
    public String getFullName() {
        if (hasCompletedProfile()) {
            return firstName + " " + lastName;
        }
        return userName; // Fallback to username if no profile
    }
    
    public String getDisplayName() {
        return userName;
    }
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public HashSet<String> getCuisines() { return new HashSet<>(cuisines); }
    public HashSet<String> getDiets() { return new HashSet<>(diets); }
    public HashSet<String> getAllergies() { return new HashSet<>(allergies); }
    
    public int getMaxCalories() { return maxCalories; }
    public void setMaxCalories(int maxCalories) { 
        if (maxCalories > 0) {
            this.maxCalories = maxCalories;
        }
    }
    
    public int getMinCalories() { return minCalories; }
    public void setMinCalories(int minCalories) { 
        if (minCalories > 0) {
            this.minCalories = minCalories;
        }
    }
    
    public Pantry getPantry() { return pantry; }
    
    @Override
    public String toString() {
        return "User: " + getFullName() + " (" + email + ")";
    }
}