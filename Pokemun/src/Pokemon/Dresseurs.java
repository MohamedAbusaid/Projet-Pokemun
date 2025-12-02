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
    
    // Cache pour ne pas recharger l'image du disque 25 fois par seconde
    // Clé = Pseudo (ex: "Drascore"), Valeur = Image
    private HashMap<String, BufferedImage> cacheImages = new HashMap<>();

    public Dresseurs(Carte laCarte, String pseudoLocal) {
        this.laCarte = laCarte;
        this.pseudoLocal = pseudoLocal;
        // On ne pré-charge rien ici, on chargera à la volée dans le rendu
    }

    public void miseAJour() { 
        // Géré par la BDD
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            // On récupère tous les joueurs
            PreparedStatement requete = connexion.prepareStatement("SELECT pseudo, latitude, longitude, role, statut FROM joueurs;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String pseudo = resultat.getString("pseudo");
                
                // 1. On s'ignore soi-même (déjà dessiné par Avatar)
                if (pseudo.equalsIgnoreCase(this.pseudoLocal)) continue; 

                // 2. On ignore les capturés
                if ("CAPTURE".equals(resultat.getString("statut"))) continue; 

                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String role = resultat.getString("role"); 

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // --- GESTION DES IMAGES ---
                
                // Est-ce qu'on a déjà chargé l'image pour ce pseudo ?
                BufferedImage img = cacheImages.get(pseudo);
                
                if (img == null) {
                    // Non, alors on essaie de la charger
                    try {
                        String nomFichier = "";
                        if ("DRESSEUR".equals(role)) {
                            // Pour les chasseurs (Sacha, etc), on prend l'image générique ou spécifique
                            // Ici on tente le nom du pseudo, sinon Dresseur.png
                            try {
                                nomFichier = "/resources/" + pseudo + ".png";
                                img = ImageIO.read(getClass().getResource(nomFichier));
                            } catch (Exception e) {
                                nomFichier = "/resources/Chasseur.png"; // Fallback
                                img = ImageIO.read(getClass().getResource(nomFichier));
                            }
                        } else {
                            // Pour les POKEMON (Libegon, Drascore...), on cherche l'image de leur nom
                            // Attention à la casse ! (Drascore.png)
                            nomFichier = "/resources/" + pseudo + ".png";
                            img = ImageIO.read(getClass().getResource(nomFichier));
                        }
                        
                        // On stocke dans le cache pour la prochaine fois
                        if (img != null) {
                            cacheImages.put(pseudo, img);
                            System.out.println("Image chargée pour : " + pseudo);
                        }
                        
                    } catch (Exception ex) {
                        // Si vraiment on trouve pas, on met une "image vide" dans le cache pour arrêter de chercher
                        // et on affichera un point rouge
                        System.err.println("Impossible de charger l'image pour : " + pseudo);
                        cacheImages.put(pseudo, null); 
                    }
                }

                // --- DESSIN ---
                
                if (img != null) {
                    contexte.drawImage(img, x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback (Point de couleur) si pas d'image
                    if ("DRESSEUR".equals(role)) contexte.setColor(Color.RED);
                    else contexte.setColor(Color.YELLOW); // Jaune pour les insectes sans image
                    
                    contexte.fillOval(x - 10, y - 10, 20, 20);
                }
                
                contexte.setColor(Color.WHITE);
                contexte.drawString(pseudo, x - 10, y - 20);
            }
            requete.close();

        } catch (SQLException ex) { 
            ex.printStackTrace(); 
        }
    }
}