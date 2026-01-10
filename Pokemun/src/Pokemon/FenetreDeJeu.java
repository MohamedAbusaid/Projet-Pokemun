package Pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

public class FenetreDeJeu extends JFrame implements ActionListener, KeyListener, MouseListener {

    private BufferedImage framebuffer;
    private Graphics2D contexte;
    private JLabel jLabel1;
    private Jeu jeu;
    private Timer timer;
    
    private final int LARGEUR = 640; 
    private final int HAUTEUR = 960; 

    // MODIFICATION ICI : On accepte le rôle envoyé par l'Accueil
    public FenetreDeJeu(String pseudo, String role) {
        
        // Configuration de la fenêtre
        this.setTitle("DuBrazil - Joueur : " + pseudo + " (" + role + ")");
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.jLabel1 = new JLabel();
        this.jLabel1.setPreferredSize(new java.awt.Dimension(LARGEUR, HAUTEUR));
        this.setContentPane(this.jLabel1);
        this.pack();

        // Buffer graphique
        this.framebuffer = new BufferedImage(LARGEUR, HAUTEUR, BufferedImage.TYPE_INT_ARGB);
        this.jLabel1.setIcon(new ImageIcon(framebuffer));
        this.contexte = this.framebuffer.createGraphics();

        // Création du jeu (On passe le rôle aussi !)
        this.jeu = new Jeu(pseudo, role);

        // Timer (Boucle de jeu - 25 FPS)
        this.timer = new Timer(40, this);
        this.timer.start();

        // Inputs
        this.addKeyListener(this);
        this.jLabel1.addMouseListener(this); // Écouteur sur le label pour la précision du tir
        
        this.setLocationRelativeTo(null);
    }

    // --- BOUCLE DE JEU ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!this.jeu.estTermine()) {
            this.jeu.miseAJour();
        }
        
        this.contexte.setColor(Color.BLACK);
        this.contexte.fillRect(0, 0, LARGEUR, HAUTEUR);
        this.jeu.rendu(contexte);
        this.jLabel1.repaint();
        
        // Optionnel : Arrêter le timer à la fin, ou laisser tourner pour les animations
        // if (this.jeu.estTermine()) this.timer.stop(); 
    }

    // --- CLAVIER ---
    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_UP) this.jeu.getAvatar().setToucheHaut(true);
        if (evt.getKeyCode() == KeyEvent.VK_DOWN) this.jeu.getAvatar().setToucheBas(true);
        if (evt.getKeyCode() == KeyEvent.VK_RIGHT) this.jeu.getAvatar().setToucheDroite(true);
        if (evt.getKeyCode() == KeyEvent.VK_LEFT) this.jeu.getAvatar().setToucheGauche(true);
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_UP) this.jeu.getAvatar().setToucheHaut(false);
        if (evt.getKeyCode() == KeyEvent.VK_DOWN) this.jeu.getAvatar().setToucheBas(false);
        if (evt.getKeyCode() == KeyEvent.VK_RIGHT) this.jeu.getAvatar().setToucheDroite(false);
        if (evt.getKeyCode() == KeyEvent.VK_LEFT) this.jeu.getAvatar().setToucheGauche(false);
    }
    @Override public void keyTyped(KeyEvent evt) {}

    // --- SOURIS (TIR) ---
    @Override
    public void mousePressed(MouseEvent evt) {
        if (this.jeu.getAvatar() != null) {
            this.jeu.getAvatar().lancerMuball(evt.getX(), evt.getY());
        }
    }
    @Override public void mouseClicked(MouseEvent evt) {}
    @Override public void mouseReleased(MouseEvent evt) {}
    @Override public void mouseEntered(MouseEvent evt) {}
    @Override public void mouseExited(MouseEvent evt) {}

    // Main de test rapide (sans passer par l'accueil)
    public static void main(String[] args) {
        new FenetreDeJeu("Testeur", "Dresseur").setVisible(true);
    }
}