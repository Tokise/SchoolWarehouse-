
package Package1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class UserInfo {
    public static User getUserInfo(String username) {
        User user = null;

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT FullName, Role FROM users WHERE Username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("FullName");
                String role = rs.getString("Role");
                user = new User();
                user.setUsername(username);
                user.setFullName(fullName);
                user.setRole(role);
            }
        } catch (SQLException e) {
        }

        return user;
    }
}
