package pokemon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import outils.OutilsJDBC;

public class InitialisationBDD {

    public static void main(String[] args) {
        try {
            // Correction du pilote (laisser cette ligne si l'erreur revient)
            Class.forName("org.mariadb.jdbc.Driver"); 
            
            Connection connexion = DriverManager.getConnection(
                    "jdbc:mariadb://nemrod.ens2m.fr:3306/2025-2026_s1_vs2_tp1_DuBrazil", 
                    "etudiant", "YTDTvj9TR3CDYCmP"
            );
            Statement statement = connexion.createStatement();

            // 1. Nettoyage
            System.out.println("--- Suppression des tables existantes ---");
            // Le DROP TABLE est correct et supprime les anciennes versions
            statement.executeUpdate("DROP TABLE IF EXISTS attaques");
            statement.executeUpdate("DROP TABLE IF EXISTS pokemons");
            statement.executeUpdate("DROP TABLE IF EXISTS joueurs");
            statement.executeUpdate("DROP TABLE IF EXISTS parties");
            
            // 2. Création
            System.out.println("--- Création des tables ---");
            statement.executeUpdate("CREATE TABLE parties (id INT AUTO_INCREMENT PRIMARY KEY, etat VARCHAR(20) DEFAULT 'ATTENTE', debut TIMESTAMP DEFAULT CURRENT_TIMESTAMP, duree_max INT DEFAULT 300) ENGINE = InnoDB");
            statement.executeUpdate("CREATE TABLE joueurs (pseudo VARCHAR(32) PRIMARY KEY, motDePasse VARCHAR(32) NOT NULL, role VARCHAR(20) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, statut VARCHAR(20) DEFAULT 'LIBRE', derniereConnexion DATETIME NOT NULL) ENGINE = InnoDB");
            statement.executeUpdate("CREATE TABLE pokemons (id INT AUTO_INCREMENT PRIMARY KEY, espece VARCHAR(32) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, visible TINYINT(1) DEFAULT 1, proprietaire VARCHAR(32) NULL, FOREIGN KEY (proprietaire) REFERENCES joueurs(pseudo)) ENGINE = InnoDB");
            
            // ATTAQUES : VERSION FINALE AVEC lat_actuelle ET lon_actuelle
            statement.executeUpdate("CREATE TABLE attaques (id INT AUTO_INCREMENT PRIMARY KEY, attaquant VARCHAR(32) NOT NULL, type VARCHAR(20) NOT NULL, lat_actuelle DOUBLE NOT NULL, lon_actuelle DOUBLE NOT NULL, lat_cible DOUBLE NOT NULL, lon_cible DOUBLE NOT NULL, date_action DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (attaquant) REFERENCES joueurs(pseudo)) ENGINE = InnoDB");

            // 3. Insertion des données
            System.out.println("--- Insertion des données ---");
            statement.executeUpdate("INSERT INTO parties (etat) VALUES ('ATTENTE')");
            
            // Joueurs
            statement.executeUpdate("INSERT INTO joueurs VALUES ('Jawad', '1234', 'Dresseur', 47.251855, 5.988695, 'LIBRE', NOW())");
            statement.executeUpdate("INSERT INTO joueurs VALUES ('Tanguy', '1234', 'Drascore', 47.251617, 5.993995, 'LIBRE', NOW())");
            statement.executeUpdate("INSERT INTO joueurs VALUES ('Moha', '1234', 'Libegon', 47.250925, 5.992382, 'LIBRE', NOW())");
            
            // PNJ
            statement.executeUpdate("INSERT INTO pokemons (espece, latitude, longitude, visible, proprietaire) VALUES ('Cizayox', 47.250983, 5.995654, 1, NULL)");
            statement.executeUpdate("INSERT INTO pokemons (espece, latitude, longitude, visible, proprietaire) VALUES ('Scarabrute', 47.251500, 5.993000, 1, NULL)");

            System.out.println("Base initialisée ! Rôles mis à jour : Dresseur, Drascore, Libegon.");
            
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

        } catch (ClassNotFoundException ex) {
            System.err.println("ERREUR GRAVE : Pilote MariaDB non trouvé.");
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}