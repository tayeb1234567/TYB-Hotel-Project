import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class LogDatabase {
    private Connection conn;

    public LogDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");  // <-- load driver here too
            conn = DriverManager.getConnection("jdbc:sqlite:logs.db");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "timestamp TEXT, " +
                    "message TEXT)");
            stmt.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void addLog(String message) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO logs (timestamp, message) VALUES (?, ?)"
            );
            pstmt.setString(1, timestamp);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
