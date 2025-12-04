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
    private double maLatitude;
    private double maLongitude;
    private boolean positionInitialisee = false; // Pour ne pas bouger avant d'avoir chargé la BDD

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
        chargerPositionInitiale();

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

    private void chargerPositionInitiale() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?");
            req.setString(1, this.pseudo);
            ResultSet res = req.executeQuery();
            
            if (res.next()) {
                this.maLatitude = res.getDouble("latitude");
                this.maLongitude = res.getDouble("longitude");
                this.positionInitialisee = true; // C'est bon, on peut bouger !
                System.out.println("Position chargée : " + maLatitude + ", " + maLongitude);
            }
            req.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private void sauvegarderPositionBDD() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            // On met à jour la position du joueur
            PreparedStatement req = con.prepareStatement(
                "UPDATE joueurs SET latitude = ?, longitude = ? WHERE pseudo = ?"
            );
            req.setDouble(1, this.maLatitude);
            req.setDouble(2, this.maLongitude);
            req.setString(3, this.pseudo);
            
            req.executeUpdate();
            req.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    public void miseAJour() {
        // 1. Sécurité : On ne bouge pas si on n'a pas encore chargé notre position depuis la BDD
        if (!positionInitialisee) return; 

        // 2. On prépare la prochaine position (Hypothèse)
        double futurLat = maLatitude;
        double futurLon = maLongitude;
        boolean enMouvement = false;

        // 3. On regarde les touches appuyées
        if (toucheHaut) {
            futurLat += VITESSE_LAT;
            direction = 0;
            enMouvement = true;
        } else if (toucheBas) {
            futurLat -= VITESSE_LAT;
            direction = 1;
            enMouvement = true;
        } else if (toucheGauche) {
            futurLon -= VITESSE_LON;
            direction = 2;
            enMouvement = true;
        } else if (toucheDroite) {
            futurLon += VITESSE_LON;
            direction = 3;
            enMouvement = true;
        }

        // 4. Gestion de l'animation (les jambes qui bougent)
        if (enMouvement) {
            if (System.currentTimeMillis() - dernierChangement > 200) {
                etapeAnimation = (etapeAnimation + 1) % 2;
                dernierChangement = System.currentTimeMillis();
            }
        } else {
            etapeAnimation = 0; 
        }

        // =========================================================
        // C'EST ICI QUE TU COLLES TON BLOC DE COLLISION
        // =========================================================
        if (enMouvement) {
            // A. On demande à la carte : "Est-ce que je peux aller là ?"
            if (laCarte.estTraversable(futurLat, futurLon, this.direction)) {
                
                // OUI : On valide le déplacement
                this.maLatitude = futurLat;
                this.maLongitude = futurLon;
                
                // On sauvegarde en BDD (pour que les autres nous voient bouger)
                sauvegarderPositionBDD();

                // B. On vérifie sur QUOI on marche (Interaction)
                int typeSol = laCarte.getTuileID(futurLat, futurLon);
                
                // Exemple : Si ID 1 ou 2 = Hautes herbes
                if (typeSol == 1 || typeSol == 2) {
                     // 1 chance sur 500 à chaque pas (ajuste le 0.002 selon tes goûts)
                     if (Math.random() < 0.002) {
                         System.out.println("!!! Un Pokémon sauvage apparaît !!!");
                         // C'est ici que tu lanceras plus tard : new FenetreCombat();
                     }
                }

            } else {
                // NON : Mur -> On ne change pas maLatitude/maLongitude
                System.out.println("Bloqué par un obstacle (ID: " + laCarte.getTuileID(futurLat, futurLon) + ")");
            }
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