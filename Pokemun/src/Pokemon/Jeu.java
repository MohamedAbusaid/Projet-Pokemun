package Pokemon;

import java.awt.Graphics2D;

public class Jeu {

    private Carte carte;
    private Avatar avatar;           
    private Dresseurs lesAutresJoueurs; 
    private Pokemons lesMonstres;    

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
        return false; 
    }

    public Avatar getAvatar() {
        return avatar;
    }
}