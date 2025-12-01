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

/**
 * Avatar : gère l'affichage et le déplacement du joueur local.
 * @author Equipe DuBrazil
 */
public class Avatar {

    // Contrôles
    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    
    // Infos joueur
    private String pseudo;
    private String role; // "CHASSEUR" ou "INSECTE"
    
    // Graphismes
    protected Carte laCarte;
    private BufferedImage spriteChasseur;
    private BufferedImage spriteInsecte;
    
    // Paramètres de jeu
    private final double VITESSE = 0.0001; // Vitesse de déplacement (en degrés GPS)

    public Avatar(Carte laCarte, String pseudoJoueur) {
        this.laCarte = laCarte;
        this.pseudo = pseudoJoueur; 
        
        // Chargement des images (Sprites)
        try {
            // On charge les deux images, on décidera laquelle afficher dans le rendu
            // Assurez-vous que les fichiers sont bien dans src/resources/
            try { this.spriteChasseur = ImageIO.read(getClass().getResource("/resources/Giratina_GaucheSF.png")); } catch(Exception e){ System.err.println("Image chasseur manquante"); }
            try { this.spriteInsecte = ImageIO.read(getClass().getResource("/resources/Giratina_GaucheSF.png")); } catch(Exception e){ System.err.println("Image insecte manquante"); }
        } catch (Exception ex) {
            System.err.println("Erreur globale images : " + ex.getMessage());
        }

        // Récupération du rôle depuis la BDD au démarrage
        recupererRole();
    }

    // Récupère le rôle (CHASSEUR ou INSECTE) pour savoir quelle image afficher
    private void recupererRole() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("SELECT role FROM dresseurs WHERE pseudo = ?");
            req.setString(1, this.pseudo);
            ResultSet res = req.executeQuery();
            if (res.next()) {
                this.role = res.getString("role");
                System.out.println("Connecté en tant que : " + this.role);
            }
            req.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void miseAJour() {
        // Calcul du déplacement
        double deltaLat = 0;
        double deltaLon = 0;

        if (toucheHaut) deltaLat += VITESSE;
        if (toucheBas) deltaLat -= VITESSE;
        if (toucheDroite) deltaLon += VITESSE;
        if (toucheGauche) deltaLon -= VITESSE;

        // Si le joueur bouge, on met à jour la BDD
        if (deltaLat != 0 || deltaLon != 0) {
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement req = connexion.prepareStatement(
                        "UPDATE dresseurs SET latitude = latitude + ?, longitude = longitude + ?, derniereConnexion = NOW() WHERE pseudo = ?");
                req.setDouble(1, deltaLat);
                req.setDouble(2, deltaLon);
                req.setString(3, this.pseudo);
                req.executeUpdate();
                req.close();
                
                // Gestion spécifique : Si je suis CHASSEUR, je tente de capturer
                if ("CHASSEUR".equals(this.role)) {
                    tenterCapture(connexion);
                }
                
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    // Méthode spéciale pour le Chasseur : capture les insectes proches
    private void tenterCapture(Connection connexion) throws SQLException {
        // On cherche sa propre position
        PreparedStatement reqPos = connexion.prepareStatement(
            "SELECT latitude, longitude FROM dresseurs WHERE pseudo = ?"
        );
        reqPos.setString(1, this.pseudo);
        ResultSet res = reqPos.executeQuery();
        
        if (res.next()) {
            double maLat = res.getDouble("latitude");
            double maLon = res.getDouble("longitude");
            
            // Capture des insectes (Dresseurs avec role='INSECTE')
            // Rayon de capture : 0.0002 degrés (~20 mètres)
            PreparedStatement reqCapture = connexion.prepareStatement(
                "UPDATE dresseurs SET statut = 'CAPTURE' " +
                "WHERE role = 'INSECTE' AND statut = 'LIBRE' " +
                "AND ABS(latitude - ?) < 0.0002 AND ABS(longitude - ?) < 0.0002"
            );
            reqCapture.setDouble(1, maLat);
            reqCapture.setDouble(2, maLon);
            int captures = reqCapture.executeUpdate();
            
            if (captures > 0) {
                System.out.println("🎉 BRAVO ! Vous avez capturé " + captures + " insecte(s) !");
            }
            reqCapture.close();
        }
        reqPos.close();
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT latitude, longitude FROM dresseurs WHERE pseudo = ?");
            requete.setString(1, pseudo);
            ResultSet resultat = requete.executeQuery();
            
            if (resultat.next()) {
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                
                // Conversion GPS -> Pixels écran
                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                
                // Choix de l'image selon le rôle (récupéré au constructeur)
                BufferedImage imgAffiche = null;
                if ("CHASSEUR".equals(this.role)) imgAffiche = spriteChasseur;
                else if ("INSECTE".equals(this.role)) imgAffiche = spriteInsecte;
                
                // --- DESSIN ---
                //L'image est chargée, on la dessine centrée (32x32)
                contexte.drawImage(imgAffiche, x - 16, y - 16, 32, 32, null);
                
            }
            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Setters pour les touches (appelés par FenetreDeJeu)
    public void setToucheHaut(boolean etat) { this.toucheHaut = etat; }
    public void setToucheBas(boolean etat) { this.toucheBas = etat; }
    public void setToucheGauche(boolean etat) { this.toucheGauche = etat; }
    public void setToucheDroite(boolean etat) { this.toucheDroite = etat; }
}