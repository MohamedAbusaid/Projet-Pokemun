/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdbc;

import outils.OutilsJDBC;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author guillaume.laurent
 */
public class TestSelectAllPokemonFromDresseurs {

    public static void main(String[] args) {

        try {

            Connection connexion = DriverManager.getConnection("jdbc:mariadb://nemrod.ens2m.fr:3306/tp_jdbc", "etudiant", "YTDTvj9TR3CDYCmP");

        PreparedStatement requete = connexion.prepareStatement(
            "SELECT proprietaire, espece " +
            "FROM pokemons " +
            "ORDER BY proprietaire"
        );

        ResultSet resultat = requete.executeQuery();

        String dernierProprietaire = null;

        while (resultat.next()) {
            String proprietaire = resultat.getString("proprietaire");
            String espece = resultat.getString("espece");

            // Nouveau dresseur → affichage du titre
            if (!proprietaire.equals(dernierProprietaire)) {
                System.out.println("\n" + proprietaire + " :");
                dernierProprietaire = proprietaire;
            }

            // Affichage du Pokémon
            System.out.println("  - " + espece);
        }

            requete.close();
            connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
