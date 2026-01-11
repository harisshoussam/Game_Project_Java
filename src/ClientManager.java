import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class ClientManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String playerName;
    private int shipType;
    private final List<String> chatMessages = new ArrayList<>();
    private final Set<String> onlinePlayers = new HashSet<>();
    private final Map<String, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final List<RemoteProjectile> remoteProjectiles = new ArrayList<>();
    private boolean connected = false;
    private Thread listenerThread;
    private Bouclejeu gamePanel;
    private int matchId;

    // Représente un joueur distant
    public static class RemotePlayer {
        private String name;
        private int x, y;
        private int health;
        private int score;
        private int shipType;
        private Image[] sprites;

        public RemotePlayer(String name, int shipType) {
            this.name = name;
            this.shipType = shipType;
            this.x = 380;
            this.y = 150;  // Les joueurs distants commencent plus haut
            this.health = 3;
            this.score = 0;
            this.sprites = new Image[3];
            loadSprites();
        }

        private void loadSprites() {
            for (int i = 0; i < 3; i++) {
                sprites[i] = GestionRessources.getImage("/ship_" + shipType + ".png");
            }
        }

        public void update(int x, int y, int health, int score) {
            this.x = x;
            this.y = y;
            this.health = health;
            this.score = score;
        }

        public void draw(Graphics g) {
            // Mirror y-coordinate for versus effect
            int mirroredY = 600 - y - 60;
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(x + 25, mirroredY + 30); // Move to center of sprite
            g2d.rotate(Math.PI); // Rotate 180 degrees
            g2d.translate(-25, -30); // Move back
            g2d.drawImage(sprites[0], 0, 0, 50, 60, null);
            g2d.setTransform(new AffineTransform()); // Reset transform

            // Nom du joueur
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(name, x, mirroredY - 20);

            // Barre de vie
            g.setColor(Color.RED);
            g.fillRect(x, mirroredY - 15, 50, 5);
            g.setColor(Color.CYAN);  // Couleur différente pour l'adversaire
            g.fillRect(x, mirroredY - 15, (int)(50 * ((double)health / 3)), 5);
        }

        public Rectangle getHitbox() {
            int mirroredY = 600 - y - 60;
            return new Rectangle(x, mirroredY, 50, 60);
        }

        public String getName() { return name; }
        public int getHealth() { return health; }
    }

    // Représente un projectile envoyé par un joueur distant
    public static class RemoteProjectile {
        private int x, yOriginal; // yOriginal is the value received from the network
        private int y; // y is the mirrored value for local display
        private boolean active = true;
        private final int speed = 10;

        public RemoteProjectile(int x, int yOriginal) {
            this.x = x;
            this.yOriginal = yOriginal;
            // Mirror the y-coordinate for versus effect
            this.y = 600 - yOriginal - 15;
        }

        public void update() {
            y += speed; // Move downward
            if (y > 600) {
                active = false;
            }
        }

        public void draw(Graphics g) {
            g.setColor(Color.CYAN);  // Couleur différente pour les projectiles adverses
            g.fillRect(x, y, 5, 15);
        }

        public Rectangle getHitbox() {
            return new Rectangle(x, y, 5, 15);
        }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public ClientManager(String playerName, int shipType) {
        this.playerName = playerName;
        this.shipType = shipType;
        this.matchId = GestionBaseDonnees.getNextMatchId();
    }

    public void setGamePanel(Bouclejeu gamePanel) {
        this.gamePanel = gamePanel;
    }

    public boolean connectToServer(String serverAddress) {
        try {
            socket = new Socket(serverAddress, 5555);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Envoyer les infos du joueur au serveur
            GameMessage joinMessage = GameMessage.createJoinMessage(playerName, shipType, matchId);
            out.writeObject(joinMessage);
            out.flush();

            // Attendre la réponse du serveur
            GameMessage response = (GameMessage) in.readObject();
            if (response.getChatContent().equals("NAME_ACCEPTED")) {
                connected = true;

                // Démarrer un thread pour écouter les messages du serveur
                listenerThread = new Thread(this::listenForMessages);
                listenerThread.setDaemon(true);
                listenerThread.start();

                System.out.println("Successfully connected to server as " + playerName);
                return true;
            } else {
                System.out.println("Connection failed: Name already exists or server error");
                closeConnection();
                return false;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Connection error: " + e.getMessage());
            closeConnection();
            return false;
        }
    }

    private void listenForMessages() {
        try {
            while (connected) {
                try {
                    GameMessage message = (GameMessage) in.readObject();
                    if (message == null) {
                        System.err.println("Received null message from server");
                        break;
                    }

                    switch (message.getType()) {
                        case PLAYER_JOIN:
                            String newPlayerName = message.getPlayerName();
                            if (!newPlayerName.equals(playerName)) {
                                onlinePlayers.add(newPlayerName);
                                remotePlayers.put(newPlayerName, new RemotePlayer(newPlayerName, message.getShipType()));
                            }
                            break;

                        case PLAYER_POSITION:
                            String posPlayerName = message.getPlayerName();
                            RemotePlayer player = remotePlayers.get(posPlayerName);
                            if (player != null) {
                                player.update(message.getX(), message.getY(), message.getHealth(), message.getScore());
                                // If remote player died, check if we're the last one standing
                                if (message.getHealth() <= 0 && gamePanel != null) {
                                    boolean allOthersDead = true;
                                    for (RemotePlayer otherPlayer : remotePlayers.values()) {
                                        if (otherPlayer.getHealth() > 0) {
                                            allOthersDead = false;
                                            break;
                                        }
                                    }
                                    if (allOthersDead) {
                                        gamePanel.handleRemoteGameOver(true, message.getScore());
                                    }
                                }
                            } else if (!posPlayerName.equals(playerName)) {
                                remotePlayers.put(posPlayerName, new RemotePlayer(posPlayerName, 0));
                                onlinePlayers.add(posPlayerName);
                            }
                            break;

                        case PLAYER_SHOOT:
                            if (!message.getPlayerName().equals(playerName)) {
                                remoteProjectiles.add(new RemoteProjectile(
                                        message.getProjectileX(), message.getProjectileY()));
                            }
                            break;

                        case GAME_OVER:
                            if (gamePanel != null) {
                                gamePanel.handleRemoteGameOver(message.isWinner(),message.getScore());
                            }
                            break;

                        case CHAT_MESSAGE:
                            String chatMsg = message.getPlayerName() + ": " + message.getChatContent();
                            addChatMessage(chatMsg);
                            if (message.getPlayerName().equals("SYSTEM") &&
                                    message.getChatContent().startsWith("Online players:")) {
                                updateOnlinePlayers(message.getChatContent());
                            }
                            break;

                        default:
                            System.err.println("Unknown message type received: " + message.getType());
                            break;
                    }
                } catch (EOFException e) {
                    System.err.println("Connection closed by server");
                    break;
                } catch (SocketException e) {
                    System.err.println("Socket error: " + e.getMessage());
                    break;
                } catch (IOException e) {
                    System.err.println("IO error: " + e.getMessage());
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid message format received");
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal error in message listener: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void updateOnlinePlayers(String message) {
        String[] parts = message.split(":");
        if (parts.length > 1) {
            String[] players = parts[1].split(",");
            for (String player : players) {
                String trimmedPlayer = player.trim();
                if (!trimmedPlayer.isEmpty() && !trimmedPlayer.equals(playerName)) {
                    onlinePlayers.add(trimmedPlayer);
                    // Si c'est un joueur qu'on ne connaît pas encore, on l'ajoute
                    if (!remotePlayers.containsKey(trimmedPlayer)) {
                        remotePlayers.put(trimmedPlayer, new RemotePlayer(trimmedPlayer, 0));
                    }
                }
            }
        }
    }

    public void sendPosition(int x, int y, int health, int score) {
        if (connected && out != null) {
            try {
                GameMessage posMsg = GameMessage.createPositionMessage(playerName, x, y, health, score, matchId);
                out.writeObject(posMsg);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending position: " + e.getMessage());
            }
        }
    }

    public void sendProjectile(int x, int y) {
        if (connected && out != null) {
            try {
                GameMessage shootMsg = GameMessage.createShootMessage(playerName, x, y, matchId);
                out.writeObject(shootMsg);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending projectile: " + e.getMessage());
            }
        }
    }

    public void sendChatMessage(String content) {
        if (connected && out != null) {
            try {
                GameMessage chatMsg = GameMessage.createChatMessage(playerName, content, matchId);
                out.writeObject(chatMsg);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending chat message: " + e.getMessage());
            }
        }
    }

    public void sendHitMessage(String targetPlayerName) {
        if (connected && out != null) {
            try {
                GameMessage hitMsg = GameMessage.createHitMessage(targetPlayerName, matchId);
                out.writeObject(hitMsg);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending hit message: " + e.getMessage());
            }
        }
    }

    public void sendGameOver(boolean isWinner) {
        if (connected && out != null) {
            try {
                // Get the current score from the game panel
                int finalScore = gamePanel != null ? gamePanel.getScore() : 0;
                GameMessage gameOverMsg = GameMessage.createGameOverMessage(playerName, isWinner, finalScore, matchId);
                out.writeObject(gameOverMsg);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending game over message: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (connected) {
            closeConnection();
        }
    }

    private void closeConnection() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
        socket = null;
        out = null;
        in = null;
    }

    public synchronized void addChatMessage(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 10) {
            chatMessages.remove(0);
        }
    }

    public synchronized List<String> getChatMessages() {
        return new ArrayList<>(chatMessages);
    }

    public Set<String> getOnlinePlayers() {
        return new HashSet<>(onlinePlayers);
    }

    public Collection<RemotePlayer> getRemotePlayers() {
        return remotePlayers.values();
    }

    public List<RemoteProjectile> getRemoteProjectiles() {
        return new ArrayList<>(remoteProjectiles);
    }

    public void updateRemoteProjectiles() {
        remoteProjectiles.removeIf(p -> !p.isActive());
        for (RemoteProjectile proj : remoteProjectiles) {
            proj.update();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public int getMatchId() {
        return matchId;
    }
}