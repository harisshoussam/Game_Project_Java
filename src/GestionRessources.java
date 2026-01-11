
import javax.imageio.ImageIO;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class GestionRessources {
    private static final Map<String, Image> images = new HashMap<>();
    private static final Map<String, Clip> sounds = new HashMap<>();

    public static void preloadResources() {
        loadImage("/background.png");
        loadImage("/game_icon.png");

        for (int i = 0; i < 3; i++) {
            loadImage("/ship_" + i + ".png");
            loadImage("/enemy_" + (i == 0 ? "basic" : i == 1 ? "fast" : "tank") + ".png");
        }

    }

    public static Image getImage(String filename) {
        if (!images.containsKey(filename)) {
            loadImage(filename);
        }
        return images.getOrDefault(filename, createPlaceholderImage());
    }


    private static void loadImage(String filename) {
        try {
            BufferedImage image = ImageIO.read(GestionRessources.class.getResourceAsStream( filename));
            images.put(filename, image);
        } catch (Exception e) {
            System.err.println("Could not load image: " + filename);
            images.put(filename, createPlaceholderImage());
        }
    }


    private static Image createPlaceholderImage() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.MAGENTA);
        g2d.fillRect(0, 0, 50, 50);
        g2d.setColor(Color.BLACK);
        g2d.drawString("X", 20, 30);
        g2d.dispose();
        return img;
    }
}