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
public class Dresseurs {

    protected Carte laCarte;

    public Dresseurs(Carte laCarte) {
        this.laCarte = laCarte;
    }

    public void miseAJour() {
    }

    public void rendu(Graphics2D contexte) {

        try {

            Connection connexion = SingletonJDBC.getInstance().getConnection();

            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude FROM dresseurs;");
            ResultSet resultat = requete.executeQuery();
            while (resultat.next()) {
                
                String pseudo = resultat.getString("pseudo");
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                //System.out.println(pseudo + " = (" + latitude + "; " + longitude + ")");
                
                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                contexte.setColor(Color.red);
                contexte.fillOval(x - 5, y - 5, 10, 10);
                contexte.drawString(pseudo, x + 5, y - 5);
            }

            requete.close();           

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
