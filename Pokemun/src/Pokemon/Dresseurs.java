package pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Dresseurs {

    protected Carte laCarte;
    // 1. On déclare les variables pour stocker les images
    private BufferedImage spriteChasseur;
    private BufferedImage spriteInsecte;

    public Dresseurs(Carte laCarte) {
        this.laCarte = laCarte;
        
        // 2. On charge les images UNE SEULE FOIS au démarrage
        try {
            // Le chemin commence par / car c'est à la racine des sources
            this.spriteChasseur = ImageIO.read(getClass().getResource("/resources/Giratina_GaucheSF.png"));
            this.spriteInsecte = ImageIO.read(getClass().getResource("/resources/Giratina_GaucheSF.png"));
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Erreur chargement images Dresseurs : " + ex.getMessage());
        }
    }

    public void miseAJour() {
        // Pas de logique ici pour l'instant (géré par la BDD)
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            // On récupère aussi le ROLE pour savoir quelle image afficher
            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude, role FROM dresseurs;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String role = resultat.getString("role"); // "CHASSEUR" ou "INSECTE"

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // 3. On choisit l'image selon le rôle
                BufferedImage imgAffiche = null;
                if ("CHASSEUR".equals(role)) imgAffiche = spriteChasseur;
                else imgAffiche = spriteInsecte;

                // 4. Dessin de l'image (centrée)
                if (imgAffiche != null) {
                    // On dessine en 32x32 pixels, centré sur x,y
                    contexte.drawImage(imgAffiche, x - 16, y - 16, 64, 64, null);
                } else {
                    // Fallback (si image non trouvée) : on garde les points
                    contexte.setColor(Color.BLUE);
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
                
                // Affichage du pseudo au-dessus
                contexte.setColor(Color.WHITE);
                contexte.drawString(pseudo, x - 10, y - 20);
            }
            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}