package application;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            // Загружаем настройки из файла
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Критическая ошибка: Не удалось загрузить файл config.properties");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        String dbType = properties.getProperty("db.type", "mysql").toLowerCase();
        String url;
        String user;
        String password;

        if ("mssql".equals(dbType)) {
            url = properties.getProperty("mssql.url");
            user = properties.getProperty("mssql.user");
            password = properties.getProperty("mssql.password");
        } else {
            url = properties.getProperty("mysql.url");
            user = properties.getProperty("mysql.user");
            password = properties.getProperty("mysql.password");
        }

        // Чистый JDBC автоматически подберет нужный драйвер на основе префикса URL (jdbc:mysql или jdbc:sqlserver)
        if (user == null || user.trim().isEmpty()) {
            return DriverManager.getConnection(url); // Для Windows-авторизации MS SQL
        } else {
            return DriverManager.getConnection(url, user, password); // Для MySQL
        }
    }
}
