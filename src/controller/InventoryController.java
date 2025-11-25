package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Entities.FoodItem;
import models.Entities.Pantry;
import models.Enum.MeasurementType;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class InventoryController extends BaseController implements Initializable {
    
    // Form fields
    @FXML private TextField itemNameField;
    @FXML private ComboBox<MeasurementType> measurementTypeCombo;
    @FXML private TextField quantityField;
    @FXML private DatePicker expiryDatePicker;
    
    // Category combo box
    @FXML private ComboBox<String> categoryCombo;
    
    // Buttons
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button removeButton;
    @FXML private Button clearButton;
    
    // Table
    @FXML private TableView<FoodItem> inventoryTable;
    @FXML private TableColumn<FoodItem, Integer> idColumn;
    @FXML private TableColumn<FoodItem, String> nameColumn;
    @FXML private TableColumn<FoodItem, String> categoryColumn;
    @FXML private TableColumn<FoodItem, MeasurementType> typeColumn;
    @FXML private TableColumn<FoodItem, String> quantityColumn;
    @FXML private TableColumn<FoodItem, LocalDate> expiryColumn;
    @FXML private TableColumn<FoodItem, String> statusColumn;
    
    private ObservableList<FoodItem> itemsList;
    private FoodItem selectedItem;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFormFields();
        setupButtons();
        loadInventoryData();
        applyTableStyling();
    }
    
    private void setupTable() {
        if (inventoryTable == null) return;
        
        // Setup column cell factories
        if (idColumn != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        
        if (nameColumn != null) {
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("foodName"));
        }
        
        if (categoryColumn != null) {
            categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        }
        
        if (typeColumn != null) {
            typeColumn.setCellValueFactory(new PropertyValueFactory<>("measurementType"));
        }
        
        if (expiryColumn != null) {
            expiryColumn.setCellValueFactory(new PropertyValueFactory<>("expireDate"));
        }
        
        if (quantityColumn != null) {
            quantityColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getQuantityDisplay()));
        }
        
        // Add status column if exists
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cellData -> {
                FoodItem item = cellData.getValue();
                String status = "✅ Good";
                if (item.isExpired()) {
                    status = "❌ EXPIRED";
                } else if (item.getDaysUntilExpiration() == 0) {
                    status = "⚠️ Expires TODAY";
                } else if (item.getDaysUntilExpiration() <= 3) {
                    status = "⚠️ Expires in " + item.getDaysUntilExpiration() + " days";
                } else if (item.getDaysUntilExpiration() <= 7) {
                    status = "📅 Expires in " + item.getDaysUntilExpiration() + " days";
                }
                return new javafx.beans.property.SimpleStringProperty(status);
            });
        }
        
        // Enhanced row factory with color coding
        inventoryTable.setRowFactory(tv -> {
            TableRow<FoodItem> row = new TableRow<FoodItem>() {
                @Override
                protected void updateItem(FoodItem item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    // Clear previous styles
                    setStyle("");
                    getStyleClass().removeAll("expired-row", "expiring-today-row", 
                        "expiring-soon-row", "expiring-week-row", "safe-row");
                    
                    if (item == null || empty) {
                        setStyle("");
                    } else {
                        long daysUntilExpiry = item.getDaysUntilExpiration();
                        String baseStyle = "-fx-text-fill: black; -fx-font-weight: normal; ";
                        
                        if (item.isExpired()) {
                            setStyle(baseStyle + "-fx-background-color: #ff5252;");
                            getStyleClass().add("expired-row");
                        } else if (daysUntilExpiry == 0) {
                            setStyle(baseStyle + "-fx-background-color: #ff6e40;");
                            getStyleClass().add("expiring-today-row");
                        } else if (daysUntilExpiry <= 3) {
                            setStyle(baseStyle + "-fx-background-color: #ffa726;");
                            getStyleClass().add("expiring-soon-row");
                        } else if (daysUntilExpiry <= 5) {
                            setStyle(baseStyle + "-fx-background-color: #ffee58;");
                            getStyleClass().add("expiring-week-row");
                        } else {
                            setStyle(baseStyle + "-fx-background-color: #c8e6c9;");
                            getStyleClass().add("safe-row");
                        }
                    }
                }
            };
            
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected && row.getItem() != null) {
                    String currentStyle = row.getStyle();
                    if (currentStyle != null && !currentStyle.isEmpty()) {
                        row.setStyle(currentStyle + " -fx-border-color: #1976d2; -fx-border-width: 2px;");
                    }
                }
            });
            
            return row;
        });
        
        // Selection listener
        inventoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateForm(newSelection);
                    selectedItem = newSelection;
                }
            }
        );
    }
    
    private void applyTableStyling() {
        inventoryTable.setStyle(
            "-fx-selection-bar: transparent;" +
            "-fx-selection-bar-text: black;" +
            "-fx-cell-hover-color: transparent;" +
            "-fx-font-family: 'System';" +
            "-fx-font-size: 13px;"
        );
        
        inventoryTable.getColumns().forEach(column -> {
            column.setStyle("-fx-text-fill: black; -fx-alignment: CENTER-LEFT;");
        });
    }
    
    private void setupFormFields() {
        // Setup measurement type combo
        if (measurementTypeCombo != null) {
            measurementTypeCombo.setItems(FXCollections.observableArrayList(MeasurementType.values()));
            measurementTypeCombo.setValue(MeasurementType.COUNT);
            measurementTypeCombo.setStyle("-fx-font-size: 12px;");
        }
        
        // Setup category combo
        if (categoryCombo != null) {
            ObservableList<String> categories = FXCollections.observableArrayList(
                "Dairy", "Grains", "Protein", "Vegetables", "Fruits", 
                "Beverages", "Snacks", "Condiments", "Frozen", "Baking", 
                "Spices", "Canned Goods", "Other"
            );
            categoryCombo.setItems(categories);
            categoryCombo.setValue("Other");
            categoryCombo.setStyle("-fx-font-size: 12px;");
        }
        
        // Setup date picker with default value and disable past dates
        if (expiryDatePicker != null) {
            expiryDatePicker.setValue(LocalDate.now().plusDays(7));
            
            // Disable all past dates (yesterday and before)
            expiryDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    
                    // Disable dates before today
                    if (date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #999999;");
                    }
                }
            });
            
            // Prevent manual entry of past dates
            expiryDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
                if (newDate != null && newDate.isBefore(LocalDate.now())) {
                    expiryDatePicker.setValue(LocalDate.now());
                    showWarningAlert("Cannot select past dates. Date set to today.");
                }
            });
        }
        
        // Setup quantity field
        if (quantityField != null) {
            quantityField.setText("1");
            quantityField.setStyle("-fx-font-size: 12px;");
            
            // Add number validation
            quantityField.textProperty().addListener((obs, oldText, newText) -> {
                if (!newText.matches("\\d*\\.?\\d*")) {
                    quantityField.setText(oldText);
                }
            });
        }
        
        // Style text fields
        if (itemNameField != null) {
            itemNameField.setStyle("-fx-font-size: 12px;");
        }
    }
    
    private void setupButtons() {
        if (addButton != null) {
            addButton.setOnAction(e -> handleAddItem());
            addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-cursor: hand;");
        }
        
        if (updateButton != null) {
            updateButton.setOnAction(e -> handleUpdateItem());
            updateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                                "-fx-font-weight: bold; -fx-cursor: hand;");
        }
        
        if (removeButton != null) {
            removeButton.setOnAction(e -> handleRemoveItem());
            removeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                                "-fx-font-weight: bold; -fx-cursor: hand;");
        }
        
        if (clearButton != null) {
            clearButton.setOnAction(e -> clearForm());
            clearButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; " +
                               "-fx-font-weight: bold; -fx-cursor: hand;");
        }
    }
    
    private void handleAddItem() {
        if (!validateForm()) return;
        
        String name = itemNameField.getText().trim();
        String category = categoryCombo != null ? categoryCombo.getValue() : "Other";
        MeasurementType type = measurementTypeCombo.getValue();
        LocalDate expiryDate = expiryDatePicker.getValue();
        
        Pantry pantry = getCurrentUser().getPantry();
        
        // Check if item already exists
        if (pantry.findByName(name) != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Item Exists");
            alert.setHeaderText("Item '" + name + "' already exists");
            alert.setContentText("Do you want to add to the existing quantity instead?");
            
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                FoodItem existingItem = pantry.findByName(name);
                try {
                    double quantity = Double.parseDouble(quantityField.getText());
                    existingItem.addQuantity(quantity);
                    loadInventoryData();
                    clearForm();
                    showSuccessAlert("Quantity added to existing item!");
                } catch (NumberFormatException ex) {
                    showErrorAlert("Invalid quantity");
                }
            }
            return;
        }
        
        // Create new item
        FoodItem newItem = new FoodItem(name, category, type, expiryDate);
        
        try {
            double quantity = Double.parseDouble(quantityField.getText());
            if (type == MeasurementType.COUNT) {
                newItem.setCount((int) quantity);
                newItem.setQuantity(0);
            } else {
                newItem.setQuantity(quantity);
                newItem.setCount(0);
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid quantity");
            return;
        }
        
        pantry.addFoodItem(newItem);
        loadInventoryData();
        clearForm();
        
        // Show expiry warning if applicable
        if (newItem.getDaysUntilExpiration() == 0) {
            showWarningAlert("⚠️ Warning: This item expires today!");
        } else if (newItem.isExpiringSoon()) {
            showWarningAlert("📅 Note: This item expires in " + newItem.getDaysUntilExpiration() + " days!");
        } else {
            showSuccessAlert("✅ Item added successfully!");
        }
    }
    
    private void handleUpdateItem() {
        if (selectedItem == null) {
            showWarningAlert("Please select an item to update");
            return;
        }
        
        if (!validateForm()) return;
        
        // Update selected item properties
        selectedItem.setFoodName(itemNameField.getText().trim());
        if (categoryCombo != null) {
            selectedItem.setCategory(categoryCombo.getValue());
        }
        selectedItem.setMeasurementType(measurementTypeCombo.getValue());
        selectedItem.setExpireDate(expiryDatePicker.getValue());
        
        try {
            double quantity = Double.parseDouble(quantityField.getText());
            if (selectedItem.getMeasurementType() == MeasurementType.COUNT) {
                selectedItem.setCount((int) quantity);
                selectedItem.setQuantity(0);
            } else {
                selectedItem.setQuantity(quantity);
                selectedItem.setCount(0);
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid quantity");
            return;
        }
        
        // Refresh the table
        inventoryTable.refresh();
        loadInventoryData();
        clearForm();
        showSuccessAlert("✅ Item updated successfully!");
    }
    
    private void handleRemoveItem() {
        if (selectedItem == null) {
            showWarningAlert("Please select an item to remove");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Item");
        confirm.setContentText("Are you sure you want to remove '" + selectedItem.getFoodName() + "'?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            getCurrentUser().getPantry().removeFoodItem(selectedItem.getFoodName());
            loadInventoryData();
            clearForm();
            showSuccessAlert("✅ Item removed successfully!");
        }
    }
    
    private boolean validateForm() {
        if (itemNameField.getText().trim().isEmpty()) {
            showErrorAlert("Please enter an item name");
            itemNameField.requestFocus();
            return false;
        }
        
        if (measurementTypeCombo.getValue() == null) {
            showErrorAlert("Please select a measurement type");
            measurementTypeCombo.requestFocus();
            return false;
        }
        
        try {
            double quantity = Double.parseDouble(quantityField.getText());
            if (quantity <= 0) {
                showErrorAlert("Quantity must be greater than 0");
                quantityField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Please enter a valid number for quantity");
            quantityField.requestFocus();
            return false;
        }
        
        if (expiryDatePicker.getValue() == null) {
            showErrorAlert("Please select an expiry date");
            expiryDatePicker.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void populateForm(FoodItem item) {
        itemNameField.setText(item.getFoodName());
        if (categoryCombo != null) {
            categoryCombo.setValue(item.getCategory());
        }
        measurementTypeCombo.setValue(item.getMeasurementType());
        expiryDatePicker.setValue(item.getExpireDate());
        
        if (item.getMeasurementType() == MeasurementType.COUNT) {
            quantityField.setText(String.valueOf(item.getCount()));
        } else {
            quantityField.setText(String.valueOf(item.getQuantity()));
        }
    }
    
    private void clearForm() {
        itemNameField.clear();
        if (categoryCombo != null) {
            categoryCombo.setValue("Other");
        }
        measurementTypeCombo.setValue(MeasurementType.COUNT);
        quantityField.setText("1");
        expiryDatePicker.setValue(LocalDate.now().plusDays(7));
        selectedItem = null;
        inventoryTable.getSelectionModel().clearSelection();
    }
    
    private void loadInventoryData() {
        if (getCurrentUser() == null) return;
        
        Pantry pantry = getCurrentUser().getPantry();
        itemsList = FXCollections.observableArrayList(pantry.getAllItems());
        
        // Sort by expiry date (expired and expiring soon first)
        itemsList.sort((a, b) -> {
            if (a.isExpired() && !b.isExpired()) return -1;
            if (!a.isExpired() && b.isExpired()) return 1;
            return Long.compare(a.getDaysUntilExpiration(), b.getDaysUntilExpiration());
        });
        
        inventoryTable.setItems(itemsList);
        inventoryTable.refresh();
    }
}