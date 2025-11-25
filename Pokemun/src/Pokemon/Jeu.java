package pokemon;

import java.awt.Graphics2D;

/**
 * Classe Maître du Jeu.
 * Elle centralise tous les éléments (Carte, Joueur local, Autres joueurs, Monstres).
 * @author Equipe DuBrazil
 */
public class Jeu {

    private Carte carte;
    private Avatar avatar;           // Le joueur local (celui qui est devant l'écran)
    private Dresseurs lesAutresJoueurs; // Les autres joueurs connectés (récupérés de la BDD)
    private Pokemons lesMonstres;    // Les PNJ / Insectes / Objets (récupérés de la BDD)

    /**
     * Constructeur du Jeu.
     * @param pseudoJoueur Le pseudo du joueur qui vient de se connecter.
     */
    public Jeu(String pseudoJoueur) {
        // 1. On charge la carte (le décor)
        this.carte = new Carte();
        
        // 2. On initialise l'Avatar du joueur local avec son pseudo
        // C'est ici que se fait le lien crucial pour savoir qui on contrôle !
        this.avatar = new Avatar(carte, pseudoJoueur);
        
        // 3. On initialise les gestionnaires pour voir les autres entités
        this.lesAutresJoueurs = new Dresseurs(carte);
        this.lesMonstres = new Pokemons(carte);
    }

    /**
     * Mise à jour de la logique du jeu (Appelé 25 fois/seconde).
     */
    public void miseAJour() {
        // On met à jour la carte (si elle est animée)
        this.carte.miseAJour();
        
        // On met à jour les monstres (IA, déplacement aléatoire défini dans leur classe)
        this.lesMonstres.miseAJour();
        
        // On met à jour la liste des autres joueurs (lecture BDD)
        this.lesAutresJoueurs.miseAJour();
        
        // On met à jour notre personnage (gestion clavier + envoi position BDD + tentative capture)
        this.avatar.miseAJour();
    }

    /**
     * Affichage graphique du jeu.
     * L'ordre d'appel définit les "couches" (ce qui est dessiné en dernier est au-dessus).
     */
    public void rendu(Graphics2D contexte) {
        // 1. Le fond (Carte)
        this.carte.rendu(contexte);
        
        // 2. Les éléments du jeu (Monstres, Objets)
        this.lesMonstres.rendu(contexte);
        
        // 3. Les autres joueurs
        this.lesAutresJoueurs.rendu(contexte);
        
        // 4. Notre personnage (pour qu'il soit toujours visible par-dessus le reste)
        this.avatar.rendu(contexte);
    }
    
    /**
     * Vérifie si la partie est finie.
     * (À connecter plus tard à la table 'parties' de la BDD)
     */
    public boolean estTermine() {
        return false; // Pour l'instant, le jeu tourne à l'infini
    }

    // Accesseur pour permettre à FenetreDeJeu de transmettre les touches clavier
    public Avatar getAvatar() {
        return avatar;
    }
}