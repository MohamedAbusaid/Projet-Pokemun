package pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Dresseurs {

    protected Carte laCarte;
    private String pseudoLocal;

    // On stocke les planches découpées pour chaque RÔLE
    // Clé = Rôle (ex: "Dresseur", "Drascore"), Valeur = Tableau 2D
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();

    // Pour mémoriser le mouvement des autres joueurs
    private HashMap<String, EtatJoueur> memoire = new HashMap<>();

    public Dresseurs(Carte laCarte, String pseudoLocal) {
        this.laCarte = laCarte;
        this.pseudoLocal = pseudoLocal;

        // Chargement des planches connues
        chargerPlanche("Dresseur", "/resources/Dresseur.png");
        chargerPlanche("Drascore", "/resources/Drascore.png");
        chargerPlanche("Libegon", "/resources/Libegon.png");
        // Ajoutez ici les autres rôles possibles
    }

    private void chargerPlanche(String role, String chemin) {
        try {
            BufferedImage planche = ImageIO.read(getClass().getResource(chemin));
            BufferedImage[][] tuiles = new BufferedImage[2][4];
            for (int col = 0; col < 2; col++) {
                for (int lig = 0; lig < 4; lig++) {
                    tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
                }
            }
            sprites.put(role, tuiles);
        } catch (Exception e) {
            System.err.println("Erreur image Dresseurs (" + role + ") : " + chemin);
        }
    }

    public void miseAJour() { }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            // On lit la table 'joueurs'
            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude, role, statut FROM joueurs;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                if (pseudo.equals(this.pseudoLocal)) continue; 
                if ("CAPTURE".equals(resultat.getString("statut"))) continue; 

                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String role = resultat.getString("role"); // "Dresseur", "Drascore"...

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // --- CALCUL ANIMATION DISTANTE ---
                EtatJoueur etat = memoire.get(pseudo);
                if (etat == null) {
                    etat = new EtatJoueur(x, y);
                    memoire.put(pseudo, etat);
                }
                
                // Déduction direction
                int direction = etat.direction;
                boolean bouge = false;
                if (x > etat.lastX) { direction = 3; bouge = true; }
                else if (x < etat.lastX) { direction = 2; bouge = true; }
                else if (y > etat.lastY) { direction = 1; bouge = true; }
                else if (y < etat.lastY) { direction = 0; bouge = true; }

                if (bouge) {
                    etat.etapeAnimation = (etat.etapeAnimation + 1) % 2;
                    etat.direction = direction;
                } else {
                    etat.etapeAnimation = 0;
                }
                etat.lastX = x;
                etat.lastY = y;

                // --- AFFICHAGE ---
                BufferedImage[][] planche = sprites.get(role);
                
                if (planche != null) {
                    int col = 0; 
                    int lig = 0;
                    switch(direction) {
                        case 0: col = 0; lig = 0 + etat.etapeAnimation; break;
                        case 1: col = 0; lig = 2 + etat.etapeAnimation; break;
                        case 2: col = 1; lig = 0 + etat.etapeAnimation; break;
                        case 3: col = 1; lig = 2 + etat.etapeAnimation; break;
                    }
                    contexte.drawImage(planche[col][lig], x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback si image pas chargée
                    contexte.setColor(Color.GRAY);
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
                
                contexte.setColor(Color.WHITE);
                contexte.drawString(pseudo, x - 10, y - 20);
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
    
    private class EtatJoueur {
        int lastX, lastY;
        int direction = 1; 
        int etapeAnimation = 0;
        public EtatJoueur(int x, int y) { this.lastX = x; this.lastY = y; }
    }
}