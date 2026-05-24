package application;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PlantState {
    private int id; // ДОБАВЛЕНО: ID из базы данных
    private String name;
    private String photoPath;
    private int plantSpeciesId; // ДОБАВЛЕНО: ID вида из базы данных
    private String speciesName;   // ДОБАВЛЕНО: Текстовое название вида для интерфейса

    private int wateringIntervalSeconds;  
    private int fertilizingIntervalSeconds; 
    private int repottingIntervalSeconds;   

    private LocalDate lastWatering;
    private LocalDate lastFertilizing;
    private LocalDate lastRepotting;
    
    private LocalDateTime lastWateringTime;
    private LocalDateTime lastFertilizingTime;
    private LocalDateTime lastRepottingTime;

    // Конструктор по умолчанию (пригодится при сборке объекта из ResultSet)
    public PlantState() {}

    // Ваш основной конструктор (адаптированный под ID)
    public PlantState(int id, String name, int waterSeconds, int fertSeconds, int repotSeconds) {
        this.id = id;
        this.name = name;
        this.wateringIntervalSeconds = waterSeconds;
        this.fertilizingIntervalSeconds = fertSeconds;
        this.repottingIntervalSeconds = repotSeconds;
        this.photoPath = null;
        
        this.lastWatering = LocalDate.now();
        this.lastFertilizing = LocalDate.now();
        this.lastRepotting = LocalDate.now();
        
        this.lastWateringTime = LocalDateTime.now();
        this.lastFertilizingTime = LocalDateTime.now();
        this.lastRepottingTime = LocalDateTime.now();
    }

    // --- ДОБАВЛЕННЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ СВЯЗИ С JDBC ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPlantSpeciesId() { return plantSpeciesId; }
    public void setPlantSpeciesId(int plantSpeciesId) { this.plantSpeciesId = plantSpeciesId; }

    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }

    public LocalDateTime getLastWateringTime() { return lastWateringTime; }
    public void setLastWateringTime(LocalDateTime lastWateringTime) { 
        this.lastWateringTime = lastWateringTime;
        if (lastWateringTime != null) this.lastWatering = lastWateringTime.toLocalDate();
    }

    public LocalDateTime getLastFertilizingTime() { return lastFertilizingTime; }
    public void setLastFertilizingTime(LocalDateTime lastFertilizingTime) { 
        this.lastFertilizingTime = lastFertilizingTime;
        if (lastFertilizingTime != null) this.lastFertilizing = lastFertilizingTime.toLocalDate();
    }

    public LocalDateTime getLastRepottingTime() { return lastRepottingTime; }
    public void setLastRepottingTime(LocalDateTime lastRepottingTime) { 
        this.lastRepottingTime = lastRepottingTime;
        if (lastRepottingTime != null) this.lastRepotting = lastRepottingTime.toLocalDate();
    }
    // -------------------------------------------------------

    // Геттеры и сеттеры для секунд
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    
    public int getWateringIntervalSeconds() { return wateringIntervalSeconds; }
    public void setWateringIntervalSeconds(int seconds) { this.wateringIntervalSeconds = seconds; }
    
    public int getFertilizingIntervalSeconds() { return fertilizingIntervalSeconds; }
    public void setFertilizingIntervalSeconds(int seconds) { this.fertilizingIntervalSeconds = seconds; }
    
    public int getRepottingIntervalSeconds() { return repottingIntervalSeconds; }
    public void setRepottingIntervalSeconds(int seconds) { this.repottingIntervalSeconds = seconds; }
    
    // Совместимость со старым кодом (дни)
    public int getWateringIntervalDays() { return wateringIntervalSeconds / (24 * 3600); }
    public void setWateringIntervalDays(int days) { this.wateringIntervalSeconds = days * 24 * 3600; }
    
    public int getFertilizingIntervalDays() { return fertilizingIntervalSeconds / (24 * 3600); }
    public void setFertilizingIntervalDays(int days) { this.fertilizingIntervalSeconds = days * 24 * 3600; }
    
    public int getRepottingIntervalDays() { return repottingIntervalSeconds / (24 * 3600); }
    public void setRepottingIntervalDays(int days) { this.repottingIntervalSeconds = days * 24 * 3600; }

    public CareStatus getCareStatus(ProcedureType type) {
        long secondsUntil = getSecondsUntil(type);
        if (secondsUntil <= 0) return CareStatus.RED;
        // Для тестирования секундного интервала: желтый статус загорается, если осталось менее 10 секунд
        // (Вы можете поменять назад на дни: 2 * 24 * 3600)
        if (secondsUntil <= 10) return CareStatus.YELLOW; 
        return CareStatus.GREEN;
    }

    public int getDaysUntil(ProcedureType type) {
        long secondsUntil = getSecondsUntil(type);
        return (int) (secondsUntil / (24 * 3600));
    }
    
    public long getSecondsUntil(ProcedureType type) {
        LocalDateTime lastDateTime;
        int intervalSeconds;
        
        switch (type) {
            case WATERING:
                lastDateTime = lastWateringTime;
                intervalSeconds = wateringIntervalSeconds;
                break;
            case FERTILIZING:
                lastDateTime = lastFertilizingTime;
                intervalSeconds = fertilizingIntervalSeconds;
                break;
            case REPOTTING:
                lastDateTime = lastRepottingTime;
                intervalSeconds = repottingIntervalSeconds;
                break;
            default:
                return 0;
        }
        
        if (lastDateTime == null) return 0; // Защита от NullPointerException, если ухода еще не было
        
        long secondsSince = java.time.Duration.between(lastDateTime, LocalDateTime.now()).getSeconds();
        return Math.max(0, intervalSeconds - secondsSince);
    }

    public void markCareDone(ProcedureType type) {
        switch (type) {
            case WATERING:
                lastWatering = LocalDate.now();
                lastWateringTime = LocalDateTime.now();
                break;
            case FERTILIZING:
                lastFertilizing = LocalDate.now();
                lastFertilizingTime = LocalDateTime.now();
                break;
            case REPOTTING:
                lastRepotting = LocalDate.now();
                lastRepottingTime = LocalDateTime.now();
                break;
        }
    }
}
