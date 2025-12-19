package Pokemon;

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

public class Pokemons {

    protected Carte laCarte;
    private HashMap<String, BufferedImage[][]> sprites = new HashMap<>();
    
    // CORRECTION 1: Déclaration de la HashMap pour les sprites de capture
    private HashMap<String, BufferedImage[][]> spritesCapture = new HashMap<>();
    
    private int animationFrame = 0;
    private long lastTime = 0;
    private int directionAleatoire = 1;

    // Constantes de mouvement
    public static final double VITESSE_MUBALL = 0.00008; 
    public static final double RAYON_CAPTURE = 0.0002; 

    public Pokemons(Carte carte) {
        this.laCarte = carte;
        
        // CORRECTION 2: Chargement des sprites normaux
        chargerPlanche("Cizayox", "/resources/Cizayox.png", sprites);
        chargerPlanche("Scarabrute", "/resources/Scarabrute.png", sprites);
        
        // CORRECTION 3: Chargement des sprites de capture
        // Ces fichiers .png doivent exister dans le dossier /resources
        chargerPlanche("Cizayox", "/resources/Cizayox_Capture.png", spritesCapture); 
        chargerPlanche("Scarabrute", "/resources/Scarabrute_Capture.png", spritesCapture); 
    }
    
    // CORRECTION 4: Mise à jour de chargerPlanche pour accepter la Map de destination
    private void chargerPlanche(String espece, String chemin, HashMap<String, BufferedImage[][]> destinationMap) {
        try {
            BufferedImage planche = ImageIO.read(getClass().getResource(chemin));
            BufferedImage[][] tuiles = new BufferedImage[2][4];
            for (int col = 0; col < 2; col++) {
                for (int lig = 0; lig < 4; lig++) {
                    tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
                }
            }
            destinationMap.put(espece, tuiles); // Utilise la Map passée en argument
        } catch (Exception e) {
            System.err.println("Erreur Sprite PNJ ("+espece+") : " + chemin + ". Le PNJ sera invisible.");
        }
    }


    public void miseAJour() {
        // --- 1. Mouvement aléatoire des PNJ (MOUVEMENT CONTINU) ---
        if (System.currentTimeMillis() - lastTime > 1000) {
            animationFrame = (animationFrame + 1) % 2;
            directionAleatoire = (int)(Math.random() * 4);
            lastTime = System.currentTimeMillis();
        }

        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();

            // Mise à jour des PNJ : RETIRER LA CLAUSE 'WHERE proprietaire IS NULL'
            // Tous les PNJ (sauvages et capturés) reçoivent un déplacement aléatoire.
            PreparedStatement requete = connexion.prepareStatement(
                 "UPDATE pokemons SET longitude = longitude + 0.00001 * (FLOOR(RAND()*3)-1), latitude = latitude + 0.00001 * (FLOOR(RAND()*3)-1)");

            requete.executeUpdate();
            requete.close();
        
            // --- 2. Mouvement et Collision des μ-balls ---

            // La requête SELECT doit inclure attaquant pour la capture
            PreparedStatement reqSelectAtt = connexion.prepareStatement(
                "SELECT id, attaquant, lat_actuelle, lon_actuelle, lat_cible, lon_cible FROM attaques WHERE type = 'MUBALL'"
            );
            ResultSet attaques = reqSelectAtt.executeQuery();

            while (attaques.next()) {
                int idAttaque = attaques.getInt("id");
                String attaquant = attaques.getString("attaquant"); 
                double latA = attaques.getDouble("lat_actuelle"); // Position ACTUELLE de la balle
                double lonA = attaques.getDouble("lon_actuelle");
                double latC = attaques.getDouble("lat_cible");
                double lonC = attaques.getDouble("lon_cible");

                // --- 1. GESTION DES CAPTURES (En vol) ---
                // On vérifie si la balle touche quelqu'un ICI et MAINTENANT
                boolean impact = false;

                // A. Capture JOUEUR
                PreparedStatement reqCaptJoueur = connexion.prepareStatement(
                    "UPDATE joueurs SET statut = 'CAPTURE' " +
                    "WHERE role != 'Dresseur' AND statut = 'LIBRE' " +
                    "AND ABS(latitude - ?) < ? AND ABS(longitude - ?) < ?"
                );
                reqCaptJoueur.setDouble(1, latA); // Check autour de la balle
                reqCaptJoueur.setDouble(2, RAYON_CAPTURE);
                reqCaptJoueur.setDouble(3, lonA);
                reqCaptJoueur.setDouble(4, RAYON_CAPTURE);
                if (reqCaptJoueur.executeUpdate() > 0) {
                    System.out.println("Joueur touche en vol !");
                    impact = true;
                }
                reqCaptJoueur.close();

                // B. Capture PNJ
                if (!impact) { // Si on n'a pas déjà touché un joueur
                    PreparedStatement reqCaptPNJ = connexion.prepareStatement(
                        "UPDATE pokemons SET proprietaire = ? " +
                        "WHERE proprietaire IS NULL " + 
                        "AND ABS(latitude - ?) < ? AND ABS(longitude - ?) < ?"
                    );
                    reqCaptPNJ.setString(1, attaquant);
                    reqCaptPNJ.setDouble(2, latA);
                    reqCaptPNJ.setDouble(3, RAYON_CAPTURE);
                    reqCaptPNJ.setDouble(4, lonA);
                    reqCaptPNJ.setDouble(5, RAYON_CAPTURE);
                    if (reqCaptPNJ.executeUpdate() > 0) {
                        System.out.println("PNJ capture en vol !");
                        impact = true;
                    }
                    reqCaptPNJ.close();
                }

                // SI IMPACT : On supprime la balle et on passe à la suivante
                if (impact) {
                    PreparedStatement del = connexion.prepareStatement("DELETE FROM attaques WHERE id = ?");
                    del.setInt(1, idAttaque);
                    del.executeUpdate();
                    del.close();
                    continue;
                }

                // --- 2. CALCUL DU DÉPLACEMENT ---
                double distLat = latC - latA;
                double distLon = lonC - lonA;
                double distanceTotale = Math.sqrt(distLat * distLat + distLon * distLon);

                // Sécurité : si la balle sort de la map (très loin), on la supprime
                if (distanceTotale < VITESSE_MUBALL) {
                    PreparedStatement del = connexion.prepareStatement("DELETE FROM attaques WHERE id = ?");
                    del.setInt(1, idAttaque);
                    del.executeUpdate();
                    del.close();
                    continue;
                }

                double pasLat = (distLat / distanceTotale) * VITESSE_MUBALL;
                double pasLon = (distLon / distanceTotale) * VITESSE_MUBALL;
                double nouvelleLat = latA + pasLat;
                double nouvelleLon = lonA + pasLon;

                // --- 3. COLLISION DÉCOR (Arbres) ---
                int idTuile = laCarte.getTuileID(nouvelleLat, nouvelleLon);

                // IDs murs: 28, 29, 30. Hors map: -1
                if (idTuile == -1 || idTuile == 28 || idTuile == 29 || idTuile == 30) {
                    // Boum dans un arbre
                    PreparedStatement del = connexion.prepareStatement("DELETE FROM attaques WHERE id = ?");
                    del.setInt(1, idAttaque);
                    del.executeUpdate();
                    del.close();
                    continue;
                }

                // --- 4. MISE À JOUR POSITION ---
                PreparedStatement up = connexion.prepareStatement(
                    "UPDATE attaques SET lat_actuelle = ?, lon_actuelle = ? WHERE id = ?"
                );
                up.setDouble(1, nouvelleLat);
                up.setDouble(2, nouvelleLon);
                up.setInt(3, idAttaque);
                up.executeUpdate();
                up.close();
            }
            reqSelectAtt.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
    
    
    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            // On sélectionne la colonne 'proprietaire' pour vérifier l'état de capture
            PreparedStatement requete = connexion.prepareStatement("SELECT espece, latitude, longitude, visible, proprietaire FROM pokemons;");
            ResultSet resultat = requete.executeQuery();

            while (resultat.next()) {
                String espece = resultat.getString("espece");
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String proprietaire = resultat.getString("proprietaire");
                
                boolean estCapture = (proprietaire != null);

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);

                // --- CHOIX DE LA PLANCHE À DESSINER ---
                BufferedImage[][] plancheActive = null;

                if (estCapture) {
                    // Tente de récupérer le sprite de capture
                    plancheActive = spritesCapture.get(espece); 
                    // Si l'image de capture n'existe pas, utilise le sprite normal
                    if (plancheActive == null) plancheActive = sprites.get(espece); 
                } else {
                    plancheActive = sprites.get(espece);
                }
                // --- FIN CHOIX DE LA PLANCHE ---


                if (plancheActive != null) {
                    int col = 0; 
                    int lig = 0;
                    // Animation aléatoire pour les PNJ (réutilise animationFrame et directionAleatoire)
                    switch(directionAleatoire) { 
                        case 0: col = 0; lig = 0 + animationFrame; break; 
                        case 1: col = 0; lig = 2 + animationFrame; break; 
                        case 2: col = 1; lig = 0 + animationFrame; break; 
                        case 3: col = 1; lig = 2 + animationFrame; break; 
                    }
                    contexte.drawImage(plancheActive[col][lig], x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback
                    contexte.fillOval(x - 5, y - 5, 10, 10);
                }
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}