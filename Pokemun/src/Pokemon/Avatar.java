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

public class Avatar {

    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    private String pseudo;
    private String role; 
    protected Carte laCarte;
    private BufferedImage monSprite;
    
    private final double VITESSE_LAT = 0.000015; 
    private final double VITESSE_LON = 0.000045; 

    public Avatar(Carte laCarte, String pseudoJoueur) {
        this.laCarte = laCarte;
        this.pseudo = pseudoJoueur; 
        
        // 1. Récupération du rôle depuis la table JOUEURS
        recupererRole();

        // 2. Chargement de l'image spécifique au joueur
        try {
            String nomImage = "";
            if ("Sacha".equalsIgnoreCase(pseudo)) {
                nomImage = "/resources/Dresseur.png";
            } else {
                // Attention à bien avoir Drascore.png et Libegon.png dans resources
                nomImage = "/resources/" + pseudo + ".png"; 
            }
            this.monSprite = ImageIO.read(getClass().getResource(nomImage));
        } catch (Exception ex) {
            System.err.println("Erreur chargement image Avatar (" + pseudo + ") : " + ex.getMessage());
        }
    }

    private void recupererRole() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            // CORRECTION ICI : table 'joueurs'
            PreparedStatement req = con.prepareStatement("SELECT role FROM joueurs WHERE pseudo = ?");
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
        double deltaLat = 0;
        double deltaLon = 0;

        if (toucheHaut) deltaLat += VITESSE_LAT;
        if (toucheBas) deltaLat -= VITESSE_LAT;
        if (toucheDroite) deltaLon += VITESSE_LON;
        if (toucheGauche) deltaLon -= VITESSE_LON;

        if (deltaLat != 0 || deltaLon != 0) {
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                // CORRECTION ICI : table 'joueurs'
                PreparedStatement req = connexion.prepareStatement(
                        "UPDATE joueurs SET latitude = latitude + ?, longitude = longitude + ?, derniereConnexion = NOW() WHERE pseudo = ?");
                req.setDouble(1, deltaLat);
                req.setDouble(2, deltaLon);
                req.setString(3, this.pseudo);
                req.executeUpdate();
                req.close();
                
                if ("DRESSEUR".equals(this.role)) {
                    tenterCapture(connexion);
                }
                
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void tenterCapture(Connection connexion) throws SQLException {
        // CORRECTION ICI : table 'joueurs'
        PreparedStatement reqPos = connexion.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?");
        reqPos.setString(1, this.pseudo);
        ResultSet res = reqPos.executeQuery();
        
        if (res.next()) {
            double maLat = res.getDouble("latitude");
            double maLon = res.getDouble("longitude");
            
            // CORRECTION ICI : table 'joueurs'
            PreparedStatement reqCapture = connexion.prepareStatement(
                "UPDATE joueurs SET statut = 'CAPTURE' " +
                "WHERE role = 'POKEMON' AND statut = 'LIBRE' " +
                "AND ABS(latitude - ?) < 0.0002 AND ABS(longitude - ?) < 0.0002"
            );
            reqCapture.setDouble(1, maLat);
            reqCapture.setDouble(2, maLon);
            int captures = reqCapture.executeUpdate();
            
            if (captures > 0) {
                System.out.println("🎉 BRAVO ! Vous avez capturé " + captures + " Pokémon(s) !");
            }
            reqCapture.close();
        }
        reqPos.close();
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            // CORRECTION ICI : table 'joueurs'
            PreparedStatement requete = connexion.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?");
            requete.setString(1, pseudo);
            ResultSet resultat = requete.executeQuery();
            
            if (resultat.next()) {
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                
                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                
                if (monSprite != null) {
                    contexte.drawImage(monSprite, x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback
                    contexte.setColor(Color.RED);
                    contexte.fillOval(x - 10, y - 10, 20, 20);
                }
                
                // Pseudo (Optionnel)
                // contexte.setColor(Color.WHITE);
                // contexte.drawString(pseudo, x - 15, y - 20);
            }
            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    // Setters touches inchangés
    public void setToucheHaut(boolean etat) { this.toucheHaut = etat; }
    public void setToucheBas(boolean etat) { this.toucheBas = etat; }
    public void setToucheGauche(boolean etat) { this.toucheGauche = etat; }
    public void setToucheDroite(boolean etat) { this.toucheDroite = etat; }
}