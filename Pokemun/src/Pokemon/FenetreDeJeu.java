package Pokemon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Fenêtre principale du jeu DuBrazil.
 * Elle gère la boucle de jeu (Timer), l'affichage et les entrées clavier.
 * * @author Equipe DuBrazil
 */
public class FenetreDeJeu extends JFrame implements ActionListener, KeyListener, MouseListener {

    private BufferedImage framebuffer;
    private Graphics2D contexte;
    private JLabel jLabel1;
    private Jeu jeu;
    private Timer timer;
    
    // Dimensions de la fenêtre (à adapter selon votre écran)
    private final int LARGEUR = 640; // 30 tuiles de 32px
    private final int HAUTEUR = 960; // 20 tuiles de 32px

    /**
     * Constructeur principal
     * @param pseudo Le pseudo du joueur qui vient de se connecter
     */
    public FenetreDeJeu(String pseudo) {
        // Initialisation de la fenêtre
        this.setTitle("DuBrazil - Joueur : " + pseudo); // On affiche qui joue
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.jLabel1 = new JLabel();
        this.jLabel1.setPreferredSize(new java.awt.Dimension(LARGEUR, HAUTEUR));
        this.setContentPane(this.jLabel1);
        this.pack();

        // Création du buffer graphique (Double Buffering)
        this.framebuffer = new BufferedImage(this.jLabel1.getWidth(), this.jLabel1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.jLabel1.setIcon(new ImageIcon(framebuffer));
        this.contexte = this.framebuffer.createGraphics();

        // Création du jeu avec le pseudo du joueur connecté
        // IMPORTANT : Il faudra modifier le constructeur de la classe Jeu pour accepter ce pseudo !
        this.jeu = new Jeu(pseudo);

        // Création du Timer (Boucle de jeu)
        this.timer = new Timer(40, this);
        this.timer.start();

        // Ajout de l'écouteur clavier
        this.addKeyListener(this);
        
        // Ajout de l'écouteur souris
        this.addMouseListener(this);
        
        // Centrer la fenêtre à l'écran
        this.setLocationRelativeTo(null);
    }

    // --- BOUCLE DE JEU (Appelée toutes les 40ms) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Mise à jour logique (Déplacements, BDD, Collisions)
        this.jeu.miseAJour();
        
        // 2. Nettoyage de l'écran (Fond noir pour éviter les traces)
        this.contexte.setColor(Color.BLACK);
        this.contexte.fillRect(0, 0, this.jLabel1.getWidth(), this.jLabel1.getHeight());

        // 3. Rendu graphique (Dessin de la carte, des joueurs, etc.)
        this.jeu.rendu(contexte);
        
        // 4. Rafraîchissement de l'affichage
        this.jLabel1.repaint();
        
        // Vérification de fin de partie
        if (this.jeu.estTermine()) {
            this.timer.stop();
            System.out.println("Fin de la partie !");
        }
    }

    // --- GESTION CLAVIER ---
    
    @Override
    public void keyTyped(KeyEvent evt) {
        // Inutile ici
    }

    @Override
    public void keyPressed(KeyEvent evt) {
        // Quand on APPUIE sur une touche
        if (evt.getKeyCode() == KeyEvent.VK_UP) {
            this.jeu.getAvatar().setToucheHaut(true);
        }
        if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
            this.jeu.getAvatar().setToucheBas(true);
        }
        if (evt.getKeyCode() == KeyEvent.VK_RIGHT) {
            this.jeu.getAvatar().setToucheDroite(true);
        }
        if (evt.getKeyCode() == KeyEvent.VK_LEFT) {
            this.jeu.getAvatar().setToucheGauche(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        // Quand on RELÂCHE une touche (Essentiel pour arrêter le mouvement !)
        if (evt.getKeyCode() == KeyEvent.VK_UP) {
            this.jeu.getAvatar().setToucheHaut(false);
        }
        if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
            this.jeu.getAvatar().setToucheBas(false);
        }
        if (evt.getKeyCode() == KeyEvent.VK_RIGHT) {
            this.jeu.getAvatar().setToucheDroite(false);
        }
        if (evt.getKeyCode() == KeyEvent.VK_LEFT) {
            this.jeu.getAvatar().setToucheGauche(false);
        }
    }
    
    @Override
    public void mouseClicked(java.awt.event.MouseEvent evt) {
        if ("Dresseur".equalsIgnoreCase(this.jeu.getAvatar().getRole())) {
            this.jeu.getAvatar().lancerMuball(evt.getX(), evt.getY());
        }
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent evt) {}

    @Override
    public void mouseReleased(java.awt.event.MouseEvent evt) {}

    @Override
    public void mouseEntered(java.awt.event.MouseEvent evt) {}

    @Override
    public void mouseExited(java.awt.event.MouseEvent evt) {}

    // Main pour tester rapidement (simule une connexion)
    public static void main(String[] args) {
        FenetreDeJeu fenetre = new FenetreDeJeu("Moha");
        fenetre.setVisible(true);
    }
}