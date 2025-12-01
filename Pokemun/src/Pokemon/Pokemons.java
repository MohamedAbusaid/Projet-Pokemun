package pokemon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Pokemons {

    protected Carte laCarte;
    private BufferedImage spriteBuisson; // Pour se cacher
    private BufferedImage spriteRocher;  // Obstacle

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        try {
            this.spriteBuisson = ImageIO.read(getClass().getResource("/resources/Giratina_GaucheSF.png"));
            this.spriteRocher = ImageIO.read(getClass().getResource("/resources/Giratina_GaucheSF.png"));
            // Vous pouvez ajouter d'autres sprites ici (ex: Bonus, Pièges...)
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Erreur images Pokemons");
        }
    }

    public void miseAJour() {
        // Votre logique de déplacement aléatoire ici (déjà faite)
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT espece, latitude, longitude, visible FROM pokemons;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String espece = resultat.getString("espece");
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                boolean visible = resultat.getBoolean("visible");

                // Si l'objet n'est pas visible (ex: capturé), on ne le dessine pas
                if (!visible) continue; 

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                BufferedImage img = null;
                if ("Buisson".equals(espece)) img = spriteBuisson;
                else if ("Rocher".equals(espece)) img = spriteRocher;

                if (img != null) {
                    contexte.drawImage(img, x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback : point gris
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
            }
            requete.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}