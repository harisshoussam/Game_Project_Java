import java.io.Serializable;

public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        PLAYER_POSITION,
        PLAYER_SHOOT,
        PLAYER_HIT,
        ENEMY_UPDATE,
        GAME_STATE,
        CHAT_MESSAGE,
        PLAYER_JOIN,
        PLAYER_LEAVE,
        GAME_OVER
    }

    private final MessageType type;
    private String playerName;
    private int x;
    private int y;
    private int shipType;
    private int projectileX;
    private int projectileY;
    private int health;
    private int score;
    private String chatContent;
    private boolean isWinner;
    private int matchId;

    private GameMessage(MessageType type) {
        this.type = type;
    }

    // Constructeur pour position joueur
    public static GameMessage createPositionMessage(String playerName, int x, int y, int health, int score, int matchId) {
        GameMessage msg = new GameMessage(MessageType.PLAYER_POSITION);
        msg.playerName = playerName;
        msg.x = x;
        msg.y = y;
        msg.health = health;
        msg.score = score;
        msg.matchId = matchId;
        return msg;
    }

    // Constructeur pour tir
    public static GameMessage createShootMessage(String playerName, int projectileX, int projectileY, int matchId) {
        GameMessage msg = new GameMessage(MessageType.PLAYER_SHOOT);
        msg.playerName = playerName;
        msg.projectileX = projectileX;
        msg.projectileY = projectileY;
        msg.matchId = matchId;
        return msg;
    }

    // Constructeur pour hit
    public static GameMessage createHitMessage(String playerName, int matchId) {
        GameMessage msg = new GameMessage(MessageType.PLAYER_HIT);
        msg.playerName = playerName;
        msg.matchId = matchId;
        return msg;
    }

    // Constructeur pour message de chat
    public static GameMessage createChatMessage(String playerName, String chatContent, int matchId) {
        GameMessage msg = new GameMessage(MessageType.CHAT_MESSAGE);
        msg.playerName = playerName;
        msg.chatContent = chatContent;
        msg.matchId = matchId;
        return msg;
    }

    // Constructeur pour nouveaux joueurs
    public static GameMessage createJoinMessage(String playerName, int shipType, int matchId) {
        GameMessage msg = new GameMessage(MessageType.PLAYER_JOIN);
        msg.playerName = playerName;
        msg.shipType = shipType;
        msg.matchId = matchId;
        return msg;
    }

    public static GameMessage createGameOverMessage(String playerName, boolean isWinner, int score, int matchId) {
        GameMessage msg = new GameMessage(MessageType.GAME_OVER);
        msg.playerName = playerName;
        msg.isWinner = isWinner;
        msg.score = score;
        msg.matchId = matchId;
        return msg;
    }

    // Getters
    public MessageType getType() { return type; }
    public String getPlayerName() { return playerName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getProjectileX() { return projectileX; }
    public int getProjectileY() { return projectileY; }
    public int getHealth() { return health; }
    public int getScore() { return score; }
    public String getChatContent() { return chatContent; }
    public int getShipType() { return shipType; }
    public boolean isWinner() { return isWinner; }
    public int getMatchId() { return matchId; }
}