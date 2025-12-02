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
    // On stocke des tableaux d'images au lieu d'images simples
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    
    // Pour l'animation des PNJ, on utilise un timer global simple
    private int animationFrame = 0;
    private long lastTime = 0;

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        chargerSpriteSheet("Insecateur", "/resources/Insecateur.png"); // Nom exact du fichier !
        chargerSpriteSheet("Scarabrute", "/resources/Scarabrute.png");
        // chargerSpriteSheet("Giratina", "/resources/Giratina_GaucheSF.png");
    }
    
    private void chargerSpriteSheet(String cle, String chemin) {
        try {
            BufferedImage planche = ImageIO.read(getClass().getResource(chemin));
            // Découpage 2x4
            BufferedImage[][] tuiles = new BufferedImage[2][4];
            for (int col = 0; col < 2; col++) {
                for (int lig = 0; lig < 4; lig++) {
                    tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
                }
            }
            sprites.put(cle, tuiles);
        } catch (Exception e) {
            System.err.println("Erreur Sprite PNJ : " + chemin);
        }
    }

    public void miseAJour() {
        // Animation : change toutes les 500ms pour les PNJ
        if (System.currentTimeMillis() - lastTime > 500) {
            animationFrame = (animationFrame + 1) % 2;
            lastTime = System.currentTimeMillis();
        }
        
        // Déplacement aléatoire SQL
         try {
           Connection connexion = SingletonJDBC.getInstance().getConnection();
           PreparedStatement requete = connexion.prepareStatement(
                   "UPDATE pokemons SET longitude = longitude + 0.00001 * (FLOOR(RAND()*3)-1), latitude = latitude + 0.00001 * (FLOOR(RAND()*3)-1)");
           requete.executeUpdate();
           requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
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

                BufferedImage[][] sheet = sprites.get(espece);

                if (sheet != null) {
                    // Pour les PNJ, on affiche par défaut l'animation "Vers le bas" (Col 0, Lignes 2 et 3)
                    // car on ne connait pas leur direction exacte facilement
                    int col = 0;
                    int lig = 2 + animationFrame; 
                    
                    contexte.drawImage(sheet[col][lig], x - 16, y - 16, 32, 32, null);
                } else {
                     // Fallback
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}