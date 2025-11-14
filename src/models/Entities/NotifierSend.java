package models.Entities;

public class NotifierSend implements Notifier {
	 private final User user;

	    public NotifierSend(User user) {
	        this.user = user;
	    }

	    @Override
	    public void send(Notification n) {
	        user.addNotification(n);
	    }
}
