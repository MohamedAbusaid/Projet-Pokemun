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
    
    private boolean partieFinie = false; 
    public static String gagnant = ""; 

    // Constructeur adapté pour l'Accueil
    public Jeu(String pseudoJoueur, String roleJoueur) {
        this.carte = new Carte();
        
        // On initialise l'Avatar
        this.avatar = new Avatar(carte, pseudoJoueur);
        
        // On initialise les autres joueurs
        this.lesAutresJoueurs = new Dresseurs(carte, pseudoJoueur);
        this.lesAutresJoueurs.setAvatar(this.avatar); // Important pour les buissons
        
        // On initialise les PNJ
        this.lesMonstres = new Pokemons(carte);
        
        System.out.println("Jeu démarré : " + pseudoJoueur + " (" + roleJoueur + ")");
    }

    public void miseAJour() {
        if (!partieFinie) {
            this.carte.miseAJour();
            this.lesMonstres.miseAJour();
            this.lesAutresJoueurs.miseAJour();
            this.avatar.miseAJour();
        }
    }

    public void rendu(Graphics2D contexte) {
        this.carte.rendu(contexte);         
        this.lesMonstres.rendu(contexte);   
        this.lesAutresJoueurs.rendu(contexte); 
        this.avatar.rendu(contexte);        
    }
    
    // Logique de victoire
    public boolean estTermine() {
        if (this.partieFinie) return true;

        try {
            Connection con = SingletonJDBC.getInstance().getConnection();

            // 1. TEMPS
            PreparedStatement reqTemps = con.prepareStatement("SELECT debut FROM parties ORDER BY id DESC LIMIT 1");
            ResultSet resTemps = reqTemps.executeQuery();
            long debut = 0;
            if (resTemps.next()) {
                debut = resTemps.getTimestamp("debut").getTime();
            }
            reqTemps.close();

            if (System.currentTimeMillis() - debut > 60 * 1000) {
                this.partieFinie = true;
                Jeu.gagnant = "POKEMONS"; 
                return true;
            }

            // 2. SURVIVANTS
            PreparedStatement reqVictoire = con.prepareStatement("SELECT COUNT(*) FROM joueurs WHERE role != 'Dresseur' AND statut = 'LIBRE'");
            ResultSet resVictoire = reqVictoire.executeQuery();
            resVictoire.next();
            int survivants = resVictoire.getInt(1);
            reqVictoire.close();

            if (survivants == 0) {
                this.partieFinie = true;
                Jeu.gagnant = "DRESSEUR"; 
                return true; 
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public Avatar getAvatar() {
        return avatar;
    }
}