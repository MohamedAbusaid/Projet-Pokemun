package Pokemon;

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

    // État du saut
    // Variables pour gérer le saut (Ledge)
    private boolean enSaut = false;
    private long debutSaut = 0;
    private double latDepartSaut = 0;
    // Durée du saut en millisecondes (ajustez si trop lent/rapide)
    private final int DUREE_SAUT = 500; 
    // Distance à parcourir (environ 1.5 à 2 tuiles pour être sûr de passer l'obstacle)
    private final double DISTANCE_SAUT = 0.00015;

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
        // 1. Sécurité
        if (!positionInitialisee) return;

        // --- GESTION DU SAUT EN COURS (ANIMATION) ---
        if (enSaut) {
            long tempsActuel = System.currentTimeMillis();
            long tempsEcoule = tempsActuel - debutSaut;

            if (tempsEcoule >= DUREE_SAUT) {
                // FIN DU SAUT
                enSaut = false;
                // CORRECTION DIRECTION : On utilise MOINS (-) pour descendre (Avant)
                this.maLatitude = latDepartSaut - DISTANCE_SAUT; 
            } else {
                // PENDANT LE SAUT
                double progression = (double) tempsEcoule / DUREE_SAUT;
                // CORRECTION DIRECTION : On interpolle vers le bas (-)
                this.maLatitude = latDepartSaut - (DISTANCE_SAUT * progression);
            }
            return; // Bloque les autres mouvements
        }

        // 2. Préparation variables
        double futurLat = maLatitude;
        double futurLon = maLongitude;
        boolean enMouvement = false;

        // --- DÉTECTION INTELLIGENTE DU SAUT ---
        // On vérifie si on est sur (ou si on arrive sur) un rebord
        // CORRECTION "À CHEVAL" : On regarde légèrement plus bas (sous les pieds)
        // pour anticiper la case rebord même si on est techniquement encore un peu sur la case d'avant.
        double offsetPieds = 0.00005; 
        int tuileSousPieds = laCarte.getTuileID(this.maLatitude - offsetPieds, this.maLongitude);
        
        // CORRECTION ID : Ajout de l'ID 3
        boolean surRebord = (tuileSousPieds == 31 || tuileSousPieds == 32 || tuileSousPieds == 3);

        // Cas A : Déblocage automatique (si on charge la partie sur un rebord)
        // OU Cas B : Déclenchement volontaire (si on appuie sur BAS)
        if (surRebord && (toucheBas || laCarte.getTuileID(this.maLatitude, this.maLongitude) == 31)) {
             enSaut = true;
             debutSaut = System.currentTimeMillis();
             latDepartSaut = this.maLatitude;
             return; // Le saut commence immédiatement
        }

        // 3. Contrôles Classiques
        if (toucheHaut) {
            futurLat += VITESSE_LAT; // + pour monter
            direction = 0;
            enMouvement = true;
        } else if (toucheBas) {
            futurLat -= VITESSE_LAT; // - pour descendre
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

        // 4. Validation Mouvement & Animation
        if (enMouvement) {
            // Animation des jambes
            if (System.currentTimeMillis() - dernierChangement > 200) {
                etapeAnimation = (etapeAnimation + 1) % 2;
                dernierChangement = System.currentTimeMillis();
            }

            // Vérification Collision
            if (laCarte.estTraversable(futurLat, futurLon, this.direction)) {
                this.maLatitude = futurLat;
                this.maLongitude = futurLon;
                sauvegarderPositionBDD();

                // Hautes herbes (Pokemon sauvage)
                int typeSol = laCarte.getTuileID(futurLat, futurLon);
                if (typeSol == 1 || typeSol == 2) {
                    if (Math.random() < 0.002) {
                        System.out.println("!!! Un Pokémon sauvage apparaît !!!");
                    }
                }
            } 
        } else {
            etapeAnimation = 0; 
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
        // --- 1. Utilisation des variables LOCALES (Plus de requête SQL ici !) ---
        // On utilise directement maLatitude / maLongitude qui sont à jour.
        
        int x = laCarte.longitudeEnPixel(this.maLongitude);
        int y = laCarte.latitudeEnPixel(this.maLatitude);
        
        // --- CORRECTION COULEUR ---
        Color couleurJoueur;
        if ("Dresseur".equalsIgnoreCase(this.role)) {
            couleurJoueur = Color.RED;
        } else {
            couleurJoueur = Color.YELLOW; 
        }
        
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
            // Sécurité pour ne pas planter si l'image est mal découpée
            if (col < sprites.length && lig < sprites[0].length) {
                imgAffiche = sprites[col][lig];
            }
        }

        if (imgAffiche != null) {
            contexte.drawImage(imgAffiche, x - 16, y - 16, 32, 32, null);
        } else {
            contexte.setColor(couleurJoueur);
            contexte.fillOval(x - 10, y - 10, 20, 20);
        }
        
        // --- Ombre saut ---
        int hauteurSaut = 0;
        if (enSaut) {
            long tempsEcoule = System.currentTimeMillis() - debutSaut;
            double progression = (double) tempsEcoule / DUREE_SAUT;
            hauteurSaut = (int) (20 * Math.sin(progression * Math.PI));

            contexte.setColor(new Color(0, 0, 0, 80)); 
            contexte.fillOval(x + 5, y + 28, 22, 10); 
        }
        
        // Dessin du personnage avec le décalage Y (Saut)
        // On redessine par dessus l'ombre
        if (imgAffiche != null) {
            contexte.drawImage(imgAffiche, x - 16, (y - 16) - hauteurSaut, 32, 32, null);
        }

        // --- AFFICHAGE PSEUDO ---
        contexte.setColor(couleurJoueur);
        contexte.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
        
        FontMetrics fm = contexte.getFontMetrics();
        int largeurTexte = fm.stringWidth(pseudo);
        contexte.drawString(pseudo, x - (largeurTexte / 2), (y - 20) - hauteurSaut);
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
    
    public double getLatitude() {
        return this.maLatitude;
    }

    public double getLongitude() {
        return this.maLongitude;
    }
}