package Pokemon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

public class Carte {

    protected BufferedImage decor;
    
    // Configuration de la grille
    private final int LARGEUR_TUILE = 32;
    private final int HAUTEUR_TUILE = 32;
    private final int NB_COLONNES = 20;
    private final int NB_LIGNES = 30;

    // Bornes GPS
    public static double LATITUDE_MAX = 47.251893;
    public static double LATITUDE_MIN = 47.248851;
    public static double LONGITUDE_MAX = 5.996778;
    public static double LONGITUDE_MIN = 5.988060;

    // Liste des IDs qui bloquent le joueur (Murs, Arbres, Eau)
    private Set<Integer> murs = new HashSet<>();
    private Set<Integer> sauts = new HashSet<>();

    // TA MATRICE (Hardcodée)
    // Format : [Ligne (Y)][Colonne (X)]
    private final int[][] matrice = {
        {28, 3, 30, 29, 29, 29, 28, 29, 28, 28, 29, 30, 28, 28, 30, 28, 28, 29, 28, 29},
        {29, 0, 4, 30, 30, 30, 29, 30, 29, 29, 30, 29, 28, 29, 28, 29, 29, 30, 29, 30},
        {30, 28, 0, 0, 0, 1, 30, 28, 30, 30, 28, 30, 29, 30, 29, 30, 30, 28, 30, 28},
        {28, 29, 2, 0, 2, 0, 1, 29, 1, 1, 29, 28, 30, 28, 30, 1, 2, 29, 28, 29},
        {29, 30, 0, 0, 0, 0, 0, 30, 0, 1, 30, 29, 28, 29, 0, 1, 1, 30, 29, 30},
        {30, 1, 0, 28, 0, 0, 0, 2, 0, 0, 0, 30, 29, 30, 0, 0, 0, 0, 30, 28},
        {28, 2, 0, 29, 1, 28, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 2, 0, 28, 29},
        {29, 0, 0, 30, 28, 29, 0, 0, 28, 0, 0, 0, 0, 0, 28, 0, 0, 0, 29, 30},
        {30, 28, 0, 28, 29, 30, 31, 32, 29, 28, 28, 28, 28, 0, 29, 28, 0, 0, 30, 28},
        {28, 29, 0, 29, 30, 1, 0, 0, 30, 29, 29, 29, 29, 0, 30, 29, 31, 32, 28, 29},
        {29, 30, 0, 30, 0, 0, 0, 0, 0, 30, 30, 30, 30, 0, 0, 30, 0, 0, 29, 30},
        {30, 28, 0, 0, 0, 28, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 30, 28},
        {28, 29, 0, 0, 28, 29, 28, 0, 2, 0, 0, 0, 28, 0, 28, 0, 2, 0, 1, 29},
        {29, 30, 31, 32, 29, 30, 29, 0, 0, 0, 0, 1, 29, 28, 29, 28, 0, 0, 1, 30},
        {30, 28, 0, 0, 30, 28, 30, 1, 0, 0, 0, 28, 30, 29, 30, 29, 28, 0, 0, 28},
        {28, 29, 0, 1, 28, 29, 28, 1, 28, 28, 0, 29, 28, 30, 28, 30, 29, 31, 32, 29},
        {29, 30, 0, 28, 29, 30, 29, 28, 29, 29, 0, 30, 29, 28, 29, 28, 30, 0, 1, 30},
        {30, 1, 0, 29, 30, 28, 30, 29, 30, 30, 0, 0, 30, 29, 30, 29, 0, 0, 0, 28},
        {28, 0, 0, 30, 28, 29, 28, 30, 28, 0, 0, 0, 0, 30, 1, 30, 0, 0, 0, 29},
        {29, 0, 2, 1, 29, 30, 29, 28, 29, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0, 30},
        {30, 28, 0, 0, 30, 28, 30, 29, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28},
        {28, 29, 0, 0, 28, 29, 28, 30, 0, 0, 0, 28, 0, 0, 28, 0, 0, 2, 0, 29},
        {29, 30, 0, 0, 29, 30, 29, 0, 0, 0, 28, 29, 28, 0, 29, 0, 0, 0, 0, 30},
        {30, 28, 0, 0, 30, 28, 30, 0, 0, 28, 29, 30, 29, 3, 30, 0, 0, 0, 0, 28},
        {28, 29, 0, 0, 0, 29, 0, 0, 28, 29, 30, 28, 30, 0, 1, 1, 0, 0, 0, 29},
        {29, 30, 0, 2, 0, 30, 0, 0, 29, 30, 28, 29, 0, 0, 0, 0, 0, 0, 1, 30},
        {30, 28, 1, 0, 0, 0, 0, 0, 30, 1, 29, 30, 2, 0, 0, 0, 0, 28, 1, 28},
        {28, 29, 28, 28, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 28, 29, 28, 29},
        {29, 30, 29, 29, 28, 1, 0, 28, 28, 0, 0, 0, 28, 0, 1, 28, 29, 30, 29, 30},
        {30, 28, 30, 30, 29, 28, 28, 29, 29, 28, 28, 28, 29, 28, 28, 29, 30, 28, 30, 28}
    };



    public Carte() {
        try {
            this.decor = ImageIO.read(getClass().getResource("/resources/Foret_de_Rambouillet.png"));
            
            // --- MODIFICATION ICI ---
            // On garde les murs classiques
            murs.add(28); murs.add(29); murs.add(30); 
            
            // On définit les sauts (Ledgers)
            // ATTENTION : J'ai retiré 31 et 32 de la ligne 'murs' au dessus !
            sauts.add(3); 
            sauts.add(31); 
            sauts.add(32);
            // ------------------------
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Retourne l'ID de la tuile à une position GPS donnée.
     * Utile pour savoir si c'est de l'herbe (0,1,2) ou un mur.
     */
    public int getTuileID(double latitude, double longitude) {
        int col = longitudeEnPixel(longitude) / LARGEUR_TUILE;
        int lig = latitudeEnPixel(latitude) / HAUTEUR_TUILE;

        if (col < 0 || col >= NB_COLONNES || lig < 0 || lig >= NB_LIGNES) {
            return -1; // Hors map
        }
        return matrice[lig][col]; // Attention : matrice[Ligne][Colonne]
    }

    /**
     * Vérifie si on peut marcher à cet endroit
     */
    public boolean estTraversable(double latitude, double longitude, int direction) {
        int id = getTuileID(latitude, longitude);
        
        // 1. Si c'est hors map ou un mur classique -> BLOQUÉ
        if (id == -1 || murs.contains(id)) {
            return false;
        }

        // 2. Si c'est un rebord (saut)
        if (sauts.contains(id)) {
            // On ne peut passer que si on va vers le BAS (1)
            if (direction == 1) {
                return true; // Ça passe !
            } else {
                return false; // Si on vient de gauche, droite ou bas -> BLOQUÉ
            }
        }

        // 3. Sinon (herbe, chemin...) -> OK
        return true;
    }

    public int longitudeEnPixel(double longitude) {
        int x = (int) (decor.getWidth() * (longitude - LONGITUDE_MIN) / (LONGITUDE_MAX - LONGITUDE_MIN));
        if (x < 5) {
            x = 5;
        }
        if (x > decor.getWidth() - 5) {
            x = decor.getWidth() - 5;
        }
        return x;
    }

    public int latitudeEnPixel(double latitude) {
        int y = decor.getHeight() - (int) (decor.getHeight() * (latitude - LATITUDE_MIN) / (LATITUDE_MAX - LATITUDE_MIN));
        if (y < 5) {
            y = 5;
        }
        if (y > decor.getHeight() - 5) {
            y = decor.getHeight() - 5;
        }
        return y;
    }
    
    public double pixelEnLongitude(int x) {
        double largeurDecor = this.decor.getWidth();
        double rangeLon = LONGITUDE_MAX - LONGITUDE_MIN;
        // Inverse de longitudeEnPixel
        return LONGITUDE_MIN + ((double) x / largeurDecor) * rangeLon;
    }

    public double pixelEnLatitude(int y) {
        double hauteurDecor = this.decor.getHeight();
        double rangeLat = LATITUDE_MAX - LATITUDE_MIN;
        // Inverse de latitudeEnPixel, en tenant compte de l'inversion de l'axe Y
        double yInverse = hauteurDecor - (double) y;
        return LATITUDE_MIN + (yInverse / hauteurDecor) * rangeLat;
    }

    public void miseAJour() { }

    public void rendu(Graphics2D contexte) {
        if (decor != null) contexte.drawImage(decor, 0, 0, null);
    }
}