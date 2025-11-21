package pokemon;

import java.awt.Graphics2D;

/**
 * Exemple de classe jeu
 *
 * @author guillaume.laurent
 */
public class Jeu {

    private Carte carte;
    private Pokemons pokemon;
    private Dresseurs dresseur;
    private Avatar avatar;

    public Jeu() {
        this.carte = new Carte();
        this.pokemon = new Pokemons(carte);
        this.dresseur = new Dresseurs(carte);
        this.avatar = new Avatar(carte);
    }

    public void miseAJour() {
        this.carte.miseAJour();
        this.pokemon.miseAJour();
        this.dresseur.miseAJour();
        this.avatar.miseAJour();
    }

    public void rendu(Graphics2D contexte) {
        this.carte.rendu(contexte);
        this.pokemon.rendu(contexte);
        this.dresseur.rendu(contexte);
        this.avatar.rendu(contexte);
    }
    
    public boolean estTermine() {
        return false;
    }

    public Avatar getAvatar() {
        return avatar;
    }
    
    

}
