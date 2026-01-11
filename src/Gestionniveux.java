// LevelManager.java
import java.io.Serializable;

public class Gestionniveux implements Serializable {
    private int currentLevel;
    private int enemiesDefeated;
    private int enemiesToNextLevel;
    private boolean levelCompleted;
    private final int difficulty;

    private static final int BASE_ENEMIES_REQUIRED = 5;
    private static final int BASE_SPAWN_INTERVAL = 60;
    private static final int MIN_SPAWN_INTERVAL = 15;
    private static final int BASE_ENEMY_SPEED = 1;

    public Gestionniveux(int difficulty) {
        this.difficulty = difficulty;
        reset();
    }

    public void reset() {
        this.currentLevel = 1;
        this.enemiesDefeated = 0;
        this.enemiesToNextLevel = calculateEnemiesRequired();
        this.levelCompleted = false;
    }

    public void enemyDefeated() {
        enemiesDefeated++;
        if (enemiesDefeated >= enemiesToNextLevel) {
            levelCompleted = true;
        }
    }

    public void levelUp() {
        currentLevel++;
        enemiesDefeated = 0;
        enemiesToNextLevel = calculateEnemiesRequired();
        levelCompleted = false;
    }

    private int calculateEnemiesRequired() {
        return BASE_ENEMIES_REQUIRED + (currentLevel * 2);
    }

    public int getAdjustedSpawnInterval() {
        int interval = BASE_SPAWN_INTERVAL - (currentLevel * 3) - (difficulty * 2);
        return Math.max(MIN_SPAWN_INTERVAL, interval);
    }

    public int getEnemySpeed() {
        return BASE_ENEMY_SPEED + (currentLevel / 2) + (difficulty / 2);
    }

    public boolean isLevelCompleted() { return levelCompleted; }
    public int getCurrentLevel() { return currentLevel; }
    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getEnemiesToNextLevel() { return enemiesToNextLevel; }
    public int getDifficulty() { return difficulty; }
}
