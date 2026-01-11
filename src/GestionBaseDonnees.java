import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GestionBaseDonnees {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/space_defender?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        }
    }

    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Create game_results table
            String createGameResultsTable = """
                CREATE TABLE IF NOT EXISTS game_results (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_name VARCHAR(50) NOT NULL,
                    score INT NOT NULL,
                    level INT NOT NULL,
                    difficulty VARCHAR(20) NOT NULL,
                    achieved_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;

            // Create multiplayer_results table
            String createMultiplayerResultsTable = """  
                CREATE TABLE IF NOT EXISTS multiplayer_results (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    match_id INT NOT NULL,
                    player_name VARCHAR(50) NOT NULL,
                    score INT NOT NULL,
                    played_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX (match_id)
                )
            """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createGameResultsTable);
                stmt.execute(createMultiplayerResultsTable);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public static boolean saveGameResult(String playerName, int score, int level, String difficulty) {
        String sql = "INSERT INTO game_results (player_name, score, level, difficulty) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerName);
            stmt.setInt(2, score);
            stmt.setInt(3, level);
            stmt.setString(4, difficulty);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
            return false;
        }
    }

    public static boolean saveMultiplayerResult(int matchId, String playerName, int score) {
        String sql = "INSERT INTO multiplayer_results (match_id, player_name, score) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, matchId);
            stmt.setString(2, playerName);
            stmt.setInt(3, score);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving multiplayer score: " + e.getMessage());
            return false;
        }
    }

    public static List<String> getHighScores(int limit) {
        List<String> scores = new ArrayList<>();
        String sql = "SELECT player_name, score, level, difficulty, " +
                "DATE_FORMAT(achieved_on, '%d/%m/%Y %H:%i') as date " +
                "FROM game_results " +
                "ORDER BY score DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String entry = String.format("%s - %d pts (Niv.%d %s) le %s",
                        rs.getString("player_name"),
                        rs.getInt("score"),
                        rs.getInt("level"),
                        rs.getString("difficulty"),
                        rs.getString("date"));

                scores.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Error getting highscores: " + e.getMessage());
        }
        return scores;
    }

    public static List<String> getMultiplayerHighScores(int limit) {
        List<String> scores = new ArrayList<>();
        String sql = """
            SELECT m.match_id, m.player_name, m.score, 
                   DATE_FORMAT(m.played_on, '%d/%m/%Y %H:%i') as date,
                   COUNT(*) OVER (PARTITION BY m.match_id) as players_in_match
            FROM multiplayer_results m
            ORDER BY m.score DESC
            LIMIT ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String entry = String.format("%s - %d pts (Match #%d, %d joueurs) le %s",
                        rs.getString("player_name"),
                        rs.getInt("score"),
                        rs.getInt("match_id"),
                        rs.getInt("players_in_match"),
                        rs.getString("date"));

                scores.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Error getting multiplayer highscores: " + e.getMessage());
        }
        return scores;
    }

    public static int getNextMatchId() {
        String sql = "SELECT COALESCE(MAX(match_id), 0) + 1 as next_id FROM multiplayer_results";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("next_id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting next match ID: " + e.getMessage());
        }
        return 1; // Default to 1 if there's an error
    }
}