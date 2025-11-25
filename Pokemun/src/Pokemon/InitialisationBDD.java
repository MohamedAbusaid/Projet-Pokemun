/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pokemon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import outils.OutilsJDBC;

/**
 * Initialisation de la base de données pour le projet "DuBrazil" (Chasseur vs Insectes).
 * @author Equipe DuBrazil
 */
public class InitialisationBDD {

    public static void main(String[] args) {

        try {
            // --- CORRECTION 1 : L'URL doit pointer directement sur la base, sans les dossiers intermédiaires ---
            Connection connexion = DriverManager.getConnection(
                    "jdbc:mariadb://nemrod.ens2m.fr:3306/2025-2026_s1_vs2_tp1_DuBrazil", 
                    "etudiant", 
                    "YTDTvj9TR3CDYCmP"
            );

            Statement statement = connexion.createStatement();

            // 1. Nettoyage préalable
            System.out.println("Suppression des tables existantes...");
            statement.executeUpdate("DROP TABLE IF EXISTS attaques");
            statement.executeUpdate("DROP TABLE IF EXISTS pokemons");
            statement.executeUpdate("DROP TABLE IF EXISTS dresseurs");
            statement.executeUpdate("DROP TABLE IF EXISTS parties");

            // 2. Création des tables
            
            // Table des Parties
            System.out.println("Création de la table parties...");
            statement.executeUpdate("CREATE TABLE parties ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "etat VARCHAR(20) DEFAULT 'ATTENTE', " 
                    + "debut TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "duree_max INT DEFAULT 300 " 
                    + ") ENGINE = InnoDB");

            // Table des Dresseurs (Joueurs)
            System.out.println("Création de la table dresseurs...");
            statement.executeUpdate("CREATE TABLE dresseurs ("
                    + "pseudo VARCHAR(32) PRIMARY KEY, "
                    + "motDePasse VARCHAR(32) NOT NULL, " 
                    + "role VARCHAR(20) NOT NULL, "       
                    + "latitude DOUBLE NOT NULL, "
                    + "longitude DOUBLE NOT NULL, "
                    + "statut VARCHAR(20) DEFAULT 'LIBRE', " 
                    + "derniereConnexion DATETIME NOT NULL "
                    + ") ENGINE = InnoDB");

            // Table des Pokemons (PNJ / IA / Objets)
            System.out.println("Création de la table pokemons...");
            statement.executeUpdate("CREATE TABLE pokemons ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "espece VARCHAR(32) NOT NULL, "
                    + "latitude DOUBLE NOT NULL, "
                    + "longitude DOUBLE NOT NULL, "
                    + "visible TINYINT(1) DEFAULT 1, "   
                    + "proprietaire VARCHAR(32) NULL, "   
                    + "FOREIGN KEY (proprietaire) REFERENCES dresseurs(pseudo) "
                    + ") ENGINE = InnoDB");

            // Table des Attaques
            System.out.println("Création de la table attaques...");
            statement.executeUpdate("CREATE TABLE attaques ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "attaquant VARCHAR(32) NOT NULL, "
                    + "type VARCHAR(20) NOT NULL, "      
                    + "lat_cible DOUBLE NOT NULL, "
                    + "lon_cible DOUBLE NOT NULL, "
                    + "date_action DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (attaquant) REFERENCES dresseurs(pseudo) "
                    + ") ENGINE = InnoDB");

            // 3. Insertion de données de test
            System.out.println("Insertion des données de test...");

            // Partie
            statement.executeUpdate("INSERT INTO parties (etat) VALUES ('ATTENTE')");

            // Dresseurs
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, motDePasse, role, latitude, longitude, statut, derniereConnexion) "
                    + "VALUES ('Jawad', '1234', 'CHASSEUR', 47.250221, 5.995451, 'LIBRE', NOW())");
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, motDePasse, role, latitude, longitude, statut, derniereConnexion) "
                    + "VALUES ('Moha', '1234', 'INSECTE', 47.251617, 5.993995, 'LIBRE', NOW())");
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, motDePasse, role, latitude, longitude, statut, derniereConnexion) "
                    + "VALUES ('Tanguy', '1234', 'INSECTE', 47.250925, 5.992382, 'LIBRE', NOW())");

            // Pokemons (Décors)
            statement.executeUpdate("INSERT INTO pokemons (espece, latitude, longitude, visible, proprietaire) "
                    + "VALUES ('Insecateur', 47.250983, 5.995654, 1, NULL)");
            statement.executeUpdate("INSERT INTO pokemons (espece, latitude, longitude, visible, proprietaire) "
                    + "VALUES ('Parasect', 47.251500, 5.993000, 1, NULL)");

            System.out.println("Base de données DuBrazil initialisée avec succès !");

            // 5. Affichage de vérification
            
            System.out.println("\n--- DRESSEURS ---");
            ResultSet resultat = statement.executeQuery("SELECT * FROM dresseurs;");
            OutilsJDBC.afficherResultSet(resultat);
            
            System.out.println("\n--- PARTIES ---");
            // --- CORRECTION 2 : Utilisation du nom "parties" (pluriel) comme défini dans le CREATE ---
            resultat = statement.executeQuery("SELECT * FROM parties;");
            OutilsJDBC.afficherResultSet(resultat);
            
            System.out.println("\n--- ATTAQUES ---");
            resultat = statement.executeQuery("SELECT * FROM attaques;");
            OutilsJDBC.afficherResultSet(resultat);
            
            System.out.println("\n--- POKEMONS ---");
            resultat = statement.executeQuery("SELECT * FROM pokemons;");
            OutilsJDBC.afficherResultSet(resultat);
            
            statement.close();
            connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}