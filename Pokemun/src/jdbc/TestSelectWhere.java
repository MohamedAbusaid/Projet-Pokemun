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
public class TestSelectWhere {

    public static void main(String[] args) {

        try {

            Connection connexion = DriverManager.getConnection("jdbc:mariadb://nemrod.ens2m.fr:3306/tp_jdbc", "etudiant", "YTDTvj9TR3CDYCmP");

            PreparedStatement requete = connexion.prepareStatement("SELECT proprietaire, espece FROM pokemons WHERE proprietaire = ?");
            requete.setString(1, "sacha");
            
            System.out.println(requete);
            ResultSet resultat = requete.executeQuery();
            while (resultat.next()) {
                String espece = resultat.getString("espece");
                String proprietaire = resultat.getString("proprietaire");
                System.out.println(espece + " " + proprietaire);
//                if (proprietaire.equals("sauvage")) {
//                    System.out.println(espece.substring(0,1).toUpperCase()+ espece.substring(1) 
//                            + " ".repeat(10-espece.length()) + " est encore sauvage !");
//                } 
//                else {
//                    System.out.println(espece.substring(0,1).toUpperCase() + espece.substring(1) 
//                            + " ".repeat(10-espece.length()) + " is " 
//                            + proprietaire.substring(0,1).toUpperCase() + proprietaire.substring(1) + "'s Pokemon");
//                }
            }

            requete.close();
            connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
