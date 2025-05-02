package Package1;

public class User {
    private String username;
    private String fullName;
    private String role;
    private int userId; // Add this field
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getUserId() { // Implement this method correctly
        return userId;
    }

    public void setUserId(int userId) { // Add this setter
        this.userId = userId;
    }
    
    // Helper method to check if the user is an admin
    public boolean isAdmin() {
        return "Admin".equals(role);
    }

    @Override
    public String toString() {
        return "User{" +
               "userId=" + userId +
               ", username='" + username + '\'' +
               ", fullName='" + fullName + '\'' +
               ", role='" + role + '\'' +
               '}';
    }
}