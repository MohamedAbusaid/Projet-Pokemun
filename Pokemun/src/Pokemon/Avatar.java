package pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Avatar {

    // Contrôles
    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    private String pseudo;
    private String role; 
    protected Carte laCarte;

    // --- NOUVEAUX ATTRIBUTS POUR L'ANIMATION ---
    // Tableaux 2D pour stocker les morceaux d'images [colonne][ligne]
    private BufferedImage[][] spritesChasseur; 
    private BufferedImage[][] spritesInsecte;
    
    private int direction = 1; // 0=Haut, 1=Bas, 2=Gauche, 3=Droite (Par défaut Bas)
    private int etapeAnimation = 0; // 0 ou 1 (Pied gauche / Pied droit)
    private long dernierChangement = 0; // Chrono pour la vitesse d'animation
    // -------------------------------------------
    
    private final double VITESSE_LAT = 0.000015; 
    private final double VITESSE_LON = 0.000045; 

    public Avatar(Carte laCarte, String pseudoJoueur) {
        this.laCarte = laCarte;
        this.pseudo = pseudoJoueur; 
        
        recupererRole();

        // Chargement et DÉCOUPAGE des images
        try {
            // On charge les planches complètes (64x128)
            BufferedImage plancheChasseur = ImageIO.read(getClass().getResource("/resources/Chasseur.png"));
            BufferedImage plancheInsecte = ImageIO.read(getClass().getResource("/resources/Insecte.png"));
            
            // On les découpe en petits morceaux de 32x32
            this.spritesChasseur = decouperPlanche(plancheChasseur);
            this.spritesInsecte = decouperPlanche(plancheInsecte);
            
        } catch (Exception ex) {
            System.err.println("Erreur chargement images Avatar : " + ex.getMessage());
        }
    }

    // Méthode utilitaire pour découper une image 64x128 en grille 2x4
    private BufferedImage[][] decouperPlanche(BufferedImage planche) {
        BufferedImage[][] tuiles = new BufferedImage[2][4]; // 2 colonnes, 4 lignes
        for (int col = 0; col < 2; col++) {
            for (int lig = 0; lig < 4; lig++) {
                // x, y, largeur, hauteur
                tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
            }
        }
        return tuiles;
    }

    private void recupererRole() {
        // ... (Code SQL identique à avant) ...
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("SELECT role FROM joueurs WHERE pseudo = ?");
            req.setString(1, this.pseudo);
            ResultSet res = req.executeQuery();
            if (res.next()) this.role = res.getString("role");
            req.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    public void miseAJour() {
        double deltaLat = 0;
        double deltaLon = 0;
        boolean enMouvement = false;

        // --- GESTION DE LA DIRECTION ET DU MOUVEMENT ---
        if (toucheHaut) {
            deltaLat += VITESSE_LAT;
            direction = 0; // Haut
            enMouvement = true;
        } else if (toucheBas) {
            deltaLat -= VITESSE_LAT;
            direction = 1; // Bas
            enMouvement = true;
        } else if (toucheGauche) {
            deltaLon -= VITESSE_LON;
            direction = 2; // Gauche
            enMouvement = true;
        } else if (toucheDroite) {
            deltaLon += VITESSE_LON;
            direction = 3; // Droite
            enMouvement = true;
        }

        // --- GESTION DE L'ANIMATION ---
        if (enMouvement) {
            // On change d'image toutes les 200ms
            if (System.currentTimeMillis() - dernierChangement > 200) {
                etapeAnimation = (etapeAnimation + 1) % 2; // Alterne entre 0 et 1
                dernierChangement = System.currentTimeMillis();
            }
        } else {
            etapeAnimation = 0; // Reste statique si on ne bouge pas
        }

        // Mise à jour SQL (identique à avant)
        if (deltaLat != 0 || deltaLon != 0) {
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement req = connexion.prepareStatement(
                        "UPDATE joueurs SET latitude = latitude + ?, longitude = longitude + ?, derniereConnexion = NOW() WHERE pseudo = ?");
                req.setDouble(1, deltaLat);
                req.setDouble(2, deltaLon);
                req.setString(3, this.pseudo);
                req.executeUpdate();
                req.close();
                
                if ("DRESSEUR".equals(this.role)) tenterCapture(connexion);
                
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
    
    private void tenterCapture(Connection connexion) throws SQLException {
        // ... (Code capture identique à avant) ...
        // Copiez votre code existant ici
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT latitude, longitude, role FROM joueurs WHERE pseudo = ?");
            requete.setString(1, pseudo);
            ResultSet resultat = requete.executeQuery();
            
            if (resultat.next()) {
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                
                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                
                // --- CHOIX DE LA BONNE IMAGE DANS LA GRILLE ---
                BufferedImage[][] spritesActuels = null;
                if ("DRESSEUR".equals(this.role)) spritesActuels = spritesChasseur;
                else if ("POKEMON".equals(this.role)) spritesActuels = spritesInsecte;
                
                BufferedImage imgAffiche = null;
                if (spritesActuels != null) {
                    // Logique de mapping selon votre description :
                    // Col 0 : Haut (Lignes 0,1) / Bas (Lignes 2,3)
                    // Col 1 : Gauche (Lignes 0,1) / Droite (Lignes 2,3)
                    
                    int col = 0;
                    int lig = 0;
                    
                    switch(direction) {
                        case 0: // Haut
                            col = 0; 
                            lig = 0 + etapeAnimation; // Ligne 0 ou 1
                            break;
                        case 1: // Bas
                            col = 0;
                            lig = 2 + etapeAnimation; // Ligne 2 ou 3
                            break;
                        case 2: // Gauche
                            col = 1;
                            lig = 0 + etapeAnimation; // Ligne 0 ou 1
                            break;
                        case 3: // Droite
                            col = 1;
                            lig = 2 + etapeAnimation; // Ligne 2 ou 3
                            break;
                    }
                    
                    imgAffiche = spritesActuels[col][lig];
                }

                if (imgAffiche != null) {
                    contexte.drawImage(imgAffiche, x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback
                    contexte.setColor(Color.RED);
                    contexte.fillOval(x - 10, y - 10, 20, 20);
                }
                
                contexte.setColor(Color.WHITE);
                contexte.drawString(pseudo, x - 15, y - 20);
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // Setters... (identiques)
    public void setToucheHaut(boolean etat) { this.toucheHaut = etat; }
    public void setToucheBas(boolean etat) { this.toucheBas = etat; }
    public void setToucheGauche(boolean etat) { this.toucheGauche = etat; }
    public void setToucheDroite(boolean etat) { this.toucheDroite = etat; }
}