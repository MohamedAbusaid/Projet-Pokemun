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

    private boolean toucheHaut, toucheBas, toucheDroite, toucheGauche;
    private String pseudo;
    private String role; 
    protected Carte laCarte;
    private double maLatitude;
    private double maLongitude;
    private boolean positionInitialisee = false;

    private BufferedImage[][] sprites; 
    private BufferedImage[][] spritesCapture; 
    private int direction = 1; 
    private int etapeAnimation = 0; 
    private long dernierChangement = 0; 

    // PHYSIQUE SAUT
    private boolean enSaut = false;
    private long debutSaut = 0;
    private double latDepartSaut = 0;
    private final int DUREE_SAUT = 500; 
    private final double DISTANCE_SAUT = 0.00015;

    // GAMEPLAY
    private long debutMessageCapture = 0; 
    private boolean etaitCapture = false; 
    private long debutPartie = 0;
    private final long DUREE_PARTIE = 60 * 1000; 
    private final double VITESSE_LAT = 0.000015; 
    private final double VITESSE_LON = 0.000060; 

    public Avatar(Carte laCarte, String pseudoJoueur) {
        this.laCarte = laCarte;
        this.pseudo = pseudoJoueur; 

        recupererRole(); 
        chargerPositionInitiale();
        chargerDebutPartie(); 

        try {
            String nomImage = "Dresseur".equalsIgnoreCase(role) ? "/resources/Dresseur.png" : "/resources/" + role + ".png"; 
            BufferedImage planche = ImageIO.read(getClass().getResource(nomImage));
            this.sprites = decouperPlanche(planche);

            if (!"Dresseur".equalsIgnoreCase(role)) {
                try {
                    String nomImageCapture = "/resources/" + role + "_Capture.png";
                    BufferedImage plancheCapture = ImageIO.read(getClass().getResource(nomImageCapture));
                    this.spritesCapture = decouperPlanche(plancheCapture);
                } catch (Exception e) { System.err.println("Pas d'image capture pour " + role); }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private BufferedImage[][] decouperPlanche(BufferedImage planche) {
        BufferedImage[][] tuiles = new BufferedImage[2][4];
        for (int c = 0; c < 2; c++) for (int l = 0; l < 4; l++) tuiles[c][l] = planche.getSubimage(c*32, l*32, 32, 32);
        return tuiles;
    }

    public void miseAJour() {
        if (!positionInitialisee) return;

        if (enSaut) {
            long tempsEcoule = System.currentTimeMillis() - debutSaut;
            if (tempsEcoule >= DUREE_SAUT) {
                enSaut = false;
                this.maLatitude = latDepartSaut - DISTANCE_SAUT; 
            } else {
                double progression = (double) tempsEcoule / DUREE_SAUT;
                this.maLatitude = latDepartSaut - (DISTANCE_SAUT * progression);
            }
            return; 
        }

        double futurLat = maLatitude;
        double futurLon = maLongitude;
        boolean enMouvement = false;

        double offsetPieds = 0.00005; 
        int tuileSousPieds = laCarte.getTuileID(this.maLatitude - offsetPieds, this.maLongitude);
        boolean surRebord = (tuileSousPieds == 31 || tuileSousPieds == 32 || tuileSousPieds == 3);

        if (surRebord && !toucheHaut && (toucheBas || laCarte.getTuileID(this.maLatitude, this.maLongitude) == 31)) {
             enSaut = true;
             debutSaut = System.currentTimeMillis();
             latDepartSaut = this.maLatitude;
             return; 
        }

        if (toucheHaut) {
            double testLat = maLatitude + VITESSE_LAT;
            if (laCarte.getTuileID(testLat, maLongitude) == 4) {
                afficherPanneauInfo();
                toucheHaut = false;
                return;
            }
            futurLat += VITESSE_LAT; direction = 0; enMouvement = true;
        } else if (toucheBas) {
            futurLat -= VITESSE_LAT; direction = 1; enMouvement = true;
        } else if (toucheGauche) {
            futurLon -= VITESSE_LON; direction = 2; enMouvement = true;
        } else if (toucheDroite) {
            futurLon += VITESSE_LON; direction = 3; enMouvement = true;
        }

        if (enMouvement) {
            if (System.currentTimeMillis() - dernierChangement > 200) {
                etapeAnimation = (etapeAnimation + 1) % 2;
                dernierChangement = System.currentTimeMillis();
            }
            if (laCarte.estTraversable(futurLat, futurLon, this.direction)) {
                this.maLatitude = futurLat;
                this.maLongitude = futurLon;
                sauvegarderPositionBDD();
                
                int typeSol = laCarte.getTuileID(futurLat, futurLon);
                if ((typeSol == 1 || typeSol == 2) && Math.random() < 0.002) {
                    System.out.println("!!! Un Pokémon sauvage apparaît !!!");
                }
            } 
        } else {
            etapeAnimation = 0;
        }
    }

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

                if (estCapture && !etaitCapture) { debutMessageCapture = System.currentTimeMillis(); etaitCapture = true; }
                else if (!estCapture) { etaitCapture = false; }

                int x = laCarte.longitudeEnPixel(longitude);
                int y = laCarte.latitudeEnPixel(latitude);
                Color couleurJoueur = "Dresseur".equalsIgnoreCase(this.role) ? Color.RED : Color.YELLOW;
                BufferedImage[][] spritesActifs = (estCapture && spritesCapture != null) ? spritesCapture : sprites;

                int hauteurSaut = 0;
                if (enSaut) {
                    long tempsEcoule = System.currentTimeMillis() - debutSaut;
                    double progression = (double) tempsEcoule / DUREE_SAUT;
                    hauteurSaut = (int) (20 * Math.sin(progression * Math.PI));
                    contexte.setColor(new Color(0, 0, 0, 80)); contexte.fillOval(x + 5, y + 28, 22, 10); 
                }

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

                if (imgAffiche != null) contexte.drawImage(imgAffiche, x - 16, (y - 16) - hauteurSaut, 32, 32, null);
                else { contexte.setColor(couleurJoueur); contexte.fillOval(x - 10, y - 10, 20, 20); }

                contexte.setColor(couleurJoueur); contexte.setFont(fontStandard);
                int largeurTexte = contexte.getFontMetrics().stringWidth(pseudo);
                contexte.drawString(pseudo, x - (largeurTexte / 2), (y - 20) - hauteurSaut);

                afficherHUD(contexte, connexion, estCapture);
            }
            requete.close();
            afficherAttaques(contexte, connexion);

        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void afficherPanneauInfo() {
        JTextArea contenu = new JTextArea("--- RÈGLES ---\n1. Flèches pour bouger\n2. Buissons = Invisible\n3. Dresseur : Clic = Tir\n4. Pokémon : Fuyez !");
        contenu.setBackground(new Color(222, 184, 135)); contenu.setForeground(new Color(101, 67, 33));    
        contenu.setFont(new Font("Serif", Font.BOLD, 14)); contenu.setEditable(false);
        contenu.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JOptionPane.showMessageDialog(null, contenu, "Info", JOptionPane.PLAIN_MESSAGE);
    }

    private void afficherHUD(Graphics2D g, Connection c, boolean estCapture) throws SQLException {
        if (estCapture && (System.currentTimeMillis() - debutMessageCapture < 5000)) {
            if ((System.currentTimeMillis() / 800) % 2 == 0) {
                g.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 30));
                String msg = "VOUS AVEZ ÉTÉ CAPTURÉ !";
                int w = g.getFontMetrics().stringWidth(msg);
                g.setColor(Color.BLACK); g.drawString(msg, 320 - (w/2) + 2, 480 + 2);
                g.setColor(Color.RED); g.drawString(msg, 320 - (w/2), 480);
            }
        }
        long tempsRestant = Math.max(0, DUREE_PARTIE - (System.currentTimeMillis() - this.debutPartie));
        String txtChrono = String.format("%02d:%02d", (tempsRestant/1000)/60, (tempsRestant/1000)%60);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
        int wC = g.getFontMetrics().stringWidth(txtChrono);
        g.setColor(Color.BLACK); g.drawString(txtChrono, 620 - wC + 2, 32); 
        g.setColor(Color.WHITE); g.drawString(txtChrono, 620 - wC, 30);            

        PreparedStatement req = c.prepareStatement("SELECT COUNT(*) FROM joueurs WHERE role != 'Dresseur' AND statut = 'LIBRE'");
        ResultSet res = req.executeQuery();
        int nb = res.next() ? res.getInt(1) : 0; req.close();
        
        String txtS = "Survivants : " + nb;
        int wS = g.getFontMetrics().stringWidth(txtS);
        g.setColor(Color.BLACK); g.drawString(txtS, 620 - wS + 2, 62); 
        g.setColor(Color.WHITE); g.drawString(txtS, 620 - wS, 60);
        
        boolean partieFinie = (tempsRestant == 0) || (Jeu.gagnant != null && !Jeu.gagnant.isEmpty());
        if (partieFinie && (System.currentTimeMillis() / 500) % 2 != 0) {
            String l1 = "DRESSEUR".equals(Jeu.gagnant) ? "PARTIE TERMINÉE !" : "TEMPS ÉCOULÉ !";
            String l2 = "DRESSEUR".equals(Jeu.gagnant) ? "VICTOIRE DU DRESSEUR" : "VICTOIRE DES POKÉMONS";
            g.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 30));
            int w1 = g.getFontMetrics().stringWidth(l1);
            int w2 = g.getFontMetrics().stringWidth(l2);
            g.setColor(Color.BLACK); g.drawString(l1, 320-(w1/2)+2, 400+2); g.drawString(l2, 320-(w2/2)+2, 440+2);
            g.setColor(Color.RED); g.drawString(l1, 320-(w1/2), 400); g.drawString(l2, 320-(w2/2), 440);
        }
    }

    private void afficherAttaques(Graphics2D g, Connection c) throws SQLException {
        PreparedStatement req = c.prepareStatement("SELECT lat_actuelle, lon_actuelle FROM attaques WHERE type = 'MUBALL'");
        ResultSet res = req.executeQuery();
        g.setColor(Color.CYAN);
        while (res.next()) {
             int px = laCarte.longitudeEnPixel(res.getDouble("lon_actuelle"));
             int py = laCarte.latitudeEnPixel(res.getDouble("lat_actuelle"));
             g.fillOval(px - 4, py - 4, 8, 8); 
        }
        req.close();
    }

    private void recupererRole() { try { Connection c = SingletonJDBC.getInstance().getConnection(); PreparedStatement r = c.prepareStatement("SELECT role FROM joueurs WHERE pseudo = ?"); r.setString(1, pseudo); ResultSet rs = r.executeQuery(); if(rs.next()) role = rs.getString("role"); r.close(); } catch(Exception e){} }
    private void chargerPositionInitiale() { try { Connection c = SingletonJDBC.getInstance().getConnection(); PreparedStatement r = c.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?"); r.setString(1, pseudo); ResultSet rs = r.executeQuery(); if(rs.next()){ maLatitude = rs.getDouble("latitude"); maLongitude = rs.getDouble("longitude"); positionInitialisee = true; } r.close(); } catch(Exception e){} }
    private void chargerDebutPartie() { try { Connection c = SingletonJDBC.getInstance().getConnection(); PreparedStatement r = c.prepareStatement("SELECT debut FROM parties ORDER BY id DESC LIMIT 1"); ResultSet rs = r.executeQuery(); if(rs.next()) debutPartie = rs.getTimestamp("debut").getTime(); r.close(); } catch(Exception e){} }
    private void sauvegarderPositionBDD() { try { Connection c = SingletonJDBC.getInstance().getConnection(); PreparedStatement r = c.prepareStatement("UPDATE joueurs SET latitude = ?, longitude = ? WHERE pseudo = ?"); r.setDouble(1, maLatitude); r.setDouble(2, maLongitude); r.setString(3, pseudo); r.executeUpdate(); r.close(); } catch(Exception e){} }

    public void setToucheHaut(boolean e) { toucheHaut = e; }
    public void setToucheBas(boolean e) { toucheBas = e; }
    public void setToucheGauche(boolean e) { toucheGauche = e; }
    public void setToucheDroite(boolean e) { toucheDroite = e; }
    public String getRole() { return role; }
    
    public void lancerMuball(int xPixel, int yPixel) {
        if (!"Dresseur".equalsIgnoreCase(this.role)) return;
        try {
            double latC = laCarte.pixelEnLatitude(yPixel); double lonC = laCarte.pixelEnLongitude(xPixel);
            Connection c = SingletonJDBC.getInstance().getConnection();
            double latD = 0, lonD = 0;
            PreparedStatement rP = c.prepareStatement("SELECT latitude, longitude FROM joueurs WHERE pseudo = ?"); rP.setString(1, pseudo); ResultSet rs = rP.executeQuery(); if(rs.next()){ latD=rs.getDouble("latitude"); lonD=rs.getDouble("longitude"); } rP.close();
            double dLat = latC-latD; double dLon = lonC-lonD; double dist = Math.sqrt(dLat*dLat + dLon*dLon);
            if(dist==0) return;
            double f = 10.0/dist;
            PreparedStatement rI = c.prepareStatement("INSERT INTO attaques (attaquant, type, lat_actuelle, lon_actuelle, lat_cible, lon_cible) VALUES (?, 'MUBALL', ?, ?, ?, ?)");
            rI.setString(1, pseudo); rI.setDouble(2, latD); rI.setDouble(3, lonD); rI.setDouble(4, latD+dLat*f); rI.setDouble(5, lonD+dLon*f);
            rI.executeUpdate(); rI.close();
        } catch(Exception e) { e.printStackTrace(); }
    }
}