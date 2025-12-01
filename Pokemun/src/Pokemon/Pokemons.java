package pokemon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Pokemons {

    protected Carte laCarte;
    private HashMap<String, BufferedImage> sprites = new HashMap<>();

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        chargerImage("Insecateur", "/resources/Insecateur.png");
        chargerImage("Scarabrute", "/resources/Scarabrute.png");
    }
    
    private void chargerImage(String cle, String chemin) {
        try {
            sprites.put(cle, ImageIO.read(getClass().getResource(chemin)));
        } catch (Exception e) {
            System.err.println("Image PNJ manquante : " + chemin);
        }
    }

    public void miseAJour() {
        // ... (Code de déplacement aléatoire inchangé) ...
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT espece, latitude, longitude, visible FROM pokemons;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                if (!resultat.getBoolean("visible")) continue;

                String espece = resultat.getString("espece");
                int x = laCarte.longitudeEnPixel(resultat.getDouble("longitude"));
                int y = laCarte.latitudeEnPixel(resultat.getDouble("latitude"));

                BufferedImage img = sprites.get(espece);

                if (img != null) {
                    contexte.drawImage(img, x - 16, y - 16, 32, 32, null);
                }
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}