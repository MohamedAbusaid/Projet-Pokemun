package Pokemon;

import java.awt.Graphics2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import outils.SingletonJDBC;

public class Jeu {

    private Carte carte;
    private Avatar avatar;           
    private Dresseurs lesAutresJoueurs; 
    private Pokemons lesMonstres;  
    private boolean partieFinie = false; // Pour se souvenir que c'est fini

    public Jeu(String pseudoJoueur) {
        this.carte = new Carte();
        
        // On initialise l'Avatar (le moi)
        this.avatar = new Avatar(carte, pseudoJoueur);
        
        // On initialise les autres joueurs, en précisant qui "je" suis pour qu'il m'ignore
        this.lesAutresJoueurs = new Dresseurs(carte, pseudoJoueur);
        
        // On initialise les PNJ
        this.lesMonstres = new Pokemons(carte);
    }

    public void miseAJour() {
        this.carte.miseAJour();
        this.lesMonstres.miseAJour();
        this.lesAutresJoueurs.miseAJour();
        this.avatar.miseAJour();
    }

    public void rendu(Graphics2D contexte) {
        // L'ordre définit la superposition
        this.carte.rendu(contexte);         // Fond
        this.lesMonstres.rendu(contexte);   // PNJ
        this.lesAutresJoueurs.rendu(contexte); // Autres joueurs
        this.avatar.rendu(contexte);        // Moi (au premier plan)
    }
    
    public boolean estTermine() {
        // Si on sait déjà que c'est fini, on répond tout de suite "vrai" sans interroger la BDD
        if (this.partieFinie) return true;

        try {
            Connection con = SingletonJDBC.getInstance().getConnection();

            // 1. Vérifier le TEMPS
            PreparedStatement reqTemps = con.prepareStatement("SELECT debut FROM parties ORDER BY id DESC LIMIT 1");
            ResultSet resTemps = reqTemps.executeQuery();
            long debut = 0;
            if (resTemps.next()) {
                debut = resTemps.getTimestamp("debut").getTime();
            }
            reqTemps.close();

            long duree = 60 * 1000; // 1 minute
            if (System.currentTimeMillis() - debut > duree) {
                this.partieFinie = true; // On mémorise que c'est fini
                return true;
            }

            // 2. Vérifier si le Dresseur a GAGNÉ
            PreparedStatement reqVictoire = con.prepareStatement(
                "SELECT COUNT(*) FROM joueurs WHERE role != 'Dresseur' AND statut = 'LIBRE'"
            );
            ResultSet resVictoire = reqVictoire.executeQuery();
            resVictoire.next();
            int survivants = resVictoire.getInt(1);
            reqVictoire.close();

            if (survivants == 0) {
                this.partieFinie = true; // On mémorise que c'est fini
                return true;
            }

        } catch (SQLException e) { e.printStackTrace(); }

        return false;
    }

    public Avatar getAvatar() {
        return avatar;
    }
}