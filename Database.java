import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private Connection conn;

    public Database() {
        try {
            Class.forName("org.sqlite.JDBC");  // <-- load driver here
            conn = DriverManager.getConnection("jdbc:sqlite:bookings.db");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT, " +
                    "hotel TEXT, " +
                    "details TEXT, " +
                    "image TEXT)");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBooking(String username, String hotel, String details, String image) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO bookings (username, hotel, details, image) VALUES (?, ?, ?, ?)"
            );
            pstmt.setString(1, username);
            pstmt.setString(2, hotel);
            pstmt.setString(3, details);
            pstmt.setString(4, image);
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
