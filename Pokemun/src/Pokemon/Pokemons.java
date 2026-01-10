package Pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Pokemons {

    protected Carte laCarte;
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    
    // Liste locale pour stocker les pokémons en mémoire vive
    private List<PokemonEntity> listePokemons = new ArrayList<>();

    private int animationFrame = 0;
    private long lastTimeAnimation = 0;
    private long lastTimeMoveChoice = 0;
    
    // Calibration vitesse
    private double stepLon; 
    private double stepLat; 

    // --- CONSTANTES SAUT ---
    private final int DUREE_SAUT = 500; 
    private final double DISTANCE_SAUT = 0.00015;

    // Classe interne pour gérer l'état de chaque Pokémon
    private class PokemonEntity {
        String espece;
        double lat, lon;          
        double targetLat, targetLon; 
        boolean visible;
        int direction = 1; // 0=Bas, 1=Haut, 2=Droite, 3=Gauche

        // État du saut
        boolean enSaut = false;
        long debutSaut = 0;
        double latDepartSaut = 0;

        public PokemonEntity(String espece, double lat, double lon, boolean visible) {
            this.espece = espece;
            this.lat = lat;
            this.lon = lon;
            this.targetLat = lat; 
            this.targetLon = lon;
            this.visible = visible;
        }
    }

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        chargerPlanche("Cizayox", "/resources/Cizayox.png");
        chargerPlanche("Scarabrute", "/resources/Scarabrute.png");
        
        chargerDepuisBDD();
        calibrerVitesse();
    }
    
    private void chargerDepuisBDD() {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT espece, latitude, longitude, visible FROM pokemons;");
            ResultSet resultat = requete.executeQuery();
            while (resultat.next()) {
                listePokemons.add(new PokemonEntity(
                    resultat.getString("espece"),
                    resultat.getDouble("latitude"),
                    resultat.getDouble("longitude"),
                    resultat.getBoolean("visible")
                ));
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
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
            System.err.println("Erreur Sprite : " + chemin);
        }
    }
    
    private void calibrerVitesse() {
        double refLat = 20.0; double refLon = 5.0;
        if (!listePokemons.isEmpty()) { refLat = listePokemons.get(0).lat; refLon = listePokemons.get(0).lon; }
        double distanceCibleEnPixels = 32.0; 

        double testDelta = 0.001;
        int x1 = laCarte.longitudeEnPixel(refLon);
        int x2 = laCarte.longitudeEnPixel(refLon + testDelta);
        double distPixelX = Math.abs(x2 - x1);
        this.stepLon = (distPixelX > 0) ? (distanceCibleEnPixels * testDelta) / distPixelX : 0.0001;

        int y1 = laCarte.latitudeEnPixel(refLat);
        int y2 = laCarte.latitudeEnPixel(refLat + testDelta);
        double distPixelY = Math.abs(y2 - y1);
        this.stepLat = (distPixelY > 0) ? (distanceCibleEnPixels * testDelta) / distPixelY : 0.0001;
    }

    public void miseAJour() {
        long now = System.currentTimeMillis();

        // 1. ANIMATION
        if (now - lastTimeAnimation > 500) {
            animationFrame = (animationFrame + 1) % 2;
            lastTimeAnimation = now;
        }
        
        // 2. IA
        for (PokemonEntity p : listePokemons) {

            // A. SAUT (Prioritaire)
            if (p.enSaut) {
                long tempsEcoule = now - p.debutSaut;
                if (tempsEcoule >= DUREE_SAUT) {
                    p.enSaut = false;
                    p.lat = p.latDepartSaut - DISTANCE_SAUT; 
                    p.targetLat = p.lat; 
                    p.targetLon = p.lon;
                } else {
                    double progression = (double) tempsEcoule / DUREE_SAUT;
                    p.lat = p.latDepartSaut - (DISTANCE_SAUT * progression);
                }
                continue; 
            }

            // B. CHOIX ACTION
            if (now - lastTimeMoveChoice > 2000) {
                double offsetPieds = 0.00005;
                int tuileSousPieds = laCarte.getTuileID(p.lat - offsetPieds, p.lon);
                boolean surRebord = (tuileSousPieds == 31 || tuileSousPieds == 32 || tuileSousPieds == 3);

                int dir = (int)(Math.random() * 4);
                p.direction = dir;
                
                // C. DÉCLENCHEMENT SAUT (Uniquement vers le BAS)
                if (dir == 0 && surRebord) {
                    p.enSaut = true;
                    p.debutSaut = now;
                    p.latDepartSaut = p.lat;
                    continue; 
                }

                // D. DÉPLACEMENT NORMAL
                double testLat = p.lat;
                double testLon = p.lon;
                double facteurDistance = 2.0; 
                int directionCarte = 0; 

                switch(dir) {
                    case 0: // Bas
                         testLat = p.lat - (stepLat * facteurDistance); 
                         directionCarte = 1; // 1 = Bas dans Carte.java
                         break;
                    case 1: // Haut
                         testLat = p.lat + (stepLat * facteurDistance); 
                         directionCarte = 0; // 0 = Haut
                         break;
                    case 2: // Droite
                         testLon = p.lon + (stepLon * facteurDistance); 
                         directionCarte = 3; // 3 = Droite
                         break;
                    case 3: // Gauche
                         testLon = p.lon - (stepLon * facteurDistance); 
                         directionCarte = 2; // 2 = Gauche
                         break;
                }

                if (laCarte.estTraversable(testLat, testLon, directionCarte)) {
                    p.targetLat = testLat;
                    p.targetLon = testLon;
                } else {
                    p.targetLat = p.lat;
                    p.targetLon = p.lon;
                }
            }

            // E. LISSAGE
            double vitesse = 0.05; 
            p.lat += (p.targetLat - p.lat) * vitesse;
            p.lon += (p.targetLon - p.lon) * vitesse;
        }
        
        if (now - lastTimeMoveChoice > 2000) {
            lastTimeMoveChoice = now;
        }
    }

    public void rendu(Graphics2D contexte) {
        for (PokemonEntity p : listePokemons) {
            if (!p.visible) continue;

            int x = laCarte.longitudeEnPixel(p.lon);
            int y = laCarte.latitudeEnPixel(p.lat);

            // OMBRE & HAUTEUR SAUT
            int hauteurSaut = 0;
            if (p.enSaut) {
                long tempsEcoule = System.currentTimeMillis() - p.debutSaut;
                double progression = (double) tempsEcoule / DUREE_SAUT;
                hauteurSaut = (int) (20 * Math.sin(progression * Math.PI));
                
                contexte.setColor(new Color(0, 0, 0, 80));
                contexte.fillOval(x + 5, y + 28, 22, 10);
            }

            BufferedImage[][] planche = sprites.get(p.espece);

            if (planche != null) {
                int col = 0;
                int lig = 0;
                switch(p.direction) {
                    case 0: col = 0; lig = 2 + animationFrame; break; // Bas
                    case 1: col = 0; lig = 0 + animationFrame; break; // Haut
                    case 2: col = 1; lig = 2 + animationFrame; break; // Droite
                    case 3: col = 1; lig = 0 + animationFrame; break; // Gauche
                }
                contexte.drawImage(planche[col][lig], x - 16, (y - 16) - hauteurSaut, 32, 32, null);
            } else {
                contexte.setColor(Color.MAGENTA);
                contexte.fillOval(x - 5, y - 5, 10, 10);
            }
        }
    }
}