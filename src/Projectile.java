import java.awt.*;

public class Projectile {
    private int x, y;
    private final int speed = 10;
    private final int width = 5;
    private final int height = 15;
    private boolean active = true;
    private final Color color = Color.YELLOW;

    public Projectile(int startX, int startY) {
        this.x = startX - width / 2;
        this.y = startY;
    }

    public void update() {
        y -= speed;
        if (y < 0) {
            active = false;
        }
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

    public Rectangle getHitbox() {
        return new Rectangle(x, y, width, height);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}