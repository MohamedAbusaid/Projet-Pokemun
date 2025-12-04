package pokemon;

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

public class Dresseurs {

    protected Carte laCarte;
    private String pseudoLocal;
    
    // Dictionnaire pour les images normales (Tableaux 2D)
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    
    // Dictionnaire pour les images CAPTURE (Tableaux 2D aussi maintenant !)
    private HashMap<String, BufferedImage[][]> spritesCapture = new HashMap<>();

    // Mémoire pour l'animation
    private HashMap<String, EtatJoueur> memoire = new HashMap<>();

    public Dresseurs(Carte laCarte, String pseudoLocal) {
        this.laCarte = laCarte;
        this.pseudoLocal = pseudoLocal;
        
        // 1. Chargement des planches NORMALES
        chargerPlanche(sprites, "Dresseur", "/resources/Dresseur.png");
        chargerPlanche(sprites, "Drascore", "/resources/Drascore.png");
        chargerPlanche(sprites, "Libegon", "/resources/Libegon.png");
        
        // 2. Chargement des planches CAPTURE (On utilise la même logique de découpage)
        // Assurez-vous que ces images sont bien des planches 2x4 (64x128px)
        chargerPlanche(spritesCapture, "Drascore", "/resources/Drascore_Capture.png");
        chargerPlanche(spritesCapture, "Libegon", "/resources/Libegon_Capture.png");
    }
    
    // Méthode générique pour charger et découper une planche dans une Map donnée
    private void chargerPlanche(HashMap<String, BufferedImage[][]> destination, String role, String chemin) {
        try {
            BufferedImage planche = ImageIO.read(getClass().getResource(chemin));
            BufferedImage[][] tuiles = new BufferedImage[2][4];
            
            // Découpage de la grille 2x4 (32x32 par case)
            for (int col = 0; col < 2; col++) {
                for (int lig = 0; lig < 4; lig++) {
                    tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
                }
            }
            // On stocke le tableau découpé dans la Map choisie
            destination.put(role, tuiles);
            
        } catch (Exception e) {
            System.err.println("Erreur image Dresseurs (" + role + ") : " + chemin);
        }
    }

    public void miseAJour() { }

public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude, role, statut FROM joueurs;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                if (pseudo.equals(this.pseudoLocal)) continue; // On ne s'affiche pas soi-même
                
                String statut = resultat.getString("statut");
                boolean estCapture = "CAPTURE".equals(statut);

                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String role = resultat.getString("role");

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // =========================================================
                // 1. DÉTECTION DU SAUT (L'ASTUCE EST ICI)
                // =========================================================
                // On regarde sur quelle tuile se trouve ce joueur
                int idTuile = laCarte.getTuileID(latitude, longitude);
                
                // Si c'est un rebord, on considère qu'il est en train de sauter
                boolean enSaut = (idTuile == 3 || idTuile == 31 || idTuile == 32);
                // =========================================================

                // --- CALCUL ANIMATION (Mouvement fluide) ---
                EtatJoueur etat = memoire.get(pseudo);
                if (etat == null) {
                    etat = new EtatJoueur(x, y);
                    memoire.put(pseudo, etat);
                }
                int direction = etat.direction;
                boolean bouge = false;
                
                // Détection du mouvement pour animer les jambes
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
                
                // --- DESSIN AVEC EFFET ZOOM ---
                if (plancheActive != null) {
                    int col = 0; 
                    int lig = 0;
                    
                    // Sélection du sprite de base
                    switch(direction) {
                        case 0: col = 0; lig = 0 + etat.etapeAnimation; break;
                        case 1: col = 0; lig = 2 + etat.etapeAnimation; break;
                        case 2: col = 1; lig = 0 + etat.etapeAnimation; break;
                        case 3: col = 1; lig = 2 + etat.etapeAnimation; break;
                    }
                    
                    // =====================================================
                    // 2. APPLICATION DE LA TAILLE XXL
                    // =====================================================
                    int taille = 32; // Taille normale
                    int offset = 0;  // Décalage normal
                    
                    if (enSaut) {
                        taille = 54;  // ZOOM XXL (Même taille que toi)
                        offset = -11; // Même décalage que toi
                        
                        // Petit bonus visuel : Quand ils sautent, on fige les jambes
                        // pour ne pas qu'ils aient l'air de courir en l'air
                        if (direction == 0 || direction == 1) lig = 2; 
                    }
                    // =====================================================

                    contexte.drawImage(plancheActive[col][lig], 
                        (x - 16) + offset, 
                        (y - 16) + offset, 
                        taille,            
                        taille,            
                        null);
                        
                } else {
                    // Fallback (Carré gris si pas d'image)
                    contexte.setColor(Color.GRAY);
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
                
                // Affichage du Pseudo
                if (estCapture) contexte.setColor(Color.RED);
                else contexte.setColor(Color.WHITE);
                
                int largeurTexte = contexte.getFontMetrics().stringWidth(pseudo);
                contexte.drawString(pseudo, x - (largeurTexte / 2), y - 20);
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
    
    private class EtatJoueur {
        int lastX, lastY;
        int direction = 1; 
        int etapeAnimation = 0;
        public EtatJoueur(int x, int y) { this.lastX = x; this.lastY = y; }
    }
}