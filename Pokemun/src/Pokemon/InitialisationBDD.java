package Pokemon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class InitialisationBDD {

    public static void main(String[] args) {
        try {
            // Chargement du driver
            Class.forName("org.mariadb.jdbc.Driver");
            
            Connection connexion = DriverManager.getConnection(
                    "jdbc:mariadb://nemrod.ens2m.fr:3306/2025-2026_s1_vs2_tp1_DuBrazil", 
                    "etudiant", "YTDTvj9TR3CDYCmP"
            );
            Statement statement = connexion.createStatement();

            // 1. Nettoyage
            System.out.println("--- Suppression des tables existantes ---");
            statement.executeUpdate("DROP TABLE IF EXISTS attaques");
            statement.executeUpdate("DROP TABLE IF EXISTS pokemons");
            statement.executeUpdate("DROP TABLE IF EXISTS connexions_en_cours"); // Ajout du dev
            statement.executeUpdate("DROP TABLE IF EXISTS joueurs");
            statement.executeUpdate("DROP TABLE IF EXISTS parties");
            
            // 2. Création
            System.out.println("--- Création des tables ---");
            
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS parties (id INT AUTO_INCREMENT PRIMARY KEY, etat VARCHAR(20) DEFAULT 'ATTENTE', debut TIMESTAMP DEFAULT CURRENT_TIMESTAMP, duree_max INT DEFAULT 300) ENGINE = InnoDB");
            
            // Note: Le dev a ajouté 'role' ici, c'est important
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS joueurs (pseudo VARCHAR(32) PRIMARY KEY, motDePasse VARCHAR(32) NOT NULL, role VARCHAR(20), latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, statut VARCHAR(20) DEFAULT 'LIBRE', derniereConnexion DATETIME NOT NULL) ENGINE = InnoDB");
            
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pokemons (id INT AUTO_INCREMENT PRIMARY KEY, espece VARCHAR(32) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, visible TINYINT(1) DEFAULT 1, proprietaire VARCHAR(32) NULL, FOREIGN KEY (proprietaire) REFERENCES joueurs(pseudo)) ENGINE = InnoDB");
            
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS attaques (id INT AUTO_INCREMENT PRIMARY KEY, attaquant VARCHAR(32) NOT NULL, type VARCHAR(20) NOT NULL, lat_actuelle DOUBLE NOT NULL, lon_actuelle DOUBLE NOT NULL, lat_cible DOUBLE NOT NULL, lon_cible DOUBLE NOT NULL, date_action DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (attaquant) REFERENCES joueurs(pseudo)) ENGINE = InnoDB");
            
            // Table pour le lobby
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS connexions_en_cours (pseudo VARCHAR(32) PRIMARY KEY, date_connexion DATETIME DEFAULT CURRENT_TIMESTAMP, statut VARCHAR(20) DEFAULT 'EN_LIGNE') ENGINE = InnoDB");
            
            // 3. Insertion des données initiales
            System.out.println("--- Insertion des données ---");
            statement.executeUpdate("INSERT INTO parties (etat) VALUES ('ATTENTE')");
            
            // Création des comptes (Role 'none' par défaut, sera défini par le lobby)
            statement.executeUpdate("INSERT INTO joueurs VALUES ('Jawad', '1234', 'none', 0, 0, 'LIBRE', NOW())");
            statement.executeUpdate("INSERT INTO joueurs VALUES ('Tanguy', '1234', 'none', 0, 0, 'LIBRE', NOW())");
            statement.executeUpdate("INSERT INTO joueurs VALUES ('Moha', '1234', 'none', 0, 0, 'LIBRE', NOW())");
            
            // PNJ
            statement.executeUpdate("INSERT INTO pokemons (espece, latitude, longitude, visible, proprietaire) VALUES ('Cizayox', 47.250983, 5.995654, 1, NULL)");
            statement.executeUpdate("INSERT INTO pokemons (espece, latitude, longitude, visible, proprietaire) VALUES ('Scarabrute', 47.251500, 5.993000, 1, NULL)");

            System.out.println("Base initialisée !");
            
            statement.close();
            connexion.close();

        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
        }
    }
}