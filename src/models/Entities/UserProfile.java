package models.Entities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class UserProfile extends BaseEntity{
	  	private String firstName; // store first name
	  	private String lastName; //store last name
	  	private final HashSet<String> cuisines = new HashSet<>(); //store cuisines
	  	private int maxCalories; // max calories
	    private int minCalories; //min calories
	    private int userId;
	  
	    
	    private final HashSet<String> diets = new HashSet<>(); //store diets
	    private final HashSet<String> allergies = new HashSet<>(); //store allergies
	    
	    public UserProfile(int id, String firstName, String lastName) {
	        super(id);
	        this.userId = id;
	        this.firstName = firstName;
	        this.lastName = lastName;
	    }
	    //add the method to change preferences

	    public String getFirstName() { return firstName; }
	    public void setFirstName(String firstName) { this.firstName = firstName; }

	    public String getLastName() { return lastName; }
	    public void setLastName(String lastName) { this.lastName = lastName; }

	    public HashSet<String> getCuisines() { return cuisines; }
	    public HashSet<String> getDiets() { return diets; }
	    public HashSet<String> getAllergies() { return allergies; }

	    public int getMaxCalories() { return maxCalories; }
	    public void setMaxCalories(int maxCalories) { this.maxCalories = maxCalories; }

	    public int getMinCalories() { return minCalories; }
	    public void setMinCalories(int MinCalories) { this.minCalories = MinCalories; }

	   
	   	    @Override public String toString() {
	        return firstName + " " + lastName ;
	    }
}
