import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class FenetreJeu extends JFrame {
    private MenuPrincipal menuPanel;
    private Bouclejeu gamePanel;
    private HighscorePanel highscorePanel;
    private MultiplayerPanel multiplayerPanel;

    public FenetreJeu() {
        setTitle("Space Defender");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look and feel: " + e.getMessage());
        }

        showMenu();
        setVisible(true);
    }

    public void showMenu() {
        cleanUpCurrentPanel();
        menuPanel = new MenuPrincipal(this);
        switchToPanel(menuPanel);
    }

    public void startGame(String playerName, int difficulty, int shipType, boolean isMultiplayer) {
        startGame(playerName, difficulty, shipType, isMultiplayer, "localhost");
    }

    public void startGame(String playerName, int difficulty, int shipType, boolean isMultiplayer, String serverAddress) {
        cleanUpCurrentPanel();
        gamePanel = new Bouclejeu(this, playerName, difficulty, shipType, isMultiplayer);
        if (isMultiplayer) {
            // Définir l'adresse du serveur si c'est différent de localhost
            if (!serverAddress.equals("localhost")) {
                gamePanel.setServerAddress(serverAddress);
            }
        }
        switchToPanel(gamePanel);
    }

    public void showHighscores() {
        cleanUpCurrentPanel();
        highscorePanel = new HighscorePanel(this);
        switchToPanel(highscorePanel);
    }

    public void showMultiplayerMenu() {
        cleanUpCurrentPanel();
        multiplayerPanel = new MultiplayerPanel(this);
        switchToPanel(multiplayerPanel);
    }

    public void startMultiplayerServer() {
        // Option pour démarrer le serveur de jeu
        try {
            // Démarrer le serveur dans un thread séparé
            new Thread(() -> {
                try {
                    Server.main(new String[]{});
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Erreur lors du démarrage du serveur: " + e.getMessage(),
                            "Erreur serveur", JOptionPane.ERROR_MESSAGE);
                }
            }).start();

            JOptionPane.showMessageDialog(this,
                    "Serveur démarré avec succès sur le port 5555.\nLes autres joueurs peuvent se connecter à votre IP.",
                    "Serveur démarré", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de démarrer le serveur: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cleanUpCurrentPanel() {
        if (gamePanel != null) {
            gamePanel.cleanupMultiplayer(); // Assurez-vous que cette méthode existe et nettoie correctement les ressources réseau
            remove(gamePanel);
            gamePanel = null;
        }
        if (menuPanel != null) {
            remove(menuPanel);
            menuPanel = null;
        }
        if (highscorePanel != null) {
            remove(highscorePanel);
            highscorePanel = null;
        }
        if (multiplayerPanel != null) {
            remove(multiplayerPanel);
            multiplayerPanel = null;
        }
    }

    private void switchToPanel(JPanel panel) {
        add(panel);
        revalidate();
        repaint();
        panel.requestFocusInWindow();
    }

    // Classe pour le menu multijoueur
    private static class MultiplayerPanel extends JPanel {
        private JTextField nameField;
        private JTextField serverField;
        private JComboBox<String> shipSelector;
        private JComboBox<String> difficultySelector;

        public MultiplayerPanel(FenetreJeu parent) {
            setLayout(new BorderLayout());
            setBackground(new Color(30, 30, 50));

            JLabel title = new JLabel("MODE MULTIJOUEUR", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 36));
            title.setForeground(new Color(255, 215, 0));
            title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            add(title, BorderLayout.NORTH);

            // Panel pour les options de jeu
            JPanel formPanel = new JPanel();
            formPanel.setLayout(new GridLayout(0, 2, 10, 10));
            formPanel.setBackground(new Color(30, 30, 50));
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

            // Champ nom du joueur
            JLabel nameLabel = new JLabel("Nom du joueur:");
            nameLabel.setForeground(Color.WHITE);
            nameField = new JTextField("Joueur");

            // Champ adresse du serveur
            JLabel serverLabel = new JLabel("Adresse du serveur:");
            serverLabel.setForeground(Color.WHITE);
            serverField = new JTextField("localhost");

            // Sélecteur de vaisseau
            JLabel shipLabel = new JLabel("Vaisseau:");
            shipLabel.setForeground(Color.WHITE);
            shipSelector = new JComboBox<>(new String[]{"Vaisseau 1", "Vaisseau 2", "Vaisseau 3"});

            // Sélecteur de difficulté
            JLabel difficultyLabel = new JLabel("Difficulté:");
            difficultyLabel.setForeground(Color.WHITE);
            difficultySelector = new JComboBox<>(new String[]{"Facile", "Normal", "Difficile"});

            // Ajouter les composants au formulaire
            formPanel.add(nameLabel);
            formPanel.add(nameField);
            formPanel.add(serverLabel);
            formPanel.add(serverField);
            formPanel.add(shipLabel);
            formPanel.add(shipSelector);
            formPanel.add(difficultyLabel);
            formPanel.add(difficultySelector);

            add(formPanel, BorderLayout.CENTER);

            // Panel pour les boutons
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(new Color(30, 30, 50));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));

            // Bouton pour rejoindre une partie
            JButton joinButton = new JButton("REJOINDRE UNE PARTIE");
            styleButton(joinButton, new Color(50, 205, 50));
            joinButton.addActionListener(e -> {
                String playerName = nameField.getText().trim();
                String serverAddress = serverField.getText().trim();
                int shipType = shipSelector.getSelectedIndex();
                int difficulty = difficultySelector.getSelectedIndex() + 1;

                if (playerName.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "Veuillez entrer un nom de joueur", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                parent.startGame(playerName, difficulty, shipType, true, serverAddress);
            });

            // Bouton pour héberger une partie
            JButton hostButton = new JButton("HÉBERGER UNE PARTIE");
            styleButton(hostButton, new Color(70, 130, 180));
            hostButton.addActionListener(e -> {
                parent.startMultiplayerServer();
                // On ne démarre pas le jeu tout de suite, le joueur devra cliquer sur "Rejoindre"
                // avec "localhost" comme adresse de serveur
            });

            // Bouton pour revenir au menu principal
            JButton backButton = new JButton("RETOUR");
            styleButton(backButton, new Color(178, 34, 34));
            backButton.addActionListener(e -> parent.showMenu());

            buttonPanel.add(hostButton);
            buttonPanel.add(joinButton);
            buttonPanel.add(backButton);

            add(buttonPanel, BorderLayout.SOUTH);
        }
    }

    private static class HighscorePanel extends JPanel {
        public HighscorePanel(FenetreJeu parent) {
            setLayout(new BorderLayout());
            setBackground(new Color(30, 30, 50));

            JLabel title = new JLabel("HIGH SCORES", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 36));
            title.setForeground(new Color(255, 215, 0));
            title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            add(title, BorderLayout.NORTH);

            JTextArea scoresArea = new JTextArea();
            scoresArea.setEditable(false);
            scoresArea.setBackground(new Color(30, 30, 50));
            scoresArea.setForeground(Color.WHITE);
            scoresArea.setFont(new Font("Arial", Font.PLAIN, 18));

            // Utilisez DatabaseManager au lieu de HighscoreManager
            List<String> highscores = GestionBaseDonnees.getHighScores(10); // 10 meilleurs scores
            if (highscores.isEmpty()) {
                scoresArea.setText("No scores recorded yet");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < highscores.size(); i++) {
                    // Formatage amélioré
                    sb.append(String.format("%2d. %s%n", i+1, highscores.get(i)));
                }
                scoresArea.setText(sb.toString());
            }

            JScrollPane scrollPane = new JScrollPane(scoresArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);

            JButton backButton = new JButton("BACK TO MENU");
            backButton.addActionListener(e -> parent.showMenu());
            styleButton(backButton, new Color(70, 130, 180));

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(new Color(30, 30, 50));
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }
    }

    private static void styleButton(JButton button, Color color) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
                button.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
                button.setForeground(Color.BLACK);
            }
        });
    }
}
