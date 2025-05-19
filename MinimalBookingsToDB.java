import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MinimalBookingsToDB {
    public static void main(String[] args) {
        try {
            // Path to Downloads/bookings.json
            String path = Paths.get(System.getProperty("user.home"), "Downloads", "bookings.json").toString();
            BufferedReader reader = new BufferedReader(new FileReader(path));

            // Read entire file
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line.trim());
            }
            reader.close();

            // Clean and split manually (VERY basic parsing, assumes fixed format)
            String json = jsonBuilder.toString().replace("[", "").replace("]", "");
            String[] items = json.split("\\},\\s*\\{");

            // SQLite connection
            Connection conn = DriverManager.getConnection("jdbc:sqlite:bookingcards.db");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS bookings");
            stmt.executeUpdate("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, image TEXT)");

            for (String item : items) {
                String cleanItem = item.replace("{", "").replace("}", "").replace("\"", "");
                String[] fields = cleanItem.split(",");

                String name = "", description = "", image = "";
                for (String field : fields) {
                    if (field.trim().startsWith("name:")) name = field.split(":", 2)[1].trim();
                    else if (field.trim().startsWith("description:")) description = field.split(":", 2)[1].trim();
                    else if (field.trim().startsWith("image:")) image = field.split(":", 2)[1].trim();
                }

                String sql = String.format("INSERT INTO bookings (name, description, image) VALUES ('%s', '%s', '%s')",
                        name.replace("'", "''"), description.replace("'", "''"), image.replace("'", "''"));
                stmt.executeUpdate(sql);
            }

            stmt.close();
            conn.close();
            System.out.println(" bookingcards.db created without external libraries!");

        } catch (Exception e) {
            System.out.println(" Error:");
            e.printStackTrace();
        }
    }
}
