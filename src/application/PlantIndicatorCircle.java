package application;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PlantIndicatorCircle extends Circle {
    private PlantState plant;
    private ProcedureType type;

    public PlantIndicatorCircle(PlantState plant, ProcedureType type) {
        super(25);
        this.plant = plant;
        this.type = type;
        setStroke(Color.BLACK);
        updateStatus();
    }

    public void updateStatus() {
        CareStatus status = plant.getCareStatus(type);
        switch (status) {
            case GREEN -> setFill(Color.GREEN);
            case YELLOW -> setFill(Color.ORANGE);
            case RED -> setFill(Color.RED);
        }
    }
}