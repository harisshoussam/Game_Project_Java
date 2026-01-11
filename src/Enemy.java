// Enemy.java
import java.awt.*;

public class Enemy {
    private int x, y;
    private int width, height;
    private int speedY;
    private Image image;
    private boolean alive = true;
    private int type;
    private int health;
    private int maxHealth;

    public int getMaxHealth() {
        return maxHealth;
    }

    public Enemy(int startX, int startY, int baseSpeed, int type) {
        this.x = startX;
        this.y = startY;
        this.type = type;

        switch(type) {
            case 0: // Basic
                this.image = GestionRessources.getImage("/enemy_basic.png");
                this.width = 40;
                this.height = 40;
                this.speedY = baseSpeed;
                this.health = 1;
                break;
            case 1: // Fast
                this.image = GestionRessources.getImage("/enemy_fast.png");
                this.width = 30;
                this.height = 30;
                this.speedY = baseSpeed + 2;
                this.health = 1;
                break;
            case 2: // Tank
                this.image = GestionRessources.getImage("/enemy_tank.png");
                this.width = 50;
                this.height = 50;
                this.speedY = baseSpeed - 1;
                this.health = 3;
                break;
            default:
                this.image = GestionRessources.getImage("/enemy_basic.png");
                this.width = 40;
                this.height = 40;
                this.speedY = baseSpeed;
                this.health = 1;
        }
        this.maxHealth = health;
    }

    public void update(int scrollSpeed) {
        y += speedY + scrollSpeed;
    }

    public boolean isOutOfScreen(int screenHeight) {
        return y > screenHeight;
    }

    public void draw(Graphics g) {
        if (alive) {
            g.drawImage(image, x, y, width, height, null);

            if (type == 2) {
                g.setColor(Color.RED);
                g.fillRect(x, y - 10, width, 5);
                g.setColor(Color.GREEN);
                int healthWidth = (int)(width * ((double)health / maxHealth));
                g.fillRect(x, y - 10, healthWidth, 5);
            }
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            alive = false;
        }
    }

    public Rectangle getHitbox() {
        return new Rectangle(x, y, width, height);
    }

    public boolean isAlive() { return alive; }
    public int getType() { return type; }
}