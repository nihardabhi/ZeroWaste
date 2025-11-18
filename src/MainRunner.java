import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainRunner extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the login screen first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            // Create the scene
            Scene scene = new Scene(root, 600, 400);
            
            // Configure the primary stage
            primaryStage.setTitle("ZeroWaste - Food Management System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            
            // Optional: Add an application icon (if you have one)
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
            
            // Show the stage
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            
            // Show error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start ZeroWaste");
            alert.setContentText("Error: " + e.getMessage() + "\n\n" +
                               "Please check:\n" +
                               "1. FXML files are in /views/ folder\n" +
                               "2. JavaFX is properly configured\n" +
                               "3. All controller classes exist");
            alert.showAndWait();
            
            // Exit if failed to start
            System.exit(1);
        }
    }
    
    @Override
    public void stop() {
        // Called when application is closing
        System.out.println("ZeroWaste application closing...");
        // You can add cleanup code here if needed
    }
    
    public static void main(String[] args) {
        System.out.println("Starting ZeroWaste Food Management System...");
        launch(args);
    }
}