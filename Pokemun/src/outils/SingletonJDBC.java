package outils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public final class SingletonJDBC {
    private static volatile SingletonJDBC instance = null;
    private Connection connexion;

    private SingletonJDBC() {
        try {
            // L'URL validée qui pointe vers TA base
            this.connexion = DriverManager.getConnection(
                    "jdbc:mariadb://nemrod.ens2m.fr:3306/2025-2026_s1_vs2_tp1_DuBrazil", 
                    "etudiant", "YTDTvj9TR3CDYCmP");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Erreur connexion BDD: " + ex.getMessage());
            System.exit(-1);
        }
    }

    public final static SingletonJDBC getInstance() {
        if (SingletonJDBC.instance == null) {
            synchronized (SingletonJDBC.class) {
                if (SingletonJDBC.instance == null) {
                    SingletonJDBC.instance = new SingletonJDBC();
                }
            }
        }
        return SingletonJDBC.instance;
    }

    public Connection getConnection() {
        return connexion;
    }
}