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
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    
    private int animationFrame = 0;
    private long lastTime = 0;
    private int directionAleatoire = 1;

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        chargerPlanche("Cizayox", "/resources/Cizayox.png");
        chargerPlanche("Scarabrute", "/resources/Scarabrute.png");
    }
    
    private void chargerPlanche(String espece, String chemin) {
        try {
            BufferedImage planche = ImageIO.read(getClass().getResource(chemin));
            BufferedImage[][] tuiles = new BufferedImage[2][4];
            for (int col = 0; col < 2; col++) {
                for (int lig = 0; lig < 4; lig++) {
                    tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
                }
            }
            sprites.put(espece, tuiles);
        } catch (Exception e) {
            System.err.println("Erreur Sprite PNJ ("+espece+") : " + chemin);
        }
    }

    public void miseAJour() {
        if (System.currentTimeMillis() - lastTime > 1000) {
            animationFrame = (animationFrame + 1) % 2;
            directionAleatoire = (int)(Math.random() * 4);
            lastTime = System.currentTimeMillis();
        }
        
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

                BufferedImage[][] planche = sprites.get(espece);

                if (planche != null) {
                    int col = 0; 
                    int lig = 0;
                    // Animation aléatoire pour les PNJ
                    switch(directionAleatoire) {
                        case 0: col = 0; lig = 0 + animationFrame; break; 
                        case 1: col = 0; lig = 2 + animationFrame; break; 
                        case 2: col = 1; lig = 0 + animationFrame; break; 
                        case 3: col = 1; lig = 2 + animationFrame; break; 
                    }
                    contexte.drawImage(planche[col][lig], x - 16, y - 16, 32, 32, null);
                } else {
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}