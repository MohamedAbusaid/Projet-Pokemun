package pokemon;

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
    private double stepLon; // Combien de longitude pour avancer de X pixels
    private double stepLat; // Combien de latitude pour avancer de X pixels

    // Classe interne pour gérer l'état de chaque Pokémon
    private class PokemonEntity {
        String espece;
        double lat, lon;          // Position actuelle (celle qu'on dessine)
        double targetLat, targetLon; // Destination (là où il veut aller)
        boolean visible;
        int direction = 1;

        public PokemonEntity(String espece, double lat, double lon, boolean visible) {
            this.espece = espece;
            this.lat = lat;
            this.lon = lon;
            this.targetLat = lat; // Au début, destination = position
            this.targetLon = lon;
            this.visible = visible;
        }
    }

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        chargerPlanche("Cizayox", "/resources/Cizayox.png");
        chargerPlanche("Scarabrute", "/resources/Scarabrute.png");
        
        // On charge les données de la BDD une seule fois au début !
        chargerDepuisBDD();
        
        // IMPORTANT : On calcule les ratios maintenant qu'on a des données
        calibrerVitesse();
    }
    
    // Nouvelle méthode pour charger la BDD au démarrage
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
        // ... (Ton code de chargement d'image reste identique) ...
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
        // On prend un point de référence (par exemple le premier pokemon, ou 0,0)
        // Si la liste est vide, on prend des valeurs par défaut, sinon on prend le premier
        double refLat = 20.0; // Valeur par défaut arbitraire
        double refLon = 5.0;
    
        if (!listePokemons.isEmpty()) {
            refLat = listePokemons.get(0).lat;
            refLon = listePokemons.get(0).lon;
        }

        // On veut que le PNJ parcoure, disons, 32 pixels (1 case)
        double distanceCibleEnPixels = 32.0; 

        // 1. CALIBRATION LONGITUDE (X)
        // On cherche combien de longitude il faut pour faire 32px
        double testDelta = 0.001;
        int x1 = laCarte.longitudeEnPixel(refLon);
        int x2 = laCarte.longitudeEnPixel(refLon + testDelta);
        double distancePixelObtenueX = Math.abs(x2 - x1);

        // Règle de trois : Si 0.001 donne 'distancePixelObtenueX', combien faut-il pour '32' ?
        // Formule : (TargetPx * TestDelta) / MeasuredPx
        if (distancePixelObtenueX > 0) {
            this.stepLon = (distanceCibleEnPixels * testDelta) / distancePixelObtenueX;
        } else {
            this.stepLon = 0.0001; // Sécurité si division par zéro
        }

        // 2. CALIBRATION LATITUDE (Y)
        int y1 = laCarte.latitudeEnPixel(refLat);
        int y2 = laCarte.latitudeEnPixel(refLat + testDelta);
        double distancePixelObtenueY = Math.abs(y2 - y1);

        if (distancePixelObtenueY > 0) {
            this.stepLat = (distanceCibleEnPixels * testDelta) / distancePixelObtenueY;
        } else {
            this.stepLat = 0.0001;
        }
    
    System.out.println("Calibration terminée : StepLon=" + stepLon + " / StepLat=" + stepLat);
    }

    public void miseAJour() {
        long now = System.currentTimeMillis();

        // 1. GESTION DE l'ANIMATION (Les pieds qui bougent) - toutes les 500ms par ex
        if (now - lastTimeAnimation > 500) {
            animationFrame = (animationFrame + 1) % 2;
            lastTimeAnimation = now;
        }
        
        // 2. CHOIX D'UNE NOUVELLE DESTINATION
        if (now - lastTimeMoveChoice > 2000) {
            for (PokemonEntity p : listePokemons) {
                int dir = (int)(Math.random() * 4);
                p.direction = dir;
                
                // On réinitialise la cible
                p.targetLat = p.lat;
                p.targetLon = p.lon;

                // On utilise les variables calculées (stepLat / stepLon)
                // Note : stepLat/Lon représente 32 pixels (défini dans la calibration).
                // Si tu veux qu'il bouge de 3 cases, tu fais * 3. 
                // Si tu veux 1/2 case, tu fais * 0.5.
                double facteurDistance = 2.0; 

                switch(dir) {
                    case 0: // Bas
                         p.targetLat = p.lat - (stepLat * facteurDistance); 
                         break;
                    case 1: // Haut
                         p.targetLat = p.lat + (stepLat * facteurDistance); 
                         break;
                    case 2: // Droite
                         p.targetLon = p.lon + (stepLon * facteurDistance); 
                         break;
                    case 3: // Gauche
                         p.targetLon = p.lon - (stepLon * facteurDistance); 
                         break;
                }
            }
            lastTimeMoveChoice = now;
        }

        // 3. LE LISSAGE (INTERPOLATION) - S'exécute à chaque tour de boucle (60 fois/sec)
        double vitesse = 0.05; // 5% de la distance par frame (effet de glissement)
        
        for (PokemonEntity p : listePokemons) {
            // On rapproche la position actuelle (lat) de la cible (targetLat)
            p.lat += (p.targetLat - p.lat) * vitesse;
            p.lon += (p.targetLon - p.lon) * vitesse;
        }
    }

    public void rendu(Graphics2D contexte) {
        // Plus de SQL ici ! On lit directement la mémoire Java.
        for (PokemonEntity p : listePokemons) {
            if (!p.visible) continue;

            // On convertit les coordonnées lissées en pixels
            int x = laCarte.longitudeEnPixel(p.lon);
            int y = laCarte.latitudeEnPixel(p.lat);

            BufferedImage[][] planche = sprites.get(p.espece);

            if (planche != null) {
                int col = 0;
                int lig = 0;

                // Utilisation de la direction stockée dans l'entité
                switch(p.direction) {
                    case 0: col = 0; lig = 0 + animationFrame; break; // Bas
                    case 1: col = 0; lig = 2 + animationFrame; break; // Haut
                    case 2: col = 1; lig = 0 + animationFrame; break; // Droite
                    case 3: col = 1; lig = 2 + animationFrame; break; // Gauche
                }
                
                // Dessin centré (x - 16)
                contexte.drawImage(planche[col][lig], x - 16, y - 16, 32, 32, null);
            } else {
                contexte.fillOval(x - 5, y - 5, 10, 10);
            }
        }
    }
}