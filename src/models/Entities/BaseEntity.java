package models.Entities;
import java.time.Instant;

public class BaseEntity {
	    private final int id;
	  

	    protected BaseEntity(int id) {
	        this.id = id;
	    }

	    public int getId() { return id; }
	    
}
