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
    
    // --- AJOUT : Référence vers le joueur local pour comparer les positions ---
    private Avatar monAvatar; 
    
    // Dictionnaire pour les images normales (Tableaux 2D)
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    
    // Dictionnaire pour les images CAPTURE
    private HashMap<String, BufferedImage[][]> spritesCapture = new HashMap<>();

    // Mémoire pour l'animation
    private HashMap<String, EtatJoueur> memoire = new HashMap<>();
    
    // Image de la µ-ball
    private BufferedImage muBallSprite;

    public Dresseurs(Carte laCarte, String pseudoLocal) {
        this.laCarte = laCarte;
        this.pseudoLocal = pseudoLocal;
        
        // 1. Chargement des planches NORMALES
        chargerPlanche(sprites, "Dresseur", "/resources/Dresseur.png");
        chargerPlanche(sprites, "Drascore", "/resources/Drascore.png");
        chargerPlanche(sprites, "Libegon", "/resources/Libegon.png");
        
        // 2. Chargement des planches CAPTURE
        chargerPlanche(spritesCapture, "Drascore", "/resources/Drascore_Capture.png");
        chargerPlanche(spritesCapture, "Libegon", "/resources/Libegon_Capture.png");
        
        // 3. Chargement de la µ-ball
        try {
            this.muBallSprite = ImageIO.read(getClass().getResource("/resources/mu_ball.png"));
        } catch (IOException e) {
            System.err.println("Erreur chargement Sprite μ-ball : " + e.getMessage());
        }
    }
    
    // --- AJOUT : Setter pour récupérer l'avatar local depuis Jeu.java ---
    public void setAvatar(Avatar avatar) {
        this.monAvatar = avatar;
    }

    // Méthode générique pour charger et découper une planche
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
            // --- 1. Récupération de VOS infos (Joueur Local) ---
            int maTuileID = 0;
            int monPixelX = 0;
            int monPixelY = 0;
            
            // On vérifie que monAvatar n'est pas null (au cas où setAvatar n'a pas été appelé)
            if (monAvatar != null) {
                maTuileID = laCarte.getTuileID(monAvatar.getLatitude(), monAvatar.getLongitude());
                monPixelX = laCarte.longitudeEnPixel(monAvatar.getLongitude());
                monPixelY = laCarte.latitudeEnPixel(monAvatar.getLatitude());
            }
            // ----------------------------------------------------

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
                
                // --- AJOUT : LOGIQUE DE VISIBILITÉ (Buissons ID 1) ---
                if (!estCapture) { // On applique la logique seulement si le joueur est libre
                    int tuileEnnemiID = laCarte.getTuileID(latitude, longitude);
                    boolean ennemiCache = (tuileEnnemiID == 1);
                    boolean jeSuisCache = (maTuileID == 1);

                    if (ennemiCache) {
                        boolean visible = false;
                        // Si je suis AUSSI dans un buisson
                        if (jeSuisCache) {
                            // Calcul distance (Pythagore)
                            double distance = Math.sqrt(Math.pow(x - monPixelX, 2) + Math.pow(y - monPixelY, 2));
                            // Si proche (< 150px soit environ 5 cases), je le vois
                            if (distance < 45) {
                                visible = true;
                            }
                        }
                        
                        // Si pas visible, on passe au joueur suivant (on ne le dessine pas)
                        if (!visible) continue;
                    }
                }
                // -----------------------------------------------------

                // --- CALCUL ANIMATION ---
                EtatJoueur etat = memoire.get(pseudo);
                if (etat == null) {
                    etat = new EtatJoueur(x, y);
                    memoire.put(pseudo, etat);
                }
                int direction = etat.direction;
                boolean bouge = false;
                if (x > etat.lastX) { direction = 3; bouge = true; }
                else if (x < etat.lastX) { direction = 2; bouge = true; }
                else if (y > etat.lastY) { direction = 1; bouge = true; }
                else if (y < etat.lastY) { direction = 0; bouge = true; }

                if (bouge) {
                    etat.etapeAnimation = (etat.etapeAnimation + 1) % 2;
                    etat.direction = direction;
                } else {
                    etat.etapeAnimation = 0;
                }
                etat.lastX = x;
                etat.lastY = y;

                // --- CHOIX DE LA PLANCHE ---
                BufferedImage[][] plancheActive = null;
                if (estCapture) {
                    plancheActive = spritesCapture.get(role);
                } else {
                    plancheActive = sprites.get(role);
                }

                // --- DESSIN ---
                if (plancheActive != null) {
                    int col = 0; 
                    int lig = 0;
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

                // Affichage Pseudo
                if (estCapture) contexte.setColor(Color.RED);
                else contexte.setColor(Color.WHITE);

                int largeurTexte = contexte.getFontMetrics().stringWidth(pseudo);
                contexte.drawString(pseudo, x - (largeurTexte / 2), y - 20);
            }
            requete.close();

            // --- Rendu des Attaques (μ-balls)---
            PreparedStatement reqAttaques = connexion.prepareStatement(
                "SELECT lat_actuelle, lon_actuelle FROM attaques WHERE type = 'MUBALL';"
            );
            ResultSet resultatAttaques = reqAttaques.executeQuery();

            if (muBallSprite != null) {
                int taille = 8;
                while (resultatAttaques.next()) {
                    double latitude = resultatAttaques.getDouble("lat_actuelle");
                    double longitude = resultatAttaques.getDouble("lon_actuelle");
                    int x = laCarte.longitudeEnPixel(longitude);
                    int y = laCarte.latitudeEnPixel(latitude);
                    contexte.drawImage(muBallSprite, x - taille/2, y - taille/2, taille, taille, null);
                }
            } else {
                contexte.setColor(Color.CYAN); 
                while (resultatAttaques.next()) {
                    double latitude = resultatAttaques.getDouble("lat_actuelle");
                    double longitude = resultatAttaques.getDouble("lon_actuelle");
                    int x = laCarte.longitudeEnPixel(longitude);
                    int y = laCarte.latitudeEnPixel(latitude);
                    contexte.fillOval(x - 5, y - 5, 10, 10);
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