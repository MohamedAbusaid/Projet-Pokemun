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
 *
 * @author guillaume.laurent
 */
public class InitialisationBDD {

    public static void main(String[] args) {

        try {

            Connection connexion =  DriverManager.getConnection("jdbc:mariadb://nemrod.ens2m.fr:3306/tp_jdbc", "etudiant", "YTDTvj9TR3CDYCmP");

            Statement statement = connexion.createStatement() ;
            
//            statement.executeUpdate("DROP TABLE dresseurs");
//            statement.executeUpdate("CREATE TABLE tp_jdbc.dresseurs ( "
//                    + "pseudo VARCHAR(32) NOT NULL, "
//                    + "email VARCHAR(64) NOT NULL, "
//                    + "motDePasse VARCHAR(32) NOT NULL, "
//                    + "latitude DOUBLE NOT NULL, "
//                    + "longitude DOUBLE NOT NULL, "
//                    + "derniereConnexion DATETIME NOT NULL, "
//                    + "PRIMARY KEY (pseudo)) ENGINE = InnoDB");
//           
//            statement.executeUpdate("DROP TABLE pokemons");
//            statement.executeUpdate("CREATE TABLE tp_jdbc.pokemons ( "
//                    + "id INT NOT NULL AUTO_INCREMENT, "
//                    + "espece VARCHAR(32) NOT NULL, "
//                    + "latitude DOUBLE NOT NULL, "
//                    + "longitude DOUBLE NOT NULL, "
//                    + "visible BOOLEAN NOT NULL, "
//                    + "creation DATETIME NOT NULL, "
//                    + "proprietaire VARCHAR(32) NOT NULL, "
//                    + "PRIMARY KEY (id)) ENGINE = InnoDB");
            
            statement.executeUpdate("DELETE FROM dresseurs;");           
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, email, motDePasse, latitude, longitude, derniereConnexion) "
                    + "VALUES ('sacha', 'sacha@ens2m.org', MD5('1234'), 47.250221, 5.995451, NOW())");
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, email, motDePasse, latitude, longitude, derniereConnexion) "
                    + "VALUES ('ondine', 'ondine@ens2m.org', MD5('1234'), 47.250925, 5.992382, NOW())");
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, email, motDePasse, latitude, longitude, derniereConnexion) "
                    + "VALUES ('james', 'james@ens2m.org', MD5('1234'), 47.251617, 5.993995, NOW())");
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, email, motDePasse, latitude, longitude, derniereConnexion) "
                    + "VALUES ('jessie', 'jessie@ens2m.org', MD5('1234'), 47.251848, 5.993777, NOW())");
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, email, motDePasse, latitude, longitude, derniereConnexion) "
                    + "VALUES ('flora', 'flora@ens2m.org', MD5('1234'), 47.251082, 5.993007, NOW())");
            
            statement.executeUpdate("INSERT INTO dresseurs (pseudo, email, motDePasse, latitude, longitude, derniereConnexion) "
                    + "VALUES ('pierre', 'pierre@ens2m.org', MD5('1234'), 47.250221, 5.992052, NOW())");
            
            ResultSet resultat = statement.executeQuery("SELECT * FROM dresseurs;");
            OutilsJDBC.afficherResultSet(resultat);
            
            statement.executeUpdate("DELETE FROM pokemons;");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'pikachu', 47.249983, 5.995654, 1, NOW(), 'sacha')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'miaouss', 47.251631, 5.992150, 1, NOW(), 'jessie')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'evoli', 47.251033, 5.989970, 1, NOW(), 'sauvage')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'salameche', 47.249835, 5.991402, 0, NOW(), 'sauvage')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'carapuce', 47.249598, 5.989169, 1, NOW(), 'sauvage')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'tiplouf', 47.250496, 5.991135, 1, NOW(), 'sauvage')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'carapuce', 47.250972, 5.996097, 0, NOW(), 'sauvage')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'ronflex', 47.250461, 5.993067, 1, NOW(), 'sauvage')");
            
            statement.executeUpdate("INSERT INTO pokemons (id, espece, latitude, longitude, visible, creation, proprietaire) "
                    + "VALUES (0, 'evoli', 47.249431, 5.994519, 0, NOW(), 'sacha')");
            
            resultat = statement.executeQuery("SELECT * FROM pokemons;");           
            OutilsJDBC.afficherResultSet(resultat);
            
            statement.close();
            connexion.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
