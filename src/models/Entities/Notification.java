package models.Entities;
import java.time.Instant;

public class Notification extends BaseEntity{
	  private String userId;//name
	    private String title;
	    private String message;
	    private Instant createdDate = Instant.now();

	    public Notification(int id, String userId, String title, String message) {
	        super(id);
	        this.userId = userId;
	        this.title = title;
	        this.message = message;
	    }

	    public String getUserId() { return userId; }
	    public void setUserId(String userId) { this.userId = userId; }

	    public String getTitle() { return title; }
	    public void setTitle(String title) { this.title = title; }

	    public String getMessage() { return message; }
	    public void setMessage(String message) { this.message = message; }

	    public Instant getCreatedDate() { return createdDate; }
	    public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }

}
