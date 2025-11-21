/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import outils.SingletonJDBC;

/**
 * Exemple de classe avatar
 *
 * @author guillaume.laurent
 */
public class Avatar {

    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    private String pseudo;
    protected Carte laCarte;
    
    private String messageCapture = "";

    public Avatar(Carte laCarte) {
        this.laCarte = laCarte;
        this.toucheHaut = false;
        this.toucheBas = false;
        this.toucheDroite = false;
        this.toucheGauche = false;
        this.pseudo = "sacha";

    }

    public void miseAJour() {

        if (this.toucheHaut) {
            System.out.println("Déplacement de " + this.pseudo + " vers le haut");
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement requete = connexion.prepareStatement(
                        "UPDATE dresseurs SET latitude = latitude + 0.0001 WHERE pseudo = ?");
                requete.setString(1, this.pseudo);
                requete.executeUpdate();
                requete.close();
            }
            catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        if (this.toucheBas) {
            System.out.println("Déplacement de " + this.pseudo + " vers le bas");
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement requete = connexion.prepareStatement(
                        "UPDATE dresseurs SET latitude = latitude - 0.0001 WHERE pseudo = ?");
                requete.setString(1, this.pseudo);
                requete.executeUpdate();
                requete.close();
            }
            catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        if (this.toucheDroite) {
            System.out.println("Déplacement de " + this.pseudo + " vers la droite");
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement requete = connexion.prepareStatement(
                        "UPDATE dresseurs SET longitude = longitude + 0.0001 WHERE pseudo = ?");
                requete.setString(1, this.pseudo);
                requete.executeUpdate();
                requete.close();
            }
            catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        if (this.toucheGauche) {
            System.out.println("Déplacement de " + this.pseudo + " vers la gauche");
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement requete = connexion.prepareStatement(
                        "UPDATE dresseurs SET longitude = longitude - 0.0001 WHERE pseudo = ?");
                requete.setString(1, this.pseudo);
                requete.executeUpdate();
                requete.close();
            }
            catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();

            // 1) Récupérer la position du joueur
            PreparedStatement reqPos = connexion.prepareStatement(
                "SELECT latitude, longitude FROM dresseurs WHERE pseudo = ?"
            );
            reqPos.setString(1, pseudo);
            ResultSet pos = reqPos.executeQuery();

            if (pos.next()) {
                double lat = pos.getDouble("latitude");
                double lon = pos.getDouble("longitude");

                // 2) Capturer tous les pokémons proches
                PreparedStatement reqCapture = connexion.prepareStatement(
                    "UPDATE pokemons " +
                    "SET proprietaire = ? " +
                    "WHERE ABS(latitude - ?) < 0.0001 " +
                    "AND ABS(longitude - ?) < 0.0001"
                );

                reqCapture.setString(1, pseudo);  // nouveau propriétaire
                reqCapture.setDouble(2, lat);
                reqCapture.setDouble(3, lon);

                int nbCaptures = reqCapture.executeUpdate();
                if (nbCaptures > 0) {
                    System.out.println("🎉 " + pseudo + " a capturé " + nbCaptures + " pokémon(s) !");
                }
                
                reqCapture.close();
                
                if (nbCaptures > 0) {
                    messageCapture = "🎉 " + pseudo + " a capturé " + nbCaptures + " pokémon(s) !";

                    // 🔥 Téléporte les pokémons capturés sur le joueur
                    PreparedStatement reqMove = connexion.prepareStatement(
                        "UPDATE pokemons SET latitude = ?, longitude = ? WHERE proprietaire = ?"
                    );
                    reqMove.setDouble(1, lat);
                    reqMove.setDouble(2, lon);
                    reqMove.setString(3, pseudo);
                    reqMove.executeUpdate();
                    reqMove.close();
                }
            }

            reqPos.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        
        this.toucheHaut = false;
        this.toucheBas = false;
        this.toucheDroite = false;
        this.toucheGauche = false;
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
                //System.out.println(pseudo + " = (" + latitude + "; " + longitude + ")");

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                contexte.setColor(Color.red);
                contexte.drawOval(x - 7, y - 7, 14, 14);
                //contexte.drawString(pseudo, x + 8, y - 8);
            }
            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void setToucheHaut(boolean etat) {
        this.toucheHaut = etat;
    }

    public void setToucheBas(boolean etat) {
        this.toucheBas = etat;
    }

    public void setToucheGauche(boolean etat) {
        this.toucheGauche = etat;
    }

    public void setToucheDroite(boolean etat) {
        this.toucheDroite = etat;
    }
    

}
