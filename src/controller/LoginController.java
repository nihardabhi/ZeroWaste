package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Entities.User;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LoginController extends BaseController implements Initializable {
    
    // Login fields
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Button loginBtn;
    
    // Sign up fields
    @FXML private TextField signupFirstName;
    @FXML private TextField signupLastName;
    @FXML private TextField signupEmail;
    @FXML private TextField signupUsername;
    @FXML private PasswordField signupPassword;
    @FXML private Button signupBtn;
    
    private static List<User> registeredUsers = new ArrayList<>();
    
    static {
        // Only ONE test user with preferences already set
        User testUser = new User("test@example.com", "password", "test");
        testUser.setupProfile("Test", "User");
        testUser.setProfileCompleted(true); // Mark as completed
        registeredUsers.add(testUser);
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginBtn.setOnAction(e -> handleLogin());
        signupBtn.setOnAction(e -> handleSignUp());
        loginPassword.setOnAction(e -> handleLogin());
    }
    
    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Please enter both username and password");
            return;
        }
        
        User authenticatedUser = null;
        for (User user : registeredUsers) {
            if ((user.getUserName().equalsIgnoreCase(username) || 
                 user.getEmail().equalsIgnoreCase(username)) && 
                user.validateLogin(password)) {
                authenticatedUser = user;
                break;
            }
        }
        
        if (authenticatedUser != null) {
            setCurrentUser(authenticatedUser);
            
            // Check if profile is completed
            if (authenticatedUser.isProfileCompleted()) {
                navigateToDashboard();
            } else {
                navigateToPreferences(true); // true = first time setup
            }
        } else {
            showErrorAlert("Invalid username or password");
            loginPassword.clear();
        }
    }
    
    private void handleSignUp() {
        String firstName = signupFirstName.getText().trim();
        String lastName = signupLastName.getText().trim();
        String email = signupEmail.getText().trim();
        String username = signupUsername.getText().trim();
        String password = signupPassword.getText();
        
        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || 
            email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Please fill all fields");
            return;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            showErrorAlert("Please enter a valid email");
            return;
        }
        
        if (password.length() < 6) {
            showErrorAlert("Password must be at least 6 characters");
            return;
        }
        
        // Check duplicates
        for (User user : registeredUsers) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                showErrorAlert("Email already exists");
                return;
            }
            if (user.getUserName().equalsIgnoreCase(username)) {
                showErrorAlert("Username already taken");
                return;
            }
        }
        
        // Create new user
        User newUser = new User(email, password, username);
        newUser.setupProfile(firstName, lastName);
        newUser.setProfileCompleted(false); // Mark as incomplete
        registeredUsers.add(newUser);
        setCurrentUser(newUser);
        
        showSuccessAlert("Account created! Please set up your preferences.");
        
        // Navigate to preferences for first-time setup
        navigateToPreferences(true);
    }
    
    private void navigateToDashboard() {
        Stage currentStage = getCurrentStage(loginBtn);
        currentStage.close();
        navigateToPage("/views/dashboard.fxml", null, "ZeroWaste - Dashboard");
    }
    
    private void navigateToPreferences(boolean firstTime) {
        Stage currentStage = getCurrentStage(loginBtn);
        currentStage.close();
        
        String title = firstTime ? "ZeroWaste - Complete Your Profile" : "ZeroWaste - Preferences";
        navigateToPage("/views/user-preferences.fxml", null, title);
    }
    
    public static void addUser(User user) {
        registeredUsers.add(user);
    }
}