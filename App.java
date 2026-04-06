import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import javax.sound.sampled.*; 
import java.io.File;
import javax.imageio.ImageIO;

public class App extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel containerPanel = new JPanel(cardLayout);
    
    private MenuPanel menuPanel;
    private WindowPanel gamePanel;
    
    // --- KHAI BÁO CÁC BIẾN ÂM THANH ---
    public static MusicPlayer musicPlayer = new MusicPlayer();
    public static MusicPlayer foodSoundPlayer = new MusicPlayer();
    public static MusicPlayer gameOverSound = new MusicPlayer();
    public static MusicPlayer levelUpSound = new MusicPlayer();
    public static MusicPlayer clickSound = new MusicPlayer();
    
    // Mặc định âm thanh đang MỞ (Muted = false)
    public static boolean isMuted = false; 

    public App() {
        // --- LOAD CÁC FILE ÂM THANH Ở ĐÂY ---
        musicPlayer.loadMusic("../exitsignt.wav");
        foodSoundPlayer.loadMusic("../yoshi-tongue.wav"); 
        gameOverSound.loadMusic("../dead.wav");
        levelUpSound.loadMusic("../win.wav");
        clickSound.loadMusic("../click.wav");

        // Tự động phát nhạc ngay khi mở game
        if (!isMuted) {
            musicPlayer.play();
        }

        menuPanel = new MenuPanel(this);
        gamePanel = new WindowPanel(this);

        containerPanel.add(menuPanel, "Menu");
        containerPanel.add(gamePanel, "Game");

        this.add(containerPanel);
        this.pack();
        this.setTitle("Snake Game - By Son");
        this.setResizable(false);
        this.setLocationRelativeTo(null); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        cardLayout.show(containerPanel, "Menu");
    }

    public void showGameScreen(boolean isAI) {
        cardLayout.show(containerPanel, "Game");
        gamePanel.resetLevel();
        gamePanel.startGame(isAI); 
        gamePanel.requestFocusInWindow(); 
    }

    public void showMenuScreen() {
        cardLayout.show(containerPanel, "Menu");
        menuPanel.updateMusicButton(); 
    }

    public static void main(String[] args) {
        new App();
    }
}

// =======================================================
// LỚP MÀN HÌNH CHƠI GAME & CÁC MENU NỔI (OVERLAY)
// =======================================================
class WindowPanel extends JPanel implements ActionListener {

    private App parentApp;

    public static int TILE; 
    public static final int WIDTH_TILES = 25;
    public static final int HEIGHT_TILES = 20;
    
    public static int WIDTH;
    public static int HEIGHT;
    
    public static int PAD_X;
    public static int PAD_TOP;
    public static int PAD_BOT;
    
    public static int TOTAL_WIDTH;
    public static int TOTAL_HEIGHT;

    static {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int padXTiles = 4; 
        int padYTiles = 5; 
        
        int maxTileW = (screenSize.width - 50) / (WIDTH_TILES + padXTiles);
        int maxTileH = (screenSize.height - 80) / (HEIGHT_TILES + padYTiles);
        
        TILE = Math.min(maxTileW, maxTileH); 
        
        WIDTH = TILE * WIDTH_TILES;
        HEIGHT = TILE * HEIGHT_TILES;
        
        PAD_X = TILE * 2;
        PAD_TOP = (int)(TILE * 2.5);
        PAD_BOT = TILE * 2;
        
        TOTAL_WIDTH = WIDTH + PAD_X * 2;
        TOTAL_HEIGHT = HEIGHT + PAD_TOP + PAD_BOT;
    }

    private Timer timer = new Timer(120, this);

    private int fruitX, fruitY;
    
    private int bigFruitX, bigFruitY;
    private boolean isBigFruitActive = false;
    private int smallFruitCount = 0;
    private int bigFruitTimer = 0; 

    private int[] snakeX = new int[400];
    private int[] snakeY = new int[400];
    private int snakeLength;

    private char dir;

    private boolean gameOver = false;
    private boolean levelClear = false;
    private boolean gameFinished = false;
    private boolean isAIPlaying = false; 
    private boolean isPaused = false; 
    private boolean isSpeedRun = false;

    private int score;
    private int level = 1;

    private ArrayList<Point> walls = new ArrayList<>();

    private static final int[][] LEVEL_1 = new int[20][25];
    private static final int[][] LEVEL_2 = new int[20][25];
    private static final int[][] LEVEL_3 = new int[20][25];

    private BufferedImage backgroundImage;
    
    private JButton btnSound; 
    
    // --- CÁC THÀNH PHẦN MENU NỔI (OVERLAY) ---
    private JPanel pauseMenuPanel;
    private JPanel gameOverPanel;
    private JPanel levelClearPanel;
    private JPanel gameFinishedPanel;
    
    private JButton btnPauseSound; 

    public void resetLevel() {
        this.level = 1;
    }

    public WindowPanel(App parentApp) {
        this.parentApp = parentApp;
        setPreferredSize(new Dimension(TOTAL_WIDTH, TOTAL_HEIGHT)); 
        setFocusable(true);
        setLayout(null); 

       

        initMaps();
        
        // --- TẠO CÁC BẢNG MENU NỔI ---
        createPauseMenu();
        createGameOverMenu();
        createLevelClearMenu();
        createGameFinishedMenu();
        
        // Nút góc phải màn hình
        btnSound = new JButton("Sound: ON");
        btnSound.setFont(new Font("Comic Sans MS", Font.BOLD, (int)(TILE * 0.6)));
        btnSound.setBackground(new Color(22, 56, 82)); 
        btnSound.setForeground(Color.WHITE);
        btnSound.setFocusPainted(false);
        btnSound.setFocusable(false); 
        btnSound.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        int btnW = (int)(TILE * 4.5);
        int btnH = (int)(TILE * 1.2);
        btnSound.setBounds(TOTAL_WIDTH - PAD_X - btnW, TOTAL_HEIGHT - PAD_BOT/2 - btnH/2, btnW, btnH);
        
        btnSound.addActionListener(e -> {
            if (!App.isMuted) App.clickSound.playOnce(); 
            App.isMuted = !App.isMuted;
            updateSoundButtonState();
            if (App.isMuted) App.musicPlayer.stop();
            else {
                App.clickSound.playOnce(); 
                App.musicPlayer.play();
            }
            requestFocusInWindow(); 
        });
        add(btnSound);
        
        setupKeyBindings(); 
    }

    // ==========================================
    // CÁC HÀM TẠO BẢNG MENU NỔI (OVERLAY PANELS)
    // ==========================================
    
    private void createPauseMenu() {
        pauseMenuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(0, 0, 0, 200)); 
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        pauseMenuPanel.setOpaque(false);
        pauseMenuPanel.setLayout(new GridBagLayout());
        pauseMenuPanel.setBounds(PAD_X, PAD_TOP, WIDTH, HEIGHT);
        pauseMenuPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("TẠM DỪNG");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, (int)(TILE * 2.5)));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 30, 10); 
        pauseMenuPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(10, 10, 10, 10);

        JButton btnResume = createCustomButton("Tiếp tục chơi");
        btnResume.addActionListener(e -> togglePause());
        gbc.gridy = 1;
        pauseMenuPanel.add(btnResume, gbc);

        btnPauseSound = createCustomButton(App.isMuted ? "Âm thanh: OFF" : "Âm thanh: ON");
        btnPauseSound.addActionListener(e -> {
            App.isMuted = !App.isMuted;
            updateSoundButtonState(); 
            if (App.isMuted) App.musicPlayer.stop();
            else App.musicPlayer.play();
            requestFocusInWindow();
        });
        gbc.gridy = 2;
        pauseMenuPanel.add(btnPauseSound, gbc);

        JButton btnMainMenu = createCustomButton("Trở về Menu");
        btnMainMenu.addActionListener(e -> returnToMenu());
        gbc.gridy = 3;
        pauseMenuPanel.add(btnMainMenu, gbc);

        add(pauseMenuPanel);
        setComponentZOrder(pauseMenuPanel, 0); 
    }

    private void createGameOverMenu() {
        gameOverPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(150, 0, 0, 200)); 
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        gameOverPanel.setOpaque(false);
        gameOverPanel.setLayout(new GridBagLayout());
        gameOverPanel.setBounds(PAD_X, PAD_TOP, WIDTH, HEIGHT);
        gameOverPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("GAME OVER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, (int)(TILE * 3.5)));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 40, 10); 
        gameOverPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(10, 10, 10, 10);

        JButton btnRetry = createCustomButton("Chơi lại");
        btnRetry.addActionListener(e -> { level = 1; startGame(isAIPlaying); });
        gbc.gridy = 1;
        gameOverPanel.add(btnRetry, gbc);

        JButton btnMainMenu = createCustomButton("Trở về Menu");
        btnMainMenu.addActionListener(e -> returnToMenu());
        gbc.gridy = 2;
        gameOverPanel.add(btnMainMenu, gbc);

        add(gameOverPanel);
        setComponentZOrder(gameOverPanel, 0); 
    }

    private void createLevelClearMenu() {
        levelClearPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(46, 204, 113, 200)); 
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        levelClearPanel.setOpaque(false);
        levelClearPanel.setLayout(new GridBagLayout());
        levelClearPanel.setBounds(PAD_X, PAD_TOP, WIDTH, HEIGHT);
        levelClearPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("LEVEL CLEAR!");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, (int)(TILE * 3.5)));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 40, 10); 
        levelClearPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(10, 10, 10, 10);

        JButton btnNext = createCustomButton("Màn tiếp theo");
        btnNext.addActionListener(e -> { level++; startGame(isAIPlaying); });
        gbc.gridy = 1;
        levelClearPanel.add(btnNext, gbc);

        JButton btnMainMenu = createCustomButton("Trở về Menu");
        btnMainMenu.addActionListener(e -> returnToMenu());
        gbc.gridy = 2;
        levelClearPanel.add(btnMainMenu, gbc);

        add(levelClearPanel);
        setComponentZOrder(levelClearPanel, 0); 
    }

    private void createGameFinishedMenu() {
        gameFinishedPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(19, 70, 31, 220)); 
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        gameFinishedPanel.setOpaque(false);
        gameFinishedPanel.setLayout(new GridBagLayout());
        gameFinishedPanel.setBounds(PAD_X, PAD_TOP, WIDTH, HEIGHT);
        gameFinishedPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("CHÚC MỪNG");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, (int)(TILE * 3.5)));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gameFinishedPanel.add(titleLabel, gbc);
        
        JLabel subLabel = new JLabel("Ông Chủ Thắng Lớn!");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, (int)(TILE * 1.5)));
        subLabel.setForeground(Color.WHITE);
        subLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 10, 30, 10);
        gameFinishedPanel.add(subLabel, gbc);

        gbc.insets = new Insets(10, 10, 10, 10);

        JButton btnReplay = createCustomButton("Chơi lại từ đầu");
        btnReplay.addActionListener(e -> { level = 1; startGame(isAIPlaying); });
        gbc.gridy = 2;
        gameFinishedPanel.add(btnReplay, gbc);

        JButton btnMainMenu = createCustomButton("Trở về Menu");
        btnMainMenu.addActionListener(e -> returnToMenu());
        gbc.gridy = 3;
        gameFinishedPanel.add(btnMainMenu, gbc);

        add(gameFinishedPanel);
        setComponentZOrder(gameFinishedPanel, 0); 
    }

    // Nút dùng chung cho tất cả Menu
    private JButton createCustomButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isRollover()) g2.setColor(new Color(39, 174, 96));
                else if (getModel().isPressed()) g2.setColor(new Color(23, 100, 50));
                else g2.setColor(new Color(46, 204, 113)); 
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(39, 174, 96));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, (int)(TILE * 1.1)));
        btn.setPreferredSize(new Dimension((int)(TILE * 9), (int)(TILE * 2)));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- Gắn sự kiện click sound ---
        btn.addActionListener(e -> {
            if (!App.isMuted) App.clickSound.playOnce();
        });

        return btn;
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ SỰ KIỆN TRẠNG THÁI GAME
    // ==========================================

    private void togglePause() {
        if (!gameOver && !levelClear && !gameFinished) {
            isPaused = !isPaused;
            pauseMenuPanel.setVisible(isPaused); 
            if (!isPaused) requestFocusInWindow(); 
            repaint();
        }
    }
    
    private void triggerGameOver() {
        gameOver = true;
        timer.stop();
        if (!App.isMuted) App.gameOverSound.playOnce(); 
        gameOverPanel.setVisible(true);
    }
    
    private void triggerLevelClear() {
        levelClear = true;
        timer.stop();
        if (!App.isMuted) App.levelUpSound.playOnce(); 
        levelClearPanel.setVisible(true);
    }
    
    private void triggerGameFinished() {
        gameFinished = true;
        timer.stop();
        if (!App.isMuted) App.levelUpSound.playOnce(); 
        gameFinishedPanel.setVisible(true);
    }
    
    private void returnToMenu() {
        timer.stop();
        gameOver = false;
        levelClear = false;
        gameFinished = false;
        isPaused = false;
        pauseMenuPanel.setVisible(false);
        gameOverPanel.setVisible(false);
        levelClearPanel.setVisible(false);
        gameFinishedPanel.setVisible(false);
        parentApp.showMenuScreen();
    }

    private void updateSoundButtonState() {
        if (App.isMuted) {
            btnSound.setText("Sound: OFF");
            btnSound.setBackground(new Color(231, 76, 60)); 
            if (btnPauseSound != null) btnPauseSound.setText("Âm thanh: OFF");
        } else {
            btnSound.setText("Sound: ON");
            btnSound.setBackground(new Color(22, 56, 82)); 
            if (btnPauseSound != null) btnPauseSound.setText("Âm thanh: ON");
        }
    }

    // ==========================================
    // LOGIC GAME CHÍNH
    // ==========================================

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "space_action"); 
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "pause");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "speed");

        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && !isPaused && dir != 'l') dir = 'r'; }
        });
        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && !isPaused && dir != 'r') dir = 'l'; }
        });
        am.put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && !isPaused && dir != 'd') dir = 'u'; }
        });
        am.put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && !isPaused && dir != 'u') dir = 'd'; }
        });

        am.put("space_action", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver) { 
                    level = 1; startGame(isAIPlaying); 
                } else if (!levelClear && !gameFinished) {
                    togglePause(); 
                }
            }
        });

        am.put("pause", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { togglePause(); }
        });

        am.put("escape", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { returnToMenu(); }
        });

        am.put("next", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (levelClear && level < 3) { level++; startGame(isAIPlaying); }
            }
        });

        am.put("speed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isAIPlaying && !gameOver && !levelClear && !gameFinished && !isPaused) {
                    isSpeedRun = !isSpeedRun;
                    timer.setDelay(isSpeedRun ? 15 : 120);
                }
            }
        });
    }

    private void initMaps() {
        for(int r=8; r<=11; r++) { for(int c=10; c<=14; c++) LEVEL_1[r][c] = 1; }
        for(int r=2; r<=7; r++) { LEVEL_1[r][3]=1; LEVEL_1[r][4]=1; LEVEL_1[r][20]=1; LEVEL_1[r][21]=1; }
        for(int c=5; c<=8; c++) { LEVEL_1[6][c]=1; LEVEL_1[7][c]=1; }
        for(int c=16; c<=19; c++) { LEVEL_1[6][c]=1; LEVEL_1[7][c]=1; }
        for(int r=12; r<=17; r++) { LEVEL_1[r][3]=1; LEVEL_1[r][4]=1; LEVEL_1[r][20]=1; LEVEL_1[r][21]=1; }
        for(int c=5; c<=8; c++) { LEVEL_1[12][c]=1; LEVEL_1[13][c]=1; }
        for(int c=16; c<=19; c++) { LEVEL_1[12][c]=1; LEVEL_1[13][c]=1; }

        for(int r=0; r<=6; r++) { LEVEL_2[r][3]=1; LEVEL_2[r][4]=1; LEVEL_2[r][20]=1; LEVEL_2[r][21]=1; }
        for(int r=13; r<=19; r++) { LEVEL_2[r][3]=1; LEVEL_2[r][4]=1; LEVEL_2[r][20]=1; LEVEL_2[r][21]=1; }
        for(int r=2; r<=6; r++) { LEVEL_2[r][9]=1; LEVEL_2[r][10]=1; LEVEL_2[r][14]=1; LEVEL_2[r][15]=1; } 
        for(int c=11; c<=13; c++) { LEVEL_2[5][c]=1; LEVEL_2[6][c]=1; LEVEL_2[13][c]=1; LEVEL_2[14][c]=1; } 
        for(int r=13; r<=17; r++) { LEVEL_2[r][9]=1; LEVEL_2[r][10]=1; LEVEL_2[r][14]=1; LEVEL_2[r][15]=1; } 

        for(int r=0; r<=7; r++) { LEVEL_3[r][3]=1; LEVEL_3[r][4]=1; LEVEL_3[r][20]=1; LEVEL_3[r][21]=1; }
        for(int c=5; c<=8; c++) { LEVEL_3[6][c]=1; LEVEL_3[7][c]=1; }
        for(int c=16; c<=19; c++) { LEVEL_3[6][c]=1; LEVEL_3[7][c]=1; }
        for(int c=0; c<=22; c++) LEVEL_3[10][c] = 1; 
        for(int r=10; r<=18; r++) LEVEL_3[r][22] = 1; 
        for(int c=3; c<=22; c++) LEVEL_3[18][c] = 1; 
        for(int r=12; r<=18; r++) { LEVEL_3[r][6]=1; LEVEL_3[r][7]=1; LEVEL_3[r][14]=1; LEVEL_3[r][15]=1; } 
        for(int r=11; r<=15; r++) { LEVEL_3[r][10]=1; LEVEL_3[r][11]=1; } 
        for(int c=16; c<=19; c++) { LEVEL_3[14][c]=1; LEVEL_3[15][c]=1; } 
    }

    public void startGame(boolean aiMode) { 
        this.isAIPlaying = aiMode; 
        snakeLength = 1;
        
        if (level == 1) { snakeX[0] = TILE * 12; snakeY[0] = TILE * 4; } 
        else if (level == 2) { snakeX[0] = TILE * 12; snakeY[0] = TILE * 9; } 
        else if (level == 3) { snakeX[0] = TILE * 12; snakeY[0] = TILE * 4; }
        dir = 'r';

        score = 0;
        smallFruitCount = 0;
        isBigFruitActive = false;
        bigFruitTimer = 0;
        
        gameOver = false;
        levelClear = false;
        gameFinished = false;
        isPaused = false; 
        isSpeedRun = false;
        
        pauseMenuPanel.setVisible(false);
        gameOverPanel.setVisible(false);
        levelClearPanel.setVisible(false);
        gameFinishedPanel.setVisible(false);
        
        timer.setDelay(120);

        updateSoundButtonState();

        loadLevel();
        spawnFruit();
        timer.start();
    }

    private void loadLevel() {
        walls.clear();
        int[][] map = (level == 1) ? LEVEL_1 : (level == 2) ? LEVEL_2 : LEVEL_3;
        for (int r = 0; r < HEIGHT_TILES; r++) {
            for (int c = 0; c < WIDTH_TILES; c++) {
                if (map[r][c] == 1) walls.add(new Point(c * TILE, r * TILE));
            }
        }
    }

    private void spawnFruit() {
        Random rand = new Random();
        while (true) {
            fruitX = rand.nextInt(WIDTH_TILES) * TILE;
            fruitY = rand.nextInt(HEIGHT_TILES) * TILE;
            Point p = new Point(fruitX, fruitY);
            if (!walls.contains(p)) {
                boolean onSnake = false;
                for(int i=0; i<snakeLength; i++) {
                    if(snakeX[i] == fruitX && snakeY[i] == fruitY) onSnake = true;
                }
                if (isBigFruitActive && fruitX == bigFruitX && fruitY == bigFruitY) {
                    onSnake = true; 
                }
                if(!onSnake) break; 
            }
        }
    }

    private void spawnBigFruit() {
        Random rand = new Random();
        while (true) {
            bigFruitX = rand.nextInt(WIDTH_TILES) * TILE;
            bigFruitY = rand.nextInt(HEIGHT_TILES) * TILE;
            Point p = new Point(bigFruitX, bigFruitY);
            if (!walls.contains(p)) {
                boolean onSnake = false;
                for(int i=0; i<snakeLength; i++) {
                    if(snakeX[i] == bigFruitX && snakeY[i] == bigFruitY) onSnake = true;
                }
                if (bigFruitX == fruitX && bigFruitY == fruitY) {
                    onSnake = true; 
                }
                if(!onSnake) break; 
            }
        }
    }

    class Node {
        int c, r;
        char initialMove;
        Node(int c, int r, char initialMove) {
            this.c = c; this.r = r; this.initialMove = initialMove;
        }
    }

    private char calculateAIMove() {
        int headC = snakeX[0] / TILE, headR = snakeY[0] / TILE;
        int targetC = fruitX / TILE, targetR = fruitY / TILE;
        if (isBigFruitActive) {
            targetC = bigFruitX / TILE;
            targetR = bigFruitY / TILE;
        }

        boolean[][] obstacles = new boolean[HEIGHT_TILES][WIDTH_TILES];
        for (Point w : walls) obstacles[w.y / TILE][w.x / TILE] = true;
        for (int i = 0; i < snakeLength - 1; i++) { 
            int c = snakeX[i] / TILE, r = snakeY[i] / TILE;
            if (c >= 0 && c < WIDTH_TILES && r >= 0 && r < HEIGHT_TILES) {
                obstacles[r][c] = true;
            }
        }

        int[] dc = {0, 0, -1, 1};
        int[] dr = {-1, 1, 0, 0};
        char[] dirs = {'u', 'd', 'l', 'r'};

        Queue<Node> queue = new LinkedList<>();
        boolean[][] visited = new boolean[HEIGHT_TILES][WIDTH_TILES];
        visited[headR][headC] = true;

        for (int i = 0; i < 4; i++) {
            int nc = headC + dc[i], nr = headR + dr[i];
            if (nc >= 0 && nc < WIDTH_TILES && nr >= 0 && nr < HEIGHT_TILES && !obstacles[nr][nc]) {
                if ((dir == 'u' && dirs[i] == 'd') || (dir == 'd' && dirs[i] == 'u') ||
                    (dir == 'l' && dirs[i] == 'r') || (dir == 'r' && dirs[i] == 'l')) continue;

                visited[nr][nc] = true;
                queue.add(new Node(nc, nr, dirs[i])); 
            }
        }

        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            if (curr.c == targetC && curr.r == targetR) return curr.initialMove; 
            for (int i = 0; i < 4; i++) {
                int nc = curr.c + dc[i], nr = curr.r + dr[i];
                if (nc >= 0 && nc < WIDTH_TILES && nr >= 0 && nr < HEIGHT_TILES && !obstacles[nr][nc] && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.add(new Node(nc, nr, curr.initialMove)); 
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            int nc = headC + dc[i], nr = headR + dr[i];
            if (nc >= 0 && nc < WIDTH_TILES && nr >= 0 && nr < HEIGHT_TILES && !obstacles[nr][nc]) {
                if ((dir == 'u' && dirs[i] == 'd') || (dir == 'd' && dirs[i] == 'u') ||
                    (dir == 'l' && dirs[i] == 'r') || (dir == 'r' && dirs[i] == 'l')) continue;
                return dirs[i];
            }
        }
        return dir; 
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. NỀN CARO
        g2d.setColor(new Color(248, 249, 250)); 
        g2d.fillRect(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);
        g2d.setColor(new Color(230, 230, 230)); 
        for(int i = 0; i < TOTAL_WIDTH; i += TILE) g2d.drawLine(i, 0, i, TOTAL_HEIGHT);
        for(int i = 0; i < TOTAL_HEIGHT; i += TILE) g2d.drawLine(0, i, TOTAL_WIDTH, i);
        
        g2d.setColor(new Color(0, 168, 89)); 
        g2d.fillOval(TOTAL_WIDTH - TILE*5, TOTAL_HEIGHT - TILE*3, TILE*8, TILE*5);
        g2d.fillOval(-TILE*2, TOTAL_HEIGHT - TILE*4, TILE*6, TILE*5);
        g2d.setColor(new Color(255, 193, 7)); 
        g2d.fillOval(TOTAL_WIDTH - TILE*4, -TILE*2, TILE*6, TILE*4);
        g2d.setColor(new Color(171, 190, 251)); 
        g2d.fillOval(-TILE*3, TILE*4, TILE*4, TILE*3);

        // 2. BẢNG TÊN & ĐIỂM
        g2d.setColor(new Color(22, 56, 82)); 
        g2d.fillRoundRect(PAD_X, PAD_TOP/2 - (int)(TILE*1.2), TILE*7, (int)(TILE*1.8), 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, (int)(TILE * 0.7)));
        String nameTxt = isAIPlaying ? "AI MODE" : "Player: ThinhNoP";
        if(isSpeedRun) nameTxt += " (SPEED)";
        g2d.drawString(nameTxt, PAD_X + 15, PAD_TOP/2);

        g2d.setColor(new Color(240, 101, 67)); 
        String topScoreTxt = "Score: " + score;
        FontMetrics fmTop = g2d.getFontMetrics();
        int sBoxW = fmTop.stringWidth(topScoreTxt) + 30;
        g2d.fillRoundRect(TOTAL_WIDTH - PAD_X - sBoxW, PAD_TOP/2 - (int)(TILE*1.2), sBoxW, (int)(TILE*1.8), 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawString(topScoreTxt, TOTAL_WIDTH - PAD_X - sBoxW + 15, PAD_TOP/2);

        // 3. KHUNG GAME
        g2d.setColor(new Color(22, 56, 82)); 
        g2d.fillRoundRect(PAD_X - 10, PAD_TOP - 10, WIDTH + 20, HEIGHT + 20, 35, 35);

        // 4. CHUYỂN TỌA ĐỘ 
        Shape oldClip = g2d.getClip();
        g2d.clipRect(PAD_X, PAD_TOP, WIDTH, HEIGHT);
        g2d.translate(PAD_X, PAD_TOP); 

        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, this);
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            g2d.setColor(new Color(253, 246, 227)); 
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // --- VẼ CHI TIẾT GAME ---
        boolean isBrightBg = (backgroundImage == null);
        for (Point w : walls) {
            if(isBrightBg) {
                g2d.setColor(new Color(143, 104, 76)); 
                g2d.fillRoundRect(w.x + 1, w.y + 1, TILE - 2, TILE - 2, 8, 8);
            } else {
                g2d.setColor(new Color(41, 128, 185, 220)); 
                g2d.fillRoundRect(w.x + 1, w.y + 1, TILE - 2, TILE - 2, 12, 12);
                g2d.setColor(new Color(52, 152, 219));
                g2d.drawRoundRect(w.x + 1, w.y + 1, TILE - 2, TILE - 2, 12, 12);
            }
        }

        g2d.setColor(new Color(231, 76, 60)); 
        g2d.fillOval(fruitX + 2, fruitY + 2, TILE - 4, TILE - 4);
        g2d.setColor(new Color(46, 204, 113)); 
        g2d.fillOval(fruitX + TILE/2, fruitY, TILE/3, TILE/4);

        if (isBigFruitActive) {
            int pulse = (int)(Math.sin(System.currentTimeMillis() / 150.0) * (TILE / 6.0));
            g2d.setColor(new Color(139, 69, 19)); 
            g2d.fillOval(bigFruitX - pulse, bigFruitY - pulse, TILE + pulse*2, TILE + pulse*2);
            
            g2d.setColor(Color.WHITE);
            g2d.fillOval(bigFruitX + TILE/4, bigFruitY + TILE/4, TILE/4, TILE/4);
            g2d.fillOval(bigFruitX + TILE - TILE/2, bigFruitY + TILE/4, TILE/4, TILE/4);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(bigFruitX + TILE/4 + 2, bigFruitY + TILE/4 + 2, TILE/8, TILE/8);
            g2d.fillOval(bigFruitX + TILE - TILE/2 + 2, bigFruitY + TILE/4 + 2, TILE/8, TILE/8);
        }

        for (int i = 0; i < snakeLength; i++) {
            if (i == 0) {
                g2d.setColor(new Color(255, 204, 0)); 
                g2d.fillRoundRect(snakeX[i], snakeY[i], TILE, TILE, 15, 15);
                
                g2d.setColor(Color.WHITE);
                int eyeSize = TILE / 3;
                if (dir == 'u' || dir == 'd') {
                    g2d.fillOval(snakeX[i] + TILE/5, snakeY[i] + TILE/5, eyeSize, eyeSize);
                    g2d.fillOval(snakeX[i] + TILE - TILE/5 - eyeSize, snakeY[i] + TILE/5, eyeSize, eyeSize);
                    g2d.setColor(Color.BLACK); 
                    g2d.fillOval(snakeX[i] + TILE/5 + eyeSize/4, snakeY[i] + TILE/5 + eyeSize/4, eyeSize/2, eyeSize/2);
                    g2d.fillOval(snakeX[i] + TILE - TILE/5 - eyeSize + eyeSize/4, snakeY[i] + TILE/5 + eyeSize/4, eyeSize/2, eyeSize/2);
                } else {
                    g2d.fillOval(snakeX[i] + TILE/5, snakeY[i] + TILE/5, eyeSize, eyeSize);
                    g2d.fillOval(snakeX[i] + TILE/5, snakeY[i] + TILE - TILE/5 - eyeSize, eyeSize, eyeSize);
                    g2d.setColor(Color.BLACK); 
                    g2d.fillOval(snakeX[i] + TILE/5 + eyeSize/4, snakeY[i] + TILE/5 + eyeSize/4, eyeSize/2, eyeSize/2);
                    g2d.fillOval(snakeX[i] + TILE/5 + eyeSize/4, snakeY[i] + TILE - TILE/5 - eyeSize + eyeSize/4, eyeSize/2, eyeSize/2);
                }
            } else {
                g2d.setColor(new Color(255, 220, 50)); 
                g2d.fillRoundRect(snakeX[i] + 2, snakeY[i] + 2, TILE - 4, TILE - 4, 10, 10);
            }
        }

        g2d.translate(-PAD_X, -PAD_TOP);
        g2d.setClip(oldClip);
    }

    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !levelClear && !gameFinished && !isPaused) {

            if (isBigFruitActive) {
                bigFruitTimer--;
                if (bigFruitTimer <= 0) {
                    isBigFruitActive = false; 
                }
            }

            if (isAIPlaying) {
                dir = calculateAIMove();
            }

            for (int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i - 1];
                snakeY[i] = snakeY[i - 1];
            }

            if (dir == 'r') snakeX[0] += TILE;
            if (dir == 'l') snakeX[0] -= TILE;
            if (dir == 'u') snakeY[0] -= TILE;
            if (dir == 'd') snakeY[0] += TILE;

            if (snakeX[0] < 0 || snakeY[0] < 0 || snakeX[0] >= WIDTH || snakeY[0] >= HEIGHT) {
                triggerGameOver();
                return;
            }

            if (walls.contains(new Point(snakeX[0], snakeY[0]))) {
                triggerGameOver();
                return;
            }
                
            for (int i = 1; i < snakeLength; i++) {
                if (snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                    triggerGameOver();
                    return;
                }
            }

            if (snakeX[0] == fruitX && snakeY[0] == fruitY) {
                if (!App.isMuted) App.foodSoundPlayer.playOnce(); 
                score += 5;
                snakeLength++;
                smallFruitCount++; 
                spawnFruit();

                if (smallFruitCount == 4) {
                    spawnBigFruit();
                    isBigFruitActive = true;
                    bigFruitTimer = 50; 
                    smallFruitCount = 0; 
                }

                if (score >= 50) {
                    if (level == 3) triggerGameFinished();
                    else triggerLevelClear();
                    return;
                }
            }

            if (isBigFruitActive && snakeX[0] == bigFruitX && snakeY[0] == bigFruitY) {
                if (!App.isMuted) App.foodSoundPlayer.playOnce(); 
                score += 10;
                snakeLength++;
                isBigFruitActive = false; 

                if (score >= 50) {
                    if (level == 3) triggerGameFinished();
                    else triggerLevelClear();
                    return;
                }
            }
        }
        repaint(); 
    }
}

// =======================================================
// LỚP MÀN HÌNH MENU CHÍNH (GIAO DIỆN MỚI CỰC ĐẸP)
// =======================================================
class MenuPanel extends JPanel {
    
    private App parentApp;
    private JButton btnMusic; 
    
    // --- THÊM PANEL XÁC NHẬN THOÁT ---
    private JPanel quitConfirmPanel; 

    public MenuPanel(App parentApp) {
        this.parentApp = parentApp;
        setPreferredSize(new Dimension(WindowPanel.TOTAL_WIDTH, WindowPanel.TOTAL_HEIGHT));
        setLayout(null); 

        // Khởi tạo bảng menu xác nhận thoát ẩn sẵn
        createQuitConfirmMenu();

        // Panel chứa các nút bấm nằm ở nửa dưới của khung game
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridBagLayout());
        
        // Vị trí của bảng nút tương đối với khung game
        int btnAreaY = WindowPanel.PAD_TOP + (int)(WindowPanel.HEIGHT * 0.4);
        int btnAreaH = WindowPanel.HEIGHT - (int)(WindowPanel.HEIGHT * 0.4);
        buttonPanel.setBounds(WindowPanel.PAD_X, btnAreaY, WindowPanel.WIDTH, btnAreaH);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15); 
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton btnPlay = createCustomButton("Play");
        btnPlay.addActionListener(e -> parentApp.showGameScreen(false));
        gbc.gridy = 0;
        buttonPanel.add(btnPlay, gbc);

        JButton btnAI = createCustomButton("AI play");
        btnAI.addActionListener(e -> parentApp.showGameScreen(true));
        gbc.gridy = 1;
        buttonPanel.add(btnAI, gbc);

        btnMusic = createCustomButton(App.isMuted ? "Music: OFF" : "Music: ON");
        btnMusic.addActionListener(e -> {
            App.isMuted = !App.isMuted;
            updateMusicButton();
            if (App.isMuted) App.musicPlayer.stop();
            else App.musicPlayer.play();
        });
        gbc.gridy = 2;
        buttonPanel.add(btnMusic, gbc);

        // --- SỬA LẠI NÚT QUIT ---
        JButton btnQuit = createCustomButton("Quit");
        btnQuit.addActionListener(e -> {
            quitConfirmPanel.setVisible(true); 
        });
        gbc.gridy = 3;
        buttonPanel.add(btnQuit, gbc);

        add(buttonPanel);
    }
    
    // --- HÀM TẠO BẢNG XÁC NHẬN THOÁT ---
    private void createQuitConfirmMenu() {
        quitConfirmPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 1. Phủ lớp nền đen mờ toàn màn hình
                g2d.setColor(new Color(0, 0, 0, 180)); 
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // 2. Vẽ hộp thoại (Dialog box) nhỏ gọn ở giữa
                int boxW = (int)(WindowPanel.TILE * 12);
                int boxH = (int)(WindowPanel.TILE * 9);
                int boxX = (getWidth() - boxW) / 2;
                int boxY = (getHeight() - boxH) / 2;
                
                // Nền hộp thoại màu Xanh Navy tone-sur-tone với game
                g2d.setColor(new Color(22, 56, 82)); 
                g2d.fillRoundRect(boxX, boxY, boxW, boxH, 25, 25);
                
                // Viền hộp thoại màu cam
                g2d.setColor(new Color(240, 101, 67)); 
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(boxX, boxY, boxW, boxH, 25, 25);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        quitConfirmPanel.setOpaque(false);
        quitConfirmPanel.setLayout(new GridBagLayout());
        quitConfirmPanel.setBounds(0, 0, WindowPanel.TOTAL_WIDTH, WindowPanel.TOTAL_HEIGHT);
        quitConfirmPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.gridx = 0;
        // KHÔNG CHO KÉO DÃN NÚT NỮA để giữ nguyên kích thước chuẩn
        gbc.fill = GridBagConstraints.NONE; 

        JLabel titleLabel = new JLabel("Thoát trò chơi?");
        // Giảm cỡ chữ xuống để không bị tràn màn hình
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, (int)(WindowPanel.TILE * 1.6))); 
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 20, 10); 
        quitConfirmPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(5, 10, 5, 10);

        JButton btnYes = createCustomButton("Có");
        btnYes.addActionListener(e -> System.exit(0)); 
        gbc.gridy = 1;
        quitConfirmPanel.add(btnYes, gbc);

        JButton btnNo = createCustomButton("Không");
        btnNo.addActionListener(e -> quitConfirmPanel.setVisible(false)); 
        gbc.gridy = 2;
        quitConfirmPanel.add(btnNo, gbc);

        add(quitConfirmPanel);
        setComponentZOrder(quitConfirmPanel, 0); 
    }

    public void updateMusicButton() {
        if(App.isMuted) btnMusic.setText("Music: OFF");
        else btnMusic.setText("Music: ON");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. NỀN CARO
        g2d.setColor(new Color(248, 249, 250)); 
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(new Color(230, 230, 230)); 
        for(int i = 0; i < getWidth(); i += WindowPanel.TILE) g2d.drawLine(i, 0, i, getHeight());
        for(int i = 0; i < getHeight(); i += WindowPanel.TILE) g2d.drawLine(0, i, getWidth(), i);
        
        // Vẽ mây và bụi cỏ
        g2d.setColor(new Color(0, 168, 89)); 
        g2d.fillOval(getWidth() - WindowPanel.TILE*5, getHeight() - WindowPanel.TILE*3, WindowPanel.TILE*8, WindowPanel.TILE*5);
        g2d.fillOval(-WindowPanel.TILE*2, getHeight() - WindowPanel.TILE*4, WindowPanel.TILE*6, WindowPanel.TILE*5);
        g2d.setColor(new Color(255, 193, 7)); 
        g2d.fillOval(getWidth() - WindowPanel.TILE*4, -WindowPanel.TILE*2, WindowPanel.TILE*6, WindowPanel.TILE*4);
        g2d.setColor(new Color(171, 190, 251)); 
        g2d.fillOval(-WindowPanel.TILE*3, WindowPanel.TILE*4, WindowPanel.TILE*4, WindowPanel.TILE*3);

        // 2. KHUNG GAME CHÍNH (Xanh Navy và nền kem sáng)
        g2d.setColor(new Color(22, 56, 82)); 
        g2d.fillRoundRect(WindowPanel.PAD_X - 10, WindowPanel.PAD_TOP - 10, WindowPanel.WIDTH + 20, WindowPanel.HEIGHT + 20, 35, 35);

        g2d.setColor(new Color(253, 246, 227)); 
        g2d.fillRoundRect(WindowPanel.PAD_X, WindowPanel.PAD_TOP, WindowPanel.WIDTH, WindowPanel.HEIGHT, 20, 20);

        // 3. TIÊU ĐỀ GAME (SNAKE ASIA)
        String title = "SNAKE ASIA";
        g2d.setFont(new Font("Segoe UI", Font.BOLD, (int)(WindowPanel.TILE * 4.5)));
        FontMetrics fm = g2d.getFontMetrics();
        int x = WindowPanel.PAD_X + (WindowPanel.WIDTH - fm.stringWidth(title)) / 2;
        int y = WindowPanel.PAD_TOP + (int)(WindowPanel.HEIGHT * 0.3); 

        g2d.setColor(new Color(22, 56, 82)); // Đổ bóng màu Xanh Navy
        g2d.drawString(title, x + 5, y + 5);
        g2d.setColor(new Color(240, 101, 67)); // Chữ màu Cam
        g2d.drawString(title, x, y);

        // Dòng sub-title
        String subtitle = "Code by Son - Masterpiece Edition";
        g2d.setFont(new Font("Segoe UI", Font.ITALIC, (int)(WindowPanel.TILE * 1.2)));
        FontMetrics fm2 = g2d.getFontMetrics();
        int subX = WindowPanel.PAD_X + (WindowPanel.WIDTH - fm2.stringWidth(subtitle)) / 2;
        
        g2d.setColor(Color.BLACK);
        g2d.drawString(subtitle, subX, y + 40);
    }

    private JButton createCustomButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isRollover()) g2.setColor(new Color(39, 174, 96));
                else if (getModel().isPressed()) g2.setColor(new Color(23, 100, 50));
                else g2.setColor(new Color(46, 204, 113)); 
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(39, 174, 96));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, (int)(WindowPanel.TILE * 1.1)));
        btn.setPreferredSize(new Dimension((int)(WindowPanel.TILE * 8.5), (int)(WindowPanel.TILE * 2)));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- Gắn sự kiện click sound ---
        btn.addActionListener(e -> {
            if (!App.isMuted) App.clickSound.playOnce();
        });

        return btn;
    }
}

// =======================================================
// LỚP TRÌNH PHÁT NHẠC ĐÃ ĐƯỢC CẢI TIẾN
// =======================================================
class MusicPlayer {
    private Clip clip;

    public void loadMusic(String filePath) {
        try {
            File musicPath = new File(filePath);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                clip = AudioSystem.getClip();
                clip.open(audioInput);
            } else {
                System.out.println("Không tìm thấy file nhạc: " + filePath);
            }
        } catch (Exception ex) {
            System.out.println("Lỗi đọc file nhạc: " + ex.getMessage());
        }
    }

    public void play() {
        if (clip != null) {
            clip.setFramePosition(0); 
            clip.start();
            clip.loop(Clip.LOOP_CONTINUOUSLY); 
        }
    }

    // --- HÀM PHÁT MỘT LẦN CHUYÊN DÙNG CHO HIỆU ỨNG ÂM THANH (SFX) ---
    public void playOnce() {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop(); // Ngắt tiếng cũ nếu nó vẫn đang phát
            }
            clip.setFramePosition(0); // Tua lại từ đầu
            clip.start(); // Phát lại
        }
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
}