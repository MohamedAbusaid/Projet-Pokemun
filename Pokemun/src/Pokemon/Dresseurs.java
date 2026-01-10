package Pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;
import java.io.IOException;

public class Dresseurs {

    protected Carte laCarte;
    private String pseudoLocal;
    
    // Ajout Affichage : Référence au joueur local pour calculer la distance dans les buissons
    private Avatar monAvatar; 
    
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    private HashMap<String, BufferedImage[][]> spritesCapture = new HashMap<>();
    private HashMap<String, EtatJoueur> memoire = new HashMap<>();
    private BufferedImage muBallSprite;

    public Dresseurs(Carte laCarte, String pseudoLocal) {
        this.laCarte = laCarte;
        this.pseudoLocal = pseudoLocal;
        
        chargerPlanche(sprites, "Dresseur", "/resources/Dresseur.png");
        chargerPlanche(sprites, "Drascore", "/resources/Drascore.png");
        chargerPlanche(sprites, "Libegon", "/resources/Libegon.png");
        
        chargerPlanche(spritesCapture, "Drascore", "/resources/Drascore_Capture.png");
        chargerPlanche(spritesCapture, "Libegon", "/resources/Libegon_Capture.png");
        
        try {
            this.muBallSprite = ImageIO.read(getClass().getResource("/resources/mu_ball.png"));
        } catch (IOException e) {
            System.err.println("Erreur chargement Sprite mu-ball : " + e.getMessage());
        }
    }
    
    public void setAvatar(Avatar avatar) {
        this.monAvatar = avatar;
    }

    private void chargerPlanche(HashMap<String, BufferedImage[][]> destination, String role, String chemin) {
        try {
            BufferedImage planche = ImageIO.read(getClass().getResource(chemin));
            BufferedImage[][] tuiles = new BufferedImage[2][4];
            for (int col = 0; col < 2; col++) {
                for (int lig = 0; lig < 4; lig++) {
                    tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
                }
            }
            destination.put(role, tuiles);
        } catch (Exception e) {
            System.err.println("Erreur image Dresseurs (" + role + ") : " + chemin);
        }
    }

    public void miseAJour() { }

    public void rendu(Graphics2D contexte) {
        try {
            // 1. Infos Joueur Local (Pour la visibilité Buissons)
            int maTuileID = 0;
            int monPixelX = 0;
            int monPixelY = 0;
            if (monAvatar != null) {
                maTuileID = laCarte.getTuileID(monAvatar.getLatitude(), monAvatar.getLongitude());
                monPixelX = laCarte.longitudeEnPixel(monAvatar.getLongitude());
                monPixelY = laCarte.latitudeEnPixel(monAvatar.getLatitude());
            }

            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude, role, statut FROM joueurs;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                if (pseudo.equals(this.pseudoLocal)) continue; 

                String statut = resultat.getString("statut");
                boolean estCapture = "CAPTURE".equals(statut);
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String role = resultat.getString("role");

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // --- MERGE: LOGIQUE VISIBILITÉ (Branche Affichage) ---
                if (!estCapture) {
                    int tuileEnnemiID = laCarte.getTuileID(latitude, longitude);
                    boolean ennemiCache = (tuileEnnemiID == 1); // ID 1 = Buisson
                    boolean jeSuisCache = (maTuileID == 1);

                    if (ennemiCache) {
                        boolean visible = false;
                        // Si je suis aussi caché, je peux voir les ennemis proches
                        if (jeSuisCache) {
                            double distance = Math.sqrt(Math.pow(x - monPixelX, 2) + Math.pow(y - monPixelY, 2));
                            if (distance < 45) visible = true;
                        }
                        if (!visible) continue; // Si pas visible, on saute ce joueur
                    }
                }

                // --- ANIMATION (Commun) ---
                EtatJoueur etat = memoire.get(pseudo);
                if (etat == null) {
                    etat = new EtatJoueur(x, y);
                    memoire.put(pseudo, etat);
                }
                int direction = etat.direction;
                if (x > etat.lastX) direction = 3;
                else if (x < etat.lastX) direction = 2;
                else if (y > etat.lastY) direction = 1;
                else if (y < etat.lastY) direction = 0;

                if (x != etat.lastX || y != etat.lastY) {
                    etat.etapeAnimation = (etat.etapeAnimation + 1) % 2;
                    etat.direction = direction;
                } else {
                    etat.etapeAnimation = 0;
                }
                etat.lastX = x;
                etat.lastY = y;

                // --- DESSIN (Commun) ---
                BufferedImage[][] plancheActive = estCapture ? spritesCapture.get(role) : sprites.get(role);
                if (plancheActive != null) {
                    int col = 0; int lig = 0;
                    switch(direction) {
                        case 0: col = 0; lig = 0 + etat.etapeAnimation; break;
                        case 1: col = 0; lig = 2 + etat.etapeAnimation; break;
                        case 2: col = 1; lig = 0 + etat.etapeAnimation; break;
                        case 3: col = 1; lig = 2 + etat.etapeAnimation; break;
                    }
                    contexte.drawImage(plancheActive[col][lig], x - 16, y - 16, 32, 32, null);
                } else {
                    contexte.setColor(Color.GRAY);
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }

                contexte.setColor(estCapture ? Color.RED : Color.WHITE);
                int largeurTexte = contexte.getFontMetrics().stringWidth(pseudo);
                contexte.drawString(pseudo, x - (largeurTexte / 2), y - 20);
            }
            requete.close();

            // --- MERGE: DESSIN DES ATTAQUES (Branche Main) ---
            PreparedStatement reqAttaques = connexion.prepareStatement("SELECT lat_actuelle, lon_actuelle FROM attaques WHERE type = 'MUBALL';");
            ResultSet resAtt = reqAttaques.executeQuery();
            if (muBallSprite != null) {
                while (resAtt.next()) {
                    int px = laCarte.longitudeEnPixel(resAtt.getDouble("lon_actuelle"));
                    int py = laCarte.latitudeEnPixel(resAtt.getDouble("lat_actuelle"));
                    contexte.drawImage(muBallSprite, px - 4, py - 4, 8, 8, null);
                }
            } else {
                contexte.setColor(Color.CYAN);
                while (resAtt.next()) {
                    int px = laCarte.longitudeEnPixel(resAtt.getDouble("lon_actuelle"));
                    int py = laCarte.latitudeEnPixel(resAtt.getDouble("lat_actuelle"));
                    contexte.fillOval(px - 4, py - 4, 8, 8);
                }
            }
            reqAttaques.close();

        } catch (SQLException ex) { ex.printStackTrace(); }
    }
    
    private class EtatJoueur {
        int lastX, lastY;
        int direction = 1; 
        int etapeAnimation = 0;
        public EtatJoueur(int x, int y) { this.lastX = x; this.lastY = y; }
    }
}