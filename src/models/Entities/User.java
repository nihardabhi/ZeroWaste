package models.Entities;
import java.util.ArrayList;
import java.util.List;

public class User extends BaseEntity{

	    private String email;
	    private String password; 	
	    private String userName;
	   
	    private final List<Notification> notifications = new ArrayList<>();

	    public User(int id, String email, String password, String userName) {
	        super(id);
	        this.email = email;
	        this.password = password;
	        this.userName = userName;
	    }

	    public String getEmail() { return email; }
	    public void setEmail(String email) { this.email = email; }

	    public String getPassword() { return password; }
	    public void setPassword(String input) { this.password = input; }

	    public String getUserName() { return userName; }
	    public void setUserName(String userName) { this.userName = userName; }

	    public List<Notification> getNotifications() { return notifications; }
	    public void addNotification(Notification n) { notifications.add(n); }
}
