package application;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserActions {

    public static void markCareDone(PlantState plant, ProcedureType type, TextArea historyArea) {
        // ИСПРАВЛЕНО: Сохраняем процедуру в реляционную базу данных через транзакцию JDBC.
        // Метод saveCareAction внутри себя уже вызовет plant.markCareDone(type)
    	PlantAndDB.saveCareAction(plant, type);

        // Добавляем запись в историю на экране
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        String record = String.format("[%s] %s: %s выполнен ✅",
                timestamp, plant.getName(), getProcedureName(type));

        Platform.runLater(() -> historyArea.appendText(record + "\n"));

        // Показываем всплывающее уведомление
        showNotification(plant.getName(), getProcedureName(type));
    }

    private static String getProcedureName(ProcedureType type) {
        return switch (type) {
            case WATERING -> "Полив";
            case FERTILIZING -> "Удобрение";
            case REPOTTING -> "Пересадка";
        };
    }

    private static void showNotification(String plantName, String procedure) {
        System.out.println("🔔 Напоминание: " + plantName + " - " + procedure + " отмечен как выполненный в БД!");
    }
}
