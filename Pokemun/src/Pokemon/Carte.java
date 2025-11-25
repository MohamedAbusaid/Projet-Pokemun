package pokemon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Exemple de classe carte
 *
 * @author guillaume.laurent
 */
public class Carte {

    protected BufferedImage decor;

    public static double LATITUDE_MAX = 47.251893;
    public static double LATITUDE_MIN = 47.248851;
    public static double LONGITUDE_MAX = 5.996778;
    public static double LONGITUDE_MIN = 5.988060;

    public Carte() {
        try {
            this.decor = ImageIO.read(getClass().getResource("../resources/Foret_de_Rambouillet.png"));
        } catch (IOException ex) {
            Logger.getLogger(Carte.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void miseAJour() {
    }

    public void rendu(Graphics2D contexte) {
        contexte.drawImage(this.decor, 0, 0, null);
    }

    public double getLargeur() {
        return decor.getWidth();
    }

    public double getHauteur() {
        return decor.getHeight();
    }

    public int longitudeEnPixel(double longitude) {
        int x = (int) (decor.getWidth() * (longitude - LONGITUDE_MIN) / (LONGITUDE_MAX - LONGITUDE_MIN));
        if (x < 5) {
            x = 5;
        }
        if (x > decor.getWidth() - 5) {
            x = decor.getWidth() - 5;
        }
        return x;
    }

    public int latitudeEnPixel(double latitude) {
        int y = decor.getHeight() - (int) (decor.getHeight() * (latitude - LATITUDE_MIN) / (LATITUDE_MAX - LATITUDE_MIN));
        if (y < 5) {
            y = 5;
        }
        if (y > decor.getHeight() - 5) {
            y = decor.getHeight() - 5;
        }
        return y;
    }

}
