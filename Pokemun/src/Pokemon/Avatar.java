package Pokemon;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import outils.SingletonJDBC;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

public class Avatar {

    // Contrôles
    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    
    // Infos joueur
    private String pseudo;
    private String role; 
    protected Carte laCarte;
    private double maLatitude;
    private double maLongitude;
    private boolean positionInitialisee = false;

    // --- ANIMATION ---
    private BufferedImage[][] sprites; 
    private BufferedImage[][] spritesCapture; 
    private int direction = 1; 
    private int etapeAnimation = 0; 
    private long dernierChangement = 0; 

    // --- VARIABLES FUSIONNÉES ---
    
    // 1. Physique (Saut) - Vient de la branche Affichage
    private boolean enSaut = false;
    private long debutSaut = 0;
    private double latDepartSaut = 0;
    private final int DUREE_SAUT = 500; 
    private final double DISTANCE_SAUT = 0.00015;

    // 2. Gameplay (Capture & Chrono) - Vient de la branche Main
    private long debutMessageCapture = 0; 
    private boolean etaitCapture = false; 
    private long debutPartie = 0;
    private final long DUREE_PARTIE = 60 * 1000; // 1 minute

    // Vitesses
    private final double VITESSE_LAT = 0.000015; 
    private final double VITESSE_LON = 0.000060; 

    public Avatar(Carte laCarte, String pseudoJoueur) {
        this.laCarte = laCarte;
        this.pseudo = pseudoJoueur; 

        recupererRole(); 
        chargerPositionInitiale();
        chargerDebutPartie(); // Important : Vient du Main

        try {
            // Chargement Image Normale
            String nomImage = "Dresseur".equalsIgnoreCase(role) ? "/resources/Dresseur.png" : "/resources/" + role + ".png"; 
            BufferedImage planche = ImageIO.read(getClass().getResource(nomImage));
            this.sprites = decouperPlanche(planche);

            // Chargement Image Capture (si pas dresseur) - Vient du Main
            if (!"Dresseur".equalsIgnoreCase(role)) {
                try {
                    String nomImageCapture = "/resources/" + role + "_Capture.png";
                    BufferedImage plancheCapture = ImageIO.read(getClass().getResource(nomImageCapture));
                    this.spritesCapture = decouperPlanche(plancheCapture);
                } catch (Exception e) {
                    System.err.println("Pas d'image capture pour " + role);
                }
            }
        } catch (Exception ex) {
            System.err.println("Erreur chargement Avatar : " + ex.getMessage());
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

    // --- LOGIQUE METIER (Mise à jour) ---
    // Ici, on utilise la logique complexe de la branche Affichage (Saut + Collision)
    public void miseAJour() {
        if (!positionInitialisee) return;

        // A. GESTION DU SAUT (Prioritaire)
        if (enSaut) {
            long tempsActuel = System.currentTimeMillis();
            long tempsEcoule = tempsActuel - debutSaut;
            if (tempsEcoule >= DUREE_SAUT) {
                enSaut = false;
                this.maLatitude = latDepartSaut - DISTANCE_SAUT; 
            } else {
                double progression = (double) tempsEcoule / DUREE_SAUT;
                this.maLatitude = latDepartSaut - (DISTANCE_SAUT * progression);
            }
            return; // Bloque les autres mouvements pendant le saut
        }

        double futurLat = maLatitude;
        double futurLon = maLongitude;
        boolean enMouvement = false;

        // B. DÉTECTION REBORD (Déclencheur Saut)
        double offsetPieds = 0.00005; 
        int tuileSousPieds = laCarte.getTuileID(this.maLatitude - offsetPieds, this.maLongitude);
        boolean surRebord = (tuileSousPieds == 31 || tuileSousPieds == 32 || tuileSousPieds == 3);

        if (surRebord && (toucheBas || laCarte.getTuileID(this.maLatitude, this.maLongitude) == 31)) {
             enSaut = true;
             debutSaut = System.currentTimeMillis();
             latDepartSaut = this.maLatitude;
             return; 
        }

        // C. MOUVEMENTS CLASSIQUES
        if (toucheHaut) {
            // Interaction Panneau (Branche Affichage)
            double testLat = maLatitude + VITESSE_LAT;
            int idDevant = laCarte.getTuileID(testLat, maLongitude);
            if (idDevant == 4) {
                afficherPanneauInfo();
                toucheHaut = false;
                return;
            }
            
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

        // D. VALIDATION & COLLISION
        if (enMouvement) {
            // Animation
            if (System.currentTimeMillis() - dernierChangement > 200) {
                etapeAnimation = (etapeAnimation + 1) % 2;
                dernierChangement = System.currentTimeMillis();
            }

            // Collision (La méthode estTraversable vient de la branche Affichage)
            if (laCarte.estTraversable(futurLat, futurLon, this.direction)) {
                this.maLatitude = futurLat;
                this.maLongitude = futurLon;
                sauvegarderPositionBDD();

                // Gestion Buissons (Pokémon sauvage)
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

    // --- LOGIQUE VISUELLE (Rendu) ---
    // Ici, on fusionne le dessin du sprite (Affichage) avec le HUD (Main)
    public void rendu(Graphics2D contexte) {
        try {
            java.awt.Font fontStandard = new java.awt.Font("Dialog", java.awt.Font.BOLD, 12);
            Connection connexion = SingletonJDBC.getInstance().getConnection();

            PreparedStatement requete = connexion.prepareStatement("SELECT latitude, longitude, statut FROM joueurs WHERE pseudo = ?");
            requete.setString(1, pseudo);
            ResultSet resultat = requete.executeQuery();

            if (resultat.next()) {
                double latitude = resultat.getDouble("latitude");
                double longitude = resultat.getDouble("longitude");
                String statut = resultat.getString("statut");
                boolean estCapture = "CAPTURE".equals(statut);

                // Gestion Chrono Capture (Main)
                if (estCapture && !etaitCapture) {
                    debutMessageCapture = System.currentTimeMillis();
                    etaitCapture = true;
                } else if (!estCapture) {
                    etaitCapture = false;
                }

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                Color couleurJoueur = "Dresseur".equalsIgnoreCase(this.role) ? Color.RED : Color.YELLOW;

                // Choix Sprite (Capture ou Normal)
                BufferedImage[][] spritesActifs = (estCapture && spritesCapture != null) ? spritesCapture : sprites;

                // 1. OMBRE DU SAUT (Affichage)
                int hauteurSaut = 0;
                if (enSaut) {
                    long tempsEcoule = System.currentTimeMillis() - debutSaut;
                    double progression = (double) tempsEcoule / DUREE_SAUT;
                    hauteurSaut = (int) (20 * Math.sin(progression * Math.PI));
                    contexte.setColor(new Color(0, 0, 0, 80)); 
                    contexte.fillOval(x + 5, y + 28, 22, 10); 
                }

                // 2. DESSIN SPRITE (Avec décalage Y du saut)
                BufferedImage imgAffiche = null;
                if (spritesActifs != null) {
                    int col = 0; int lig = 0;
                    switch(direction) {
                        case 0: col = 0; lig = 0 + etapeAnimation; break; 
                        case 1: col = 0; lig = 2 + etapeAnimation; break; 
                        case 2: col = 1; lig = 0 + etapeAnimation; break; 
                        case 3: col = 1; lig = 2 + etapeAnimation; break; 
                    }
                    imgAffiche = spritesActifs[col][lig];
                }

                if (imgAffiche != null) {
                    // On applique hauteurSaut ici
                    contexte.drawImage(imgAffiche, x - 16, (y - 16) - hauteurSaut, 32, 32, null);
                } else {
                    contexte.setColor(couleurJoueur);
                    contexte.fillOval(x - 10, y - 10, 20, 20);
                }

                // 3. DESSIN PSEUDO (Avec décalage Y du saut)
                contexte.setColor(couleurJoueur); 
                contexte.setFont(fontStandard);
                int largeurTexte = contexte.getFontMetrics().stringWidth(pseudo);
                contexte.drawString(pseudo, x - (largeurTexte / 2), (y - 20) - hauteurSaut);

                // --- HUD & UI (Vient entièrement de la branche Main) ---
                
                // Message "CAPTURED"
                if (estCapture && (System.currentTimeMillis() - debutMessageCapture < 5000)) {
                    if ((System.currentTimeMillis() / 800) % 2 == 0) {
                        contexte.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 30));
                        String message = "VOUS AVEZ ÉTÉ CAPTURÉ !";
                        int msgW = contexte.getFontMetrics().stringWidth(message);
                        contexte.setColor(Color.BLACK); contexte.drawString(message, 320 - (msgW / 2) + 2, 480 + 2);
                        contexte.setColor(Color.RED); contexte.drawString(message, 320 - (msgW / 2), 480);
                    }
                }

                // Chrono & Survivants
                afficherHUD(contexte, connexion); 
            }
            requete.close();
            
            // Dessin des Attaques (Main)
            afficherAttaques(contexte, connexion);

        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // --- MÉTHODES UTILITAIRES & GETTERS/SETTERS ---

    private void afficherPanneauInfo() {
        JTextArea contenu = new JTextArea("--- RÈGLES ---\n1. Flèches pour bouger\n2. Buissons = Invisible\n3. Dresseur : Clic = Tir\n4. Pokémon : Fuyez !");
        contenu.setBackground(new Color(222, 184, 135)); 
        contenu.setForeground(new Color(101, 67, 33));    
        contenu.setFont(new Font("Serif", Font.BOLD, 14)); 
        contenu.setEditable(false);
        contenu.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JOptionPane.showMessageDialog(null, contenu, "Info", JOptionPane.PLAIN_MESSAGE);
    }

    private void afficherHUD(Graphics2D g, Connection c) throws SQLException {
        long tempsRestant = Math.max(0, DUREE_PARTIE - (System.currentTimeMillis() - this.debutPartie));
        String txtChrono = String.format("%02d:%02d", (tempsRestant/1000)/60, (tempsRestant/1000)%60);
        
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
        int wChrono = g.getFontMetrics().stringWidth(txtChrono);
        g.setColor(Color.BLACK); g.drawString(txtChrono, 620 - wChrono + 2, 32); 
        g.setColor(Color.WHITE); g.drawString(txtChrono, 620 - wChrono, 30);            

        PreparedStatement req = c.prepareStatement("SELECT COUNT(*) FROM joueurs WHERE role != 'Dresseur' AND statut = 'LIBRE'");
        ResultSet res = req.executeQuery();
        int nb = res.next() ? res.getInt(1) : 0;
        req.close();
        
        String txtSurv = "Survivants : " + nb;
        int wSurv = g.getFontMetrics().stringWidth(txtSurv);
        g.setColor(Color.BLACK); g.drawString(txtSurv, 620 - wSurv + 2, 62); 
        g.setColor(Color.WHITE); g.drawString(txtSurv, 620 - wSurv, 60);
        
        // Fin de partie
        // (Tu peux remettre le bloc de fin de partie ici si tu l'as dans ton main)
    }

    private void afficherAttaques(Graphics2D g, Connection c) throws SQLException {
        PreparedStatement req = c.prepareStatement("SELECT lat_actuelle, lon_actuelle FROM attaques WHERE type = 'MUBALL'");
        ResultSet res = req.executeQuery();
        g.setColor(Color.CYAN);
        while (res.next()) {
             int px = laCarte.longitudeEnPixel(res.getDouble("lon_actuelle"));
             int py = laCarte.latitudeEnPixel(res.getDouble("lat_actuelle"));
             g.fillOval(px - 4, py - 4, 8, 8); // Dessin simple si sprite manquant
        }
        req.close();
    }

    // Méthodes BDD (Main)
    private void recupererRole() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("SELECT role FROM joueurs WHERE pseudo = ?");
            req.setString(1, this.pseudo);
            ResultSet res = req.executeQuery();
            if (res.next()) this.role = res.getString("role");
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
                this.positionInitialisee = true;
            }
            req.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void chargerDebutPartie() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("SELECT debut FROM parties ORDER BY id DESC LIMIT 1");
            ResultSet res = req.executeQuery();
            if (res.next()) this.debutPartie = res.getTimestamp("debut").getTime();
            req.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void sauvegarderPositionBDD() {
        try {
            Connection con = SingletonJDBC.getInstance().getConnection();
            PreparedStatement req = con.prepareStatement("UPDATE joueurs SET latitude = ?, longitude = ? WHERE pseudo = ?");
            req.setDouble(1, this.maLatitude);
            req.setDouble(2, this.maLongitude);
            req.setString(3, this.pseudo);
            req.executeUpdate();
            req.close();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // Méthodes Publiques & Setters
    public void setToucheHaut(boolean etat) { this.toucheHaut = etat; }
    public void setToucheBas(boolean etat) { this.toucheBas = etat; }
    public void setToucheGauche(boolean etat) { this.toucheGauche = etat; }
    public void setToucheDroite(boolean etat) { this.toucheDroite = etat; }
    public String getRole() { return role; }
    public double getLatitude() { return maLatitude; }
    public double getLongitude() { return maLongitude; }

    // Méthode de tir (Main)
    public void lancerMuball(int xPixel, int yPixel) {
        if (!"Dresseur".equalsIgnoreCase(this.role)) return;
       try {
           double latClic = this.laCarte.pixelEnLatitude(yPixel);
           double lonClic = this.laCarte.pixelEnLongitude(xPixel);
           Connection connexion = SingletonJDBC.getInstance().getConnection();

           // Récupérer ma position actuelle pour le départ du tir
           double latDepart = 0.0, lonDepart = 0.0;
           try (PreparedStatement reqPos = connexion.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?")) {
               reqPos.setString(1, pseudo);
               ResultSet pos = reqPos.executeQuery();
               if (pos.next()) {
                   latDepart = pos.getDouble("latitude");
                   lonDepart = pos.getDouble("longitude");
               }
           }

           double deltaLat = latClic - latDepart;
           double deltaLon = lonClic - lonDepart;
           double distance = Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon);
           if (distance == 0) return;

           double facteur = 10.0 / distance; 
           double latFinale = latDepart + (deltaLat * facteur);
           double lonFinale = lonDepart + (deltaLon * facteur);

           PreparedStatement reqInsert = connexion.prepareStatement(
               "INSERT INTO attaques (attaquant, type, lat_actuelle, lon_actuelle, lat_cible, lon_cible) VALUES (?, 'MUBALL', ?, ?, ?, ?)"
           );
           reqInsert.setString(1, pseudo);
           reqInsert.setDouble(2, latDepart);
           reqInsert.setDouble(3, lonDepart);
           reqInsert.setDouble(4, latFinale); 
           reqInsert.setDouble(5, lonFinale);
           reqInsert.executeUpdate();
           reqInsert.close();
           
       } catch (SQLException ex) { ex.printStackTrace(); }
    }
}