package Pokemon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import outils.OutilsJDBC;

public class AffichageBDD {

    public static void main(String[] args) {
        try {
            Connection connexion = DriverManager.getConnection(
                    "jdbc:mariadb://nemrod.ens2m.fr:3306/2025-2026_s1_vs2_tp1_DuBrazil", 
                    "etudiant", "YTDTvj9TR3CDYCmP"
            );
            Statement statement = connexion.createStatement();
            
            // 4. Affichage de vérification
            System.out.println("\n========================================");
            System.out.println("       CONTENU DE LA BASE DE DONNÉES      ");
            System.out.println("========================================");

            System.out.println("\n--- TABLE : JOUEURS ---");
            ResultSet resJoueurs = statement.executeQuery("SELECT * FROM joueurs");
            OutilsJDBC.afficherResultSet(resJoueurs);
            
            System.out.println("\n--- TABLE : POKEMONS ---");
            ResultSet resPokemons = statement.executeQuery("SELECT * FROM pokemons");
            OutilsJDBC.afficherResultSet(resPokemons);

            System.out.println("\n--- TABLE : PARTIES ---");
            ResultSet resParties = statement.executeQuery("SELECT * FROM parties");
            OutilsJDBC.afficherResultSet(resParties);

            System.out.println("\n--- TABLE : ATTAQUES ---");
            ResultSet resAttaques = statement.executeQuery("SELECT * FROM attaques");
            OutilsJDBC.afficherResultSet(resAttaques);

            statement.close();
            connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}