import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;

public class Bouclejeu extends JPanel {
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private int score = 0;
    private final Random random = new Random();
    private int spawnTimer = 0;

    private Joueur player;
    private final Image background;
    private int backgroundY = 0;
    private int scrollSpeed = 2;
    private final Gestionniveux pp;
    private boolean isLevelTransition = false;
    private long transitionStartTime;
    private boolean gameOver = false;
    private final Image playerLifeIcon;
    private final String playerName;
    private final int initialDifficulty;
    private final int shipType;
    private final FenetreJeu parent;
    private ClientManager clientManager;
    private boolean isMultiplayer = false;
    private String serverAddress = "localhost";
    private Timer gameTimer;
    private final List<String> chatMessages = new ArrayList<>();
    private boolean pvpMode = false;

    private static int maxPlayersEver = 0;

    private boolean isWinner = false;
    private int finalScore = 0;

    public Bouclejeu(FenetreJeu parent, String playerName, int difficulty, int shipType, boolean isMultiplayer) {
        this.parent = parent;
        this.playerName = playerName;
        this.initialDifficulty = difficulty;
        this.shipType = shipType;
        this.isMultiplayer = isMultiplayer;
        this.pp = new Gestionniveux(difficulty);
        this.pvpMode = isMultiplayer;

        // Initialisation des ressources qui ne dépendent pas de la connexion
        this.background = GestionRessources.getImage("/background.png");
        this.playerLifeIcon = GestionRessources.getImage("/ship_" + shipType + ".png")
                .getScaledInstance(30, 36, Image.SCALE_SMOOTH);

        if (isMultiplayer) {
            clientManager = new ClientManager(playerName, shipType);
            if (!clientManager.connectToServer(serverAddress)) {
                JOptionPane.showMessageDialog(this, "Échec de connexion au serveur ou nom déjà pris", "Erreur", JOptionPane.ERROR_MESSAGE);
                parent.showMenu();
                return;
            }
            clientManager.setGamePanel(this);
        }

        // Initialisation du joueur après avoir vérifié la connexion
        this.player = new Joueur(380, 450, shipType);  // Local player stays at bottom

        setFocusable(true);
        setupKeyListeners();
        startGameLoop();
    }

    private void handleChatInput() {
        String message = JOptionPane.showInputDialog(this, "Entrez votre message:");
        if (message != null && !message.trim().isEmpty()) {
            if (isMultiplayer) {
                clientManager.sendChatMessage(message);
            } else {
                addChatMessage("Vous: " + message);
            }
        }
    }

    private void addChatMessage(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 10) {
            chatMessages.remove(0);
        }
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isMovementKey(e.getKeyCode())) {
                    player.handleKeyRelease(e.getKeyCode());
                }
            }
        });
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_T) {
            handleChatInput();
            return;
        }
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                parent.showMenu();
            }
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE && player.canShoot()) {
            Projectile projectile = new Projectile(player.getCenterX(), player.getY());
            projectiles.add(projectile);
            player.shoot();

            if (isMultiplayer) {
                clientManager.sendProjectile(player.getCenterX(), player.getY());
            }
        } else if (isMovementKey(e.getKeyCode())) {
            player.handleKeyPress(e.getKeyCode());
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            parent.showMenu();
        }
    }

    private boolean isMovementKey(int keyCode) {
        return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT ||
                keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
    }

    private void startGameLoop() {
        gameTimer = new Timer(16, e -> {
            if (!gameOver) {
                updateGame();
            }
            repaint();
        });
        gameTimer.start();
    }

    private void updateGame() {
        if (isLevelTransition) {
            if (System.currentTimeMillis() - transitionStartTime > 2000) {
                isLevelTransition = false;
                pp.levelUp();
                scrollSpeed = 2 + pp.getCurrentLevel() / 3;
            }
            return;
        }

        player.update();

        if (isMultiplayer) {
            clientManager.sendPosition(player.getX(), player.getY(), player.getHealth(), score);
            clientManager.updateRemoteProjectiles();
        }

        updateBackground();

        if (!isMultiplayer && ++spawnTimer >= pp.getAdjustedSpawnInterval()) {
            spawnEnemy();
            spawnTimer = 0;
        }

        enemies.forEach(e -> e.update(scrollSpeed));
        projectiles.forEach(Projectile::update);

        handleCollisions();

        enemies.removeIf(e -> !e.isAlive() || e.isOutOfScreen(getHeight()));
        projectiles.removeIf(p -> !p.isActive());

        if (!isMultiplayer && pp.isLevelCompleted()) {
            isLevelTransition = true;
            transitionStartTime = System.currentTimeMillis();
        }
    }

    private void updateBackground() {
        backgroundY += scrollSpeed;
        if (backgroundY >= getHeight()) {
            backgroundY = 0;
        }
    }

    private void spawnEnemy() {
        int baseSpeed = pp.getEnemySpeed();
        int type = random.nextInt(3); // 0: basic, 1: fast, 2: tank
        enemies.add(new Enemy(
                random.nextInt(getWidth() - 50),
                -50,
                baseSpeed,
                type));
    }
    private void handleCollisions() {
        // Collisions entre projectiles locaux et ennemis
        new ArrayList<>(enemies).forEach(enemy -> {
            new ArrayList<>(projectiles).forEach(projectile -> {
                if (projectile.isActive() && enemy.isAlive() &&
                        projectile.getHitbox().intersects(enemy.getHitbox())) {
                    enemy.takeDamage(1);
                    projectile.setActive(false);

                    if (!enemy.isAlive()) {
                        score += (enemy.getType() == 0) ? 10 :
                                (enemy.getType() == 1) ? 15 : 30;
                        pp.enemyDefeated();
                    }
                }
            });
        });

        // Collisions entre joueur local et ennemis
        new ArrayList<>(enemies).forEach(enemy -> {
            if (enemy.isAlive() && enemy.getHitbox().intersects(player.getHitbox())) {
                enemy.takeDamage(enemy.getMaxHealth());
                player.takeDamage();
                checkGameOver();
            }
        });

        // En mode multijoueur, vérifier les collisions
        if (isMultiplayer) {
            // Collisions entre projectiles locaux et joueurs distants (PvP)
            if (pvpMode) {
                new ArrayList<>(projectiles).forEach(projectile -> {
                    for (ClientManager.RemotePlayer remotePlayer : clientManager.getRemotePlayers()) {
                        // Collision detection uses mirrored coordinates
                        if (projectile.isActive() && projectile.getHitbox().intersects(remotePlayer.getHitbox())) {
                            projectile.setActive(false);
                            // Send hit message to server (only player name needed)
                            clientManager.sendHitMessage(remotePlayer.getName());
                        }
                    }
                });
            }

            // Collisions entre projectiles distants et joueur local
            new ArrayList<>(clientManager.getRemoteProjectiles()).forEach(projectile -> {
                if (projectile.isActive() && projectile.getHitbox().intersects(player.getHitbox())) {
                    projectile.setActive(false);
                    player.takeDamage();
                    checkGameOver();
                }
            });
        }
    }

    private void checkGameOver() {
        if (player.isDead()) {
            gameOver = true;
            if (isMultiplayer) {
                clientManager.sendGameOver(false);
            }
        }
    }

    private void resetGame() {
        player = new Joueur(380, 450, shipType);
        enemies.clear();
        projectiles.clear();
        score = 0;
        gameOver = false;
        pp.reset();

        if (isMultiplayer) {
            // Réinitialiser les données multijoueur
            clientManager.getRemoteProjectiles().clear();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Dessiner l'arrière-plan avec effet de défilement
        g.drawImage(background, 0, backgroundY - getHeight(), getWidth(), getHeight(), null);
        g.drawImage(background, 0, backgroundY, getWidth(), getHeight(), null);

        // Dessiner les ennemis
        enemies.forEach(e -> e.draw(g));

        // Dessiner les projectiles locaux
        projectiles.forEach(p -> p.draw(g));

        // En mode multijoueur, dessiner les joueurs distants et leurs projectiles
        if (isMultiplayer) {
            for (ClientManager.RemotePlayer remotePlayer : clientManager.getRemotePlayers()) {
                remotePlayer.draw(g);
            }

            for (ClientManager.RemoteProjectile remoteProjectile : clientManager.getRemoteProjectiles()) {
                remoteProjectile.draw(g);
            }

            // Dessiner les messages de chat
            drawChatMessages(g);
        }

        // Dessiner le joueur local
        player.draw(g);

        // Afficher le score et le niveau
        drawHUD(g);

        // Afficher les transitions de niveau
        if (isLevelTransition) {
            drawLevelTransition(g);
        }

        // Afficher l'écran de fin de jeu
        if (gameOver) {
            drawGameOver(g);
        }
    }

    private void drawChatMessages(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 10, 300, 150);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));

        List<String> messages = isMultiplayer ? clientManager.getChatMessages() : chatMessages;
        int y = 30;
        for (String message : messages) {
            g.drawString(message, 20, y);
            y += 15;
        }

        g.setColor(Color.YELLOW);
        g.drawString("Appuyez sur T pour discuter", 20, 160);
    }

    private void drawHUD(Graphics g) {
        // Dessiner le score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, getHeight() - 50);

        // Dessiner le niveau
        g.setColor(Color.YELLOW);
        g.drawString("Niveau: " + pp.getCurrentLevel(), 20, getHeight() - 20);

        // Dessiner les vies restantes
        for (int i = 0; i < player.getHealth(); i++) {
            g.drawImage(playerLifeIcon, getWidth() - 40 - (i * 35), getHeight() - 40, null);
        }

        // En mode multijoueur, afficher les joueurs en ligne
        if (isMultiplayer) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Joueurs en ligne: " + (clientManager.getOnlinePlayers().size() + 1),
                    getWidth() - 200, 30);
        }
    }

    private void drawLevelTransition(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String message = "NIVEAU " + (pp.getCurrentLevel() + 1);
        int stringWidth = g.getFontMetrics().stringWidth(message);
        g.drawString(message, (getWidth() - stringWidth) / 2, getHeight() / 2);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());

        if (isMultiplayer) {
            if (isWinner) {
                // Winner screen
                g.setColor(Color.GREEN);
                g.setFont(new Font("Arial", Font.BOLD, 40));
                String message = "VICTORY!";
                int stringWidth = g.getFontMetrics().stringWidth(message);
                g.drawString(message, (getWidth() - stringWidth) / 2, getHeight() / 2 - 40);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                String scoreMsg = "Score final: " + finalScore;
                stringWidth = g.getFontMetrics().stringWidth(scoreMsg);
                g.drawString(scoreMsg, (getWidth() - stringWidth) / 2, getHeight() / 2 + 10);
            } else {
                // Loser screen
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 40));
                String message = "GAME OVER";
                int stringWidth = g.getFontMetrics().stringWidth(message);
                g.drawString(message, (getWidth() - stringWidth) / 2, getHeight() / 2 - 40);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                String scoreMsg = "Score final: " + finalScore;
                stringWidth = g.getFontMetrics().stringWidth(scoreMsg);
                g.drawString(scoreMsg, (getWidth() - stringWidth) / 2, getHeight() / 2 + 10);
            }
        } else {
            // Single player game over screen
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            String message = "GAME OVER";
            int stringWidth = g.getFontMetrics().stringWidth(message);
            g.drawString(message, (getWidth() - stringWidth) / 2, getHeight() / 2 - 40);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            String scoreMsg = "Score final: " + finalScore;
            stringWidth = g.getFontMetrics().stringWidth(scoreMsg);
            g.drawString(scoreMsg, (getWidth() - stringWidth) / 2, getHeight() / 2 + 10);
        }

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        String restartMsg = "Appuyez sur ENTRÉE pour retourner au menu principal";
        int stringWidth = g.getFontMetrics().stringWidth(restartMsg);
        g.drawString(restartMsg, (getWidth() - stringWidth) / 2, getHeight() / 2 + 50);
    }

    public void cleanupMultiplayer() {
        if (isMultiplayer && clientManager != null) {
            clientManager.disconnect();
        }
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }

    // Méthode utilitaire pour déterminer si un point est dans la zone de jeu
    private boolean isInBounds(int x, int y) {
        return x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight();
    }

    // Méthode pour régler le mode PvP
    public void setPvpMode(boolean pvpMode) {
        this.pvpMode = pvpMode;
    }

    // Méthode pour obtenir le score actuel
    public int getScore() {
        return score;
    }

    // Méthode pour obtenir le joueur
    public Joueur getPlayer() {
        return player;
    }

    // Méthode pour changer l'adresse du serveur (si besoin de se connecter à un autre serveur)
    public void setServerAddress(String address) {
        this.serverAddress = address;
        if (isMultiplayer && clientManager != null) {
            clientManager.disconnect();
            clientManager = new ClientManager(playerName, shipType);
            if (!clientManager.connectToServer(serverAddress)) {
                JOptionPane.showMessageDialog(this, "Échec de connexion au serveur", "Erreur", JOptionPane.ERROR_MESSAGE);
                parent.showMenu();
            }
        }
    }
    public void cleanUp() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (isMultiplayer && clientManager != null) {
            clientManager.disconnect();
        }
    }

    public void handleRemoteGameOver(boolean isWinner, int finalScore) {
        gameOver = true;
        this.isWinner = isWinner;
        this.finalScore = finalScore;
        if (isMultiplayer) {
            if (isWinner) {
                // We're the winner, send game over message to server
                clientManager.sendGameOver(true);
            }
        }
    }
}