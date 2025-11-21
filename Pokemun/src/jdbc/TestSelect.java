/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author guillaume.laurent
 */
public class TestSelect {

    public static void main(String[] args) {

        try {

            Connection connexion = DriverManager.getConnection("jdbc:mariadb://nemrod.ens2m.fr:3306/tp_jdbc", "etudiant", "YTDTvj9TR3CDYCmP");

            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude FROM dresseurs");
            System.out.println(requete);
            ResultSet resultat = requete.executeQuery();
            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                System.out.println(pseudo + " = (" + latitude + "; " + longitude + ")");
            }

            requete.close();
            connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
