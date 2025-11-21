package pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import outils.SingletonJDBC;

/**
 * Exemple de classe lutin
 *
 * @author guillaume.laurent
 */
public class Pokemons {

    protected Carte laCarte;

    public Pokemons(Carte carte) {
        this.laCarte = carte;
    }

    public void miseAJour() {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement(
                    "UPDATE pokemons "
                            + "SET longitude = longitude + 0.00001 * (FLOOR(RAND()*3)-1)"
                            + ", latitude = latitude + 0.00001 * (FLOOR(RAND()*3)-1)");
            requete.executeUpdate();
            requete.close();
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }                        
        // ajouter une requete ici...

    }

    public void rendu(Graphics2D contexte) {

        try {

            Connection connexion = SingletonJDBC.getInstance().getConnection();

            PreparedStatement requete = connexion.prepareStatement("SELECT id, espece, latitude, longitude, visible FROM pokemons;");
            ResultSet resultat = requete.executeQuery();
            while (resultat.next()) {

                int id = resultat.getInt("id");
                String espece = resultat.getString("espece");
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                boolean visible = resultat.getBoolean("visible");
                //System.out.println(espece + " = (" + latitude + "; " + longitude + ")");

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                if (visible) {
                    contexte.setColor(Color.BLUE);
                } else {
                    contexte.setColor(Color.GRAY);
                }

                contexte.fillOval(x - 5, y - 5, 10, 10);
                contexte.drawString(espece + " #" + id, x + 5, y - 5);
            }
            
            requete = connexion.prepareStatement(
                "SELECT dresseurs.latitude, dresseurs.longitude, pokemons.latitude, pokemons.longitude, visible"
                + " FROM dresseurs INNER JOIN pokemons "
                + " ON dresseurs.pseudo = pokemons.proprietaire;");
            resultat = requete.executeQuery();
            while (resultat.next()) {
                
                int x1 = laCarte.longitudeEnPixel(resultat.getDouble("dresseurs.longitude"));
                int y1 = laCarte.latitudeEnPixel(resultat.getDouble("dresseurs.latitude"));
                int x2 = laCarte.longitudeEnPixel(resultat.getDouble("pokemons.longitude"));
                int y2 = laCarte.latitudeEnPixel(resultat.getDouble("pokemons.latitude"));
                boolean visible = resultat.getBoolean("visible");
                if (visible) {
                    contexte.setColor(Color.BLUE);
                } else {
                    contexte.setColor(Color.GRAY);
                }contexte.drawLine(x1, y1, x2, y2);
            }

            requete.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
