/*
 * Classe de test rapide pour vérifier que le SingletonJDBC fonctionne.
 * À lancer une fois pour valider la connexion avant de partager le code.
 */
package pokemon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import outils.SingletonJDBC;

public class TestConnexion {

    public static void main(String[] args) {
        System.out.println("--- TEST DE CONNEXION AU SERVEUR NEMROD ---");

        try {
            // 1. Demande de connexion au Singleton
            // Si ça plante ici, c'est que l'URL ou le driver dans SingletonJDBC est mauvais
            Connection connexion = SingletonJDBC.getInstance().getConnection();

            if (connexion != null && !connexion.isClosed()) {
                System.out.println("Connexion etablie avec succes !");
                
                // Affichage du nom de la base pour être sûr qu'on est au bon endroit
                System.out.println("Base de donnees actuelle : " + connexion.getCatalog());

                // 2. Test fonctionnel : Lecture de la table DRESSEURS
                System.out.println("\nTentative de lecture des joueurs...");
                
                String sql = "SELECT pseudo, role, statut FROM dresseurs";
                PreparedStatement requete = connexion.prepareStatement(sql);
                ResultSet resultat = requete.executeQuery();

                boolean auMoinsUnJoueur = false;
                
                System.out.println("----------------------------------------------");
                while (resultat.next()) {
                    auMoinsUnJoueur = true;
                    String pseudo = resultat.getString("pseudo");
                    String role = resultat.getString("role");
                    String statut = resultat.getString("statut");
                    
                    System.out.format("| %-15s | %-10s | %-10s |\n", pseudo, role, statut);
                }
                System.out.println("----------------------------------------------");

                if (auMoinsUnJoueur) {
                    System.out.println(" SUCCES TOTAL : La base repond et contient des donnees.");
                } else {
                    System.out.println("️ ATTENTION : La connexion marche, mais la table est vide.");
                    System.out.println("   -> As-tu bien lancé InitialisationBDD.java avant ?");
                }

                requete.close();
                
            } else {
                System.err.println("❌ ÉCHEC : La connexion renvoyée est null.");
            }

        } catch (SQLException ex) {
            System.err.println("❌ CRASH : Une erreur SQL est survenue.");
            System.err.println("Message : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}