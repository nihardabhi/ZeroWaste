package models.Entities;

public abstract class BaseEntity {
    private static int counter = 1000;  // Start from 1000 to look more realistic
    private final int id;
    
    protected BaseEntity() {
        this.id = counter++;
    }
    
    public int getId() { 
        return id; 
    }
}