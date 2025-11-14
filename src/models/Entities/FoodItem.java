package models.Entities;

import java.time.LocalDate;
import models.Enum.MeasurementType;

public class FoodItem extends BaseEntity implements Comparable<FoodItem> {

    private String foodName;
    private MeasurementType measurementType;
    private LocalDate expireDate;
    private int count;

    public FoodItem(int id,
                    String foodName,
                    MeasurementType type,
                    LocalDate expireDate,
                    String label,
                    int count,
                    String category) {
        super(id);
        this.foodName = foodName;
        this.measurementType = type;
        this.expireDate = expireDate;
        this.count = count;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    public void setMeasurementType(MeasurementType measurementType) {
        this.measurementType = measurementType;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // earlier expire date = "smaller", so it appears first when sorted
    @Override
    public int compareTo(FoodItem o) {
        return this.expireDate.compareTo(o.expireDate);
    }
}
