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

public class Dresseurs {

    protected Carte laCarte;
    private String pseudoLocal;
    
    // Dictionnaire pour stocker toutes les images chargées
    // Clé = Nom (ex: "Sacha"), Valeur = Image
    private HashMap<String, BufferedImage> sprites = new HashMap<>();

    public Dresseurs(Carte laCarte, String pseudoLocal) {
        this.laCarte = laCarte;
        this.pseudoLocal = pseudoLocal;
        
        // On pré-charge toutes les images connues
        chargerImage("Sacha", "/resources/Dresseur.png");
        chargerImage("Drascore", "/resources/Drascore.png");
        chargerImage("Libegon", "/resources/Libegon.png");
        // Ajoutez d'autres joueurs ici si besoin
    }
    
    private void chargerImage(String cle, String chemin) {
        try {
            sprites.put(cle, ImageIO.read(getClass().getResource(chemin)));
        } catch (Exception e) {
            System.err.println("Image manquante : " + chemin);
        }
    }

    public void miseAJour() { }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude, statut FROM joueurs;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                if (pseudo.equals(this.pseudoLocal)) continue; 
                
                if ("CAPTURE".equals(resultat.getString("statut"))) continue; 

                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // On récupère l'image correspondant au pseudo
                BufferedImage img = sprites.get(pseudo);

                if (img != null) {
                    contexte.drawImage(img, x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback (point rouge) si joueur inconnu
                    contexte.setColor(java.awt.Color.RED);
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}