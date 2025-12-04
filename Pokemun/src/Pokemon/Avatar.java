package pokemon;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;

public class Avatar {

    // Contrôles
    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    
    // Infos joueur
    private String pseudo;
    private String role; 
    protected Carte laCarte;

    // --- ANIMATION ---
    private BufferedImage[][] sprites; // Tableau 2D [colonne][ligne]
    private int direction = 1; // 0=Haut, 1=Bas, 2=Gauche, 3=Droite
    private int etapeAnimation = 0; 
    private long dernierChangement = 0; 
    
    // Vitesses ajustées
    private final double VITESSE_LAT = 0.000015; 
    private final double VITESSE_LON = 0.000060; 

    public Avatar(Carte laCarte, String pseudoJoueur) {
        this.laCarte = laCarte;
        this.pseudo = pseudoJoueur; 
        
        recupererRole(); // Récupère "Dresseur", "Drascore", etc.

        // Chargement de la planche correspondante
        try {
            String nomImage = "";
            if ("Dresseur".equalsIgnoreCase(role)) {
                nomImage = "/resources/Dresseur.png"; 
            } else {
                // Si je suis un Pokémon, je charge l'image de mon espèce
                nomImage = "/resources/" + role + ".png"; 
            }
            
            BufferedImage planche = ImageIO.read(getClass().getResource(nomImage));
            this.sprites = decouperPlanche(planche);
            
        } catch (Exception ex) {
            System.err.println("Erreur chargement Avatar (" + role + ") : " + ex.getMessage());
        }
    }

    private BufferedImage[][] decouperPlanche(BufferedImage planche) {
        BufferedImage[][] tuiles = new BufferedImage[2][4];
        for (int col = 0; col < 2; col++) {
            for (int lig = 0; lig < 4; lig++) {
                tuiles[col][lig] = planche.getSubimage(col * 32, lig * 32, 32, 32);
            }
        }
        return tuiles;
    }

    private void recupererRole() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("SELECT role FROM joueurs WHERE pseudo = ?");
            req.setString(1, this.pseudo);
            ResultSet res = req.executeQuery();
            if (res.next()) {
                this.role = res.getString("role");
                System.out.println("Avatar : " + this.pseudo + " est un " + this.role);
            }
            req.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    public void miseAJour() {
        double deltaLat = 0;
        double deltaLon = 0;
        boolean enMouvement = false;

        if (toucheHaut) {
            deltaLat += VITESSE_LAT;
            direction = 0;
            enMouvement = true;
        } else if (toucheBas) {
            deltaLat -= VITESSE_LAT;
            direction = 1;
            enMouvement = true;
        } else if (toucheGauche) {
            deltaLon -= VITESSE_LON;
            direction = 2;
            enMouvement = true;
        } else if (toucheDroite) {
            deltaLon += VITESSE_LON;
            direction = 3;
            enMouvement = true;
        }

        // Animation
        if (enMouvement) {
            if (System.currentTimeMillis() - dernierChangement > 200) {
                etapeAnimation = (etapeAnimation + 1) % 2;
                dernierChangement = System.currentTimeMillis();
            }
        } else {
            etapeAnimation = 0; 
        }

        // Envoi BDD (Table joueurs)
        if (deltaLat != 0 || deltaLon != 0) {
            try {
                Connection connexion = SingletonJDBC.getInstance().getConnection();
                PreparedStatement req = connexion.prepareStatement(
                        "UPDATE joueurs SET latitude = latitude + ?, longitude = longitude + ?, derniereConnexion = NOW() WHERE pseudo = ?");
                req.setDouble(1, deltaLat);
                req.setDouble(2, deltaLon);
                req.setString(3, this.pseudo);
                req.executeUpdate();
                req.close();
                
                if ("Dresseur".equals(this.role)) tenterCapture(connexion);
                
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
    
    private void tenterCapture(Connection connexion) throws SQLException {
        PreparedStatement reqPos = connexion.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?");
        reqPos.setString(1, this.pseudo);
        ResultSet res = reqPos.executeQuery();
        
        if (res.next()) {
            double maLat = res.getDouble("latitude");
            double maLon = res.getDouble("longitude");
            
            PreparedStatement reqCapture = connexion.prepareStatement(
                "UPDATE joueurs SET statut = 'CAPTURE' " +
                "WHERE role != 'Dresseur' AND statut = 'LIBRE' " +
                "AND ABS(latitude - ?) < 0.0002 AND ABS(longitude - ?) < 0.0002"
            );
            reqCapture.setDouble(1, maLat);
            reqCapture.setDouble(2, maLon);
            int captures = reqCapture.executeUpdate();
            if (captures > 0) System.out.println("Capture !");
            reqCapture.close();
        }
        reqPos.close();
    }

    public void rendu(Graphics2D contexte) {
        try {
            Connection connexion = SingletonJDBC.getInstance().getConnection();
            PreparedStatement requete = connexion.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?");
            requete.setString(1, pseudo);
            ResultSet resultat = requete.executeQuery();
            
            if (resultat.next()) {
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                
                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                
                // --- CORRECTION COULEUR ---
                // On définit la couleur du joueur ICI pour l'utiliser pour le rond ET le texte
                Color couleurJoueur;
                if ("Dresseur".equalsIgnoreCase(this.role)) {
                    couleurJoueur = Color.RED;
                } else {
                    couleurJoueur = Color.YELLOW; // Jaune pour les Pokémon (Insectes)
                }
                // --------------------------

                // Calcul de la bonne image
                BufferedImage imgAffiche = null;
                if (sprites != null) {
                    int col = 0;
                    int lig = 0;
                    switch(direction) {
                        case 0: col = 0; lig = 0 + etapeAnimation; break; 
                        case 1: col = 0; lig = 2 + etapeAnimation; break; 
                        case 2: col = 1; lig = 0 + etapeAnimation; break; 
                        case 3: col = 1; lig = 2 + etapeAnimation; break; 
                    }
                    imgAffiche = sprites[col][lig];
                }

                if (imgAffiche != null) {
                    contexte.drawImage(imgAffiche, x - 16, y - 16, 32, 32, null);
                } else {
                    // Fallback avec la bonne couleur
                    contexte.setColor(couleurJoueur);
                    contexte.fillOval(x - 10, y - 10, 20, 20);
                }
                
                // --- AFFICHAGE PSEUDO CENTRÉ AVEC LA BONNE COULEUR ---
                contexte.setColor(couleurJoueur); // Rouge pour Dresseur, Jaune pour Pokemon
                contexte.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
                
                FontMetrics fm = contexte.getFontMetrics();
                int largeurTexte = fm.stringWidth(pseudo);
                contexte.drawString(pseudo, x - (largeurTexte / 2), y - 20);
                // -----------------------------------------------------
            }
            requete.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    public void setToucheHaut(boolean etat) { this.toucheHaut = etat; }
    public void setToucheBas(boolean etat) { this.toucheBas = etat; }
    public void setToucheGauche(boolean etat) { this.toucheGauche = etat; }
    public void setToucheDroite(boolean etat) { this.toucheDroite = etat; }
    
    public String getRole() { 
        return role;
    }

    /**
     * Lance une μ-ball si le joueur est le Dresseur.
     * Insère la position de départ (dresseur) et la cible (clic) dans la BDD.
     * @param xPixel La coordonnée X du clic.
     * @param yPixel La coordonnée Y du clic.
     */
    public void lancerMuball(int xPixel, int yPixel) {
        if (!"Dresseur".equalsIgnoreCase(this.role)) return; // Seul le dresseur peut attaquer

        try {
            double latCible = this.laCarte.pixelEnLatitude(yPixel);
            double lonCible = this.laCarte.pixelEnLongitude(xPixel);

            Connection connexion = SingletonJDBC.getInstance().getConnection();

            // 1. Récupérer la position de DÉPART (position actuelle du dresseur)
            double latDepart = 0.0, lonDepart = 0.0;
            try (PreparedStatement reqPos = connexion.prepareStatement(
                    "SELECT latitude, longitude FROM joueurs WHERE pseudo = ?")) {
                reqPos.setString(1, pseudo);
                try (ResultSet pos = reqPos.executeQuery()) {
                    if (pos.next()) {
                        latDepart = pos.getDouble("latitude");
                        lonDepart = pos.getDouble("longitude");
                    }
                }
            }

            // 2. Insérer la nouvelle attaque (µ-ball)
            try (PreparedStatement reqInsertAttaque = connexion.prepareStatement(
                "INSERT INTO attaques (attaquant, type, lat_actuelle, lon_actuelle, lat_cible, lon_cible) VALUES (?, 'MUBALL', ?, ?, ?, ?)"
            )) {
                reqInsertAttaque.setString(1, pseudo);
                reqInsertAttaque.setDouble(2, latDepart);   // Position ACTUELLE (départ)
                reqInsertAttaque.setDouble(3, lonDepart);
                reqInsertAttaque.setDouble(4, latCible);    // Position CIBLE (clic)
                reqInsertAttaque.setDouble(5, lonCible);

                reqInsertAttaque.executeUpdate();
                System.out.println(pseudo + " a lancé une μ-ball vers (" + lonCible + ", " + latCible + ")");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}