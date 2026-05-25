package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlantAndDB {

    // 1. Загрузка всех растений из БД со всеми их графиками и историей
    public static List<PlantState> loadAllPlants() {
        List<PlantState> plants = new ArrayList<>();
        
        String query = "SELECT p.ID, p.Name, p.PhotoPath, p.PlantSpeciesID, s.Name AS SpeciesName, " +
                       "MAX(CASE WHEN pt.Name = 'Полив' THEN cs.Frequency END) AS WaterFreq, " +
                       "MAX(CASE WHEN pt.Name = 'Удобрение' THEN cs.Frequency END) AS FertFreq, " +
                       "MAX(CASE WHEN pt.Name = 'Пересадка' THEN cs.Frequency END) AS RepotFreq " +
                       "FROM Plants p " +
                       "JOIN PlantSpecies s ON p.PlantSpeciesID = s.ID " +
                       "LEFT JOIN CareSchedules cs ON p.ID = cs.PlantID " +
                       "LEFT JOIN ProcedureType pt ON cs.ProcedureTypeID = pt.ID " +
                       "GROUP BY p.ID, p.Name, p.PhotoPath, p.PlantSpeciesID, s.Name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("Name");
                
                int waterSeconds = rs.getInt("WaterFreq") * 24 * 3600;
                int fertSeconds = rs.getInt("FertFreq") * 24 * 3600;
                int repotSeconds = rs.getInt("RepotFreq") * 24 * 3600;

                if (waterSeconds == 0) waterSeconds = 5 * 24 * 3600;
                if (fertSeconds == 0) fertSeconds = 30 * 24 * 3600;
                if (repotSeconds == 0) repotSeconds = 365 * 24 * 3600;

                PlantState plant = new PlantState(id, name, waterSeconds, fertSeconds, repotSeconds);
                plant.setPhotoPath(rs.getString("PhotoPath"));
                plant.setPlantSpeciesId(rs.getInt("PlantSpeciesID"));
                plant.setSpeciesName(rs.getString("SpeciesName"));

                loadLastCareTimes(con, plant);
                plants.add(plant);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plants;
    }

    private static void loadLastCareTimes(Connection con, PlantState plant) throws SQLException {
        String query = "SELECT pt.Name, MAX(ch.PerformedAt) AS LastTime " +
                       "FROM CareHistory ch " +
                       "JOIN ProcedureType pt ON ch.ProcedureTypeID = pt.ID " +
                       "WHERE ch.PlantsID = ? " +
                       "GROUP BY pt.Name";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, plant.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String procName = rs.getString("Name");
                    Timestamp ts = rs.getTimestamp("LastTime");
                    if (ts != null) {
                        LocalDateTime ldt = ts.toLocalDateTime();
                        if ("Полив".equalsIgnoreCase(procName)) plant.setLastWateringTime(ldt);
                        else if ("Удобрение".equalsIgnoreCase(procName)) plant.setLastFertilizingTime(ldt);
                        else if ("Пересадка".equalsIgnoreCase(procName)) plant.setLastRepottingTime(ldt);
                    }
                }
            }
        }
    }

    public static int insertNewPlant(PlantState plant) {
        String insertPlantQuery = "INSERT INTO Plants (Name, PhotoPath, PlantSpeciesID) VALUES (?, ?, ?)";
        String insertScheduleQuery = "INSERT INTO CareSchedules (PlantID, Frequency, NextDate, ProcedureTypeID) " +
                                     "VALUES (?, ?, ?, (SELECT ID FROM ProcedureType WHERE Name = ?))";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            int generatedPlantId = -1;

            try (PreparedStatement psPlant = con.prepareStatement(insertPlantQuery, Statement.RETURN_GENERATED_KEYS)) {
                psPlant.setString(1, plant.getName());
                psPlant.setString(2, plant.getPhotoPath());
                psPlant.setInt(3, plant.getPlantSpeciesId());
                psPlant.executeUpdate();

                try (ResultSet gk = psPlant.getGeneratedKeys()) {
                    if (gk.next()) {
                        generatedPlantId = gk.getInt(1);
                    }
                }
            }

            if (generatedPlantId == -1) {
                con.rollback();
                return -1;
            }

            try (PreparedStatement psSched = con.prepareStatement(insertScheduleQuery)) {
                java.sql.Date initialNextDate = java.sql.Date.valueOf(java.time.LocalDate.now());

                psSched.setInt(1, generatedPlantId);
                psSched.setInt(2, plant.getWateringIntervalDays());
                psSched.setDate(3, initialNextDate);
                psSched.setString(4, "Полив");
                psSched.addBatch();

                psSched.setInt(1, generatedPlantId);
                psSched.setInt(2, plant.getFertilizingIntervalDays());
                psSched.setDate(3, initialNextDate);
                psSched.setString(4, "Удобрение");
                psSched.addBatch();

                psSched.setInt(1, generatedPlantId);
                psSched.setInt(2, plant.getRepottingIntervalDays());
                psSched.setDate(3, initialNextDate);
                psSched.setString(4, "Пересадка");
                psSched.addBatch();

                psSched.executeBatch();
            }

            con.commit();
            return generatedPlantId;

        } catch (SQLException e) {
            System.err.println("Ошибка транзакции добавления растения: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public static void saveCareAction(PlantState plant, ProcedureType type) {
        String procName = "";
        int frequencyDays = 0;
        switch (type) {
            case WATERING: procName = "Полив"; frequencyDays = plant.getWateringIntervalDays(); break;
            case FERTILIZING: procName = "Удобрение"; frequencyDays = plant.getFertilizingIntervalDays(); break;
            case REPOTTING: procName = "Пересадка"; frequencyDays = plant.getRepottingIntervalDays(); break;
        }

        String insertHistoryQuery = "INSERT INTO CareHistory (PlantsID, PerformedAt, Note, ProcedureTypeID) " +
                                    "VALUES (?, ?, ?, (SELECT ID FROM ProcedureType WHERE Name = ?))";
        String updateScheduleQuery = "UPDATE CareSchedules SET NextDate = ? " +
                                     "WHERE PlantID = ? AND ProcedureTypeID = (SELECT ID FROM ProcedureType WHERE Name = ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            LocalDateTime now = LocalDateTime.now();
            
            try (PreparedStatement psHist = con.prepareStatement(insertHistoryQuery)) {
                psHist.setInt(1, plant.getId());
                psHist.setTimestamp(2, Timestamp.valueOf(now));
                psHist.setString(3, "Выполнен уход: " + procName);
                psHist.setString(4, procName);
                psHist.executeUpdate();
            }

            try (PreparedStatement psSched = con.prepareStatement(updateScheduleQuery)) {
                java.sql.Date nextDate = java.sql.Date.valueOf(now.plusDays(frequencyDays).toLocalDate());
                psSched.setDate(1, nextDate);
                psSched.setInt(2, plant.getId());
                psSched.setString(3, procName);
                psSched.executeUpdate();
            }

            con.commit();
            plant.markCareDone(type);
            
        } catch (SQLException e) {
            System.err.println("Ошибка транзакции ухода: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updatePlantPhoto(int plantId, String newPhotoPath) {
        String query = "UPDATE Plants SET PhotoPath = ? WHERE ID = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, newPhotoPath);
            ps.setInt(2, plantId);
            ps.executeUpdate();
            System.out.println("📸 Путь к фото успешно сохранен в БД для растения ID: " + plantId);
        } catch (SQLException e) {
            System.err.println("Ошибка сохранения фото в БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<String> getPlantHistoryFromDB(int plantId) {
        List<String> historyLines = new ArrayList<>();
        String query = "SELECT ch.PerformedAt, ch.Note " +
                       "FROM CareHistory ch " +
                       "WHERE ch.PlantsID = ? " +
                       "ORDER BY ch.PerformedAt DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, plantId);
            try (ResultSet rs = ps.executeQuery()) {
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("PerformedAt");
                    String note = rs.getString("Note");
                    if (ts != null) {
                        String timestamp = ts.toLocalDateTime().format(dtf);
                        historyLines.add("[" + timestamp + "] " + note);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyLines;
    }

    // ИСПРАВЛЕННЫЙ МЕТОД: теперь возвращает boolean и обновляет ВСЕ поля
    public static boolean updatePlantData(PlantState plant) {
        String updatePlantQuery = "UPDATE Plants SET Name = ?, PlantSpeciesID = ? WHERE ID = ?";
        String updateScheduleQuery = "UPDATE CareSchedules SET Frequency = ? " +
                                     "WHERE PlantID = ? AND ProcedureTypeID = (SELECT ID FROM ProcedureType WHERE Name = ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            // Обновляем имя и вид в таблице Plants
            try (PreparedStatement psPlant = con.prepareStatement(updatePlantQuery)) {
                psPlant.setString(1, plant.getName());
                psPlant.setInt(2, plant.getPlantSpeciesId());
                psPlant.setInt(3, plant.getId());
                int rowsUpdated = psPlant.executeUpdate();
                if (rowsUpdated == 0) {
                    con.rollback();
                    return false;
                }
            }

            // Обновляем частоты в CareSchedules
            try (PreparedStatement psSched = con.prepareStatement(updateScheduleQuery)) {
                // Полив
                psSched.setInt(1, plant.getWateringIntervalDays());
                psSched.setInt(2, plant.getId());
                psSched.setString(3, "Полив");
                psSched.executeUpdate();

                // Удобрение
                psSched.setInt(1, plant.getFertilizingIntervalDays());
                psSched.setInt(2, plant.getId());
                psSched.setString(3, "Удобрение");
                psSched.executeUpdate();

                // Пересадка
                psSched.setInt(1, plant.getRepottingIntervalDays());
                psSched.setInt(2, plant.getId());
                psSched.setString(3, "Пересадка");
                psSched.executeUpdate();
            }

            con.commit();
            System.out.println("✅ Данные растения ID " + plant.getId() + " успешно обновлены в БД.");
            return true;
            
        } catch (SQLException e) {
            System.err.println("Ошибка обновления данных в БД: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isSpeciesExists(String speciesName) {
        String query = "SELECT COUNT(*) FROM PlantSpecies WHERE LOWER(Name) = LOWER(?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, speciesName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean insertNewSpecies(String name, String advice) {
        if (getSpeciesIdByName(name) != -1) {
            return false; 
        }
        
        String query = "INSERT INTO PlantSpecies (Name, CareAdvice) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, name.trim());
            ps.setString(2, advice != null ? advice.trim() : "Рекомендации не указаны");
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void updatePlantSpecies(int plantId, int newSpeciesId) {
        String query = "UPDATE Plants SET PlantSpeciesID = ? WHERE ID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, newSpeciesId);
            ps.setInt(2, plantId);
            ps.executeUpdate();
            System.out.println("✅ Вид растения ID " + plantId + " обновлен на speciesId=" + newSpeciesId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getSpeciesIdByName(String speciesName) {
        String query = "SELECT ID FROM PlantSpecies WHERE LOWER(Name) = LOWER(?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, speciesName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<String> getAllSpeciesNames() {
        List<String> list = new ArrayList<>();
        String query = "SELECT Name FROM PlantSpecies ORDER BY Name";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) list.add(rs.getString("Name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void deletePlant(int plantId) {
        String query = "DELETE FROM Plants WHERE ID = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, plantId);
            ps.executeUpdate();
            System.out.println("❌ Растение с ID " + plantId + " успешно удалено из БД.");
        } catch (SQLException e) {
            System.err.println("Ошибка удаления растения из БД: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
