import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SaveDataToDatabase {
    public static void main(String[] args) {
        // Database connection details
        String hostname = "192.168.1.109";
        String port = "3306";
        String database = "component_info";
        String username = "user";
        String password = "blopfis1";
        // Data to be inserted
        String serverName = "example_server";
        int uptime = 1000; // Example uptime value
        String cpuInfo = "Example CPU info";
        String memoryInfo = "Example memory info";
        String diskInfo = "Example disk info";

        // JDBC connection and statement objects
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, username, password)) {
            // Prepare SQL statement for insertion
            String sql = "INSERT INTO component_stats (server_name, uptime, cpu_info, memory_info, disk_info) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set parameters for the prepared statement
                statement.setString(1, serverName);
                statement.setInt(2, uptime);
                statement.setString(3, cpuInfo);
                statement.setString(4, memoryInfo);
                statement.setString(5, diskInfo);

                // Execute the SQL statement
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Data inserted successfully.");
                } else {
                    System.out.println("Failed to insert data.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
