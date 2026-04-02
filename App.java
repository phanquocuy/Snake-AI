import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import javax.sound.sampled.*; 
import java.io.File;

public class App extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel containerPanel = new JPanel(cardLayout);
    
    private MenuPanel menuPanel;
    private WindowPanel gamePanel;
    
    public static MusicPlayer musicPlayer = new MusicPlayer();

    public App() {
        musicPlayer.loadMusic("exitsign.wav");

        menuPanel = new MenuPanel(this);
        gamePanel = new WindowPanel();

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
        gamePanel.startGame(isAI); 
        gamePanel.requestFocusInWindow(); 
    }

    public static void main(String[] args) {
        new App();
    }
}

// =======================================================
// LỚP MÀN HÌNH MENU CHÍNH
// =======================================================
class MenuPanel extends JPanel {
    
    private App parentApp;

    public MenuPanel(App parentApp) {
        this.parentApp = parentApp;
        setPreferredSize(new Dimension(WindowPanel.WIDTH, WindowPanel.HEIGHT));
        setBackground(new Color(30, 30, 30)); 
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); 
        gbc.gridx = 0;
        
        JLabel titleLabel = new JLabel("SNAKE GAME");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 50));
        titleLabel.setForeground(new Color(46, 204, 113));
        gbc.gridy = 0;
        add(titleLabel, gbc);

        JButton btnPlay = createCustomButton("Play");
        btnPlay.addActionListener(e -> {
            parentApp.showGameScreen(false); 
        });
        gbc.gridy = 1;
        add(btnPlay, gbc);

        JButton btnAI = createCustomButton("AI play");
        btnAI.addActionListener(e -> {
            parentApp.showGameScreen(true); 
        });
        gbc.gridy = 2;
        add(btnAI, gbc);

        JButton btnMusic = createCustomButton("Music: OFF");
        btnMusic.addActionListener(e -> {
            if (App.musicPlayer.isPlaying()) {
                App.musicPlayer.stop();
                btnMusic.setText("Music: OFF");
            } else {
                App.musicPlayer.play();
                btnMusic.setText("Music: ON");
            }
        });
        gbc.gridy = 3;
        add(btnMusic, gbc);

        JButton btnQuit = createCustomButton("Quit");
        btnQuit.addActionListener(e -> {
            System.exit(0); 
        });
        gbc.gridy = 4;
        add(btnQuit, gbc);
    }

    private JButton createCustomButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 25));
        btn.setPreferredSize(new Dimension(220, 50));
        btn.setBackground(new Color(173, 255, 47)); 
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false); 
        btn.setBorder(BorderFactory.createRaisedBevelBorder()); 
        return btn;
    }
}

// =======================================================
// LỚP MÀN HÌNH CHƠI GAME & TÍCH HỢP AI MỚI
// =======================================================
class WindowPanel extends JPanel implements ActionListener {

    public static final int TILE = 20;
    public static final int WIDTH_TILES = 25;
    public static final int HEIGHT_TILES = 20;
    public static final int WIDTH = TILE * WIDTH_TILES;
    public static final int HEIGHT = TILE * HEIGHT_TILES;

    private Timer timer = new Timer(120, this);

    private int fruitX, fruitY;
    
    // Khai báo biến cho Mồi To (Quả Táo Vàng)
    private int bigFruitX, bigFruitY;
    private boolean isBigFruitActive = false;
    private int smallFruitCount = 0;
    private int bigFruitTimer = 0; // Bộ đếm ngược thời gian táo vàng tồn tại

    private int[] snakeX = new int[400];
    private int[] snakeY = new int[400];
    private int snakeLength;

    private char dir;

    private boolean gameOver = false;
    private boolean levelClear = false;
    private boolean isAIPlaying = false; 

    private int score;
    private int level = 1;

    private ArrayList<Point> walls = new ArrayList<>();

    private static final int[][] LEVEL_1 = new int[20][25];
    private static final int[][] LEVEL_2 = new int[20][25];
    private static final int[][] LEVEL_3 = new int[20][25];

    public WindowPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 30, 30)); 
        setFocusable(true);

        initMaps();
        setupKeyBindings(); 
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "restart");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");

        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && dir != 'l') dir = 'r'; }
        });
        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && dir != 'r') dir = 'l'; }
        });
        am.put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && dir != 'd') dir = 'u'; }
        });
        am.put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (!isAIPlaying && dir != 'u') dir = 'd'; }
        });

        am.put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver) { level = 1; startGame(isAIPlaying); }
            }
        });

        am.put("next", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (levelClear && level < 3) { level++; startGame(isAIPlaying); }
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

            if (curr.c == targetC && curr.r == targetR) {
                return curr.initialMove; 
            }

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

        if (!gameOver && !levelClear) {
            g.setColor(new Color(255, 128, 0)); 
            for (Point w : walls) g.fillRect(w.x, w.y, TILE, TILE);

            // Vẽ Táo Nhỏ (Đỏ)
            g.setColor(Color.RED);
            g.fillOval(fruitX, fruitY, TILE, TILE);

            // Vẽ Táo To (Vàng rực)
            if (isBigFruitActive) {
                // Nhấp nháy khi thời gian đếm ngược còn dưới 15 nhịp (khoảng 1.8 giây)
                if (bigFruitTimer > 15 || bigFruitTimer % 2 == 0) {
                    g.setColor(Color.YELLOW);
                    // Phóng to quả táo thêm 8 pixel và căn giữa
                    g.fillOval(bigFruitX - 4, bigFruitY - 4, TILE + 8, TILE + 8);
                    g.setColor(Color.ORANGE);
                    // Vẽ viền cho nổi bật
                    g.drawOval(bigFruitX - 4, bigFruitY - 4, TILE + 8, TILE + 8); 
                }
            }

            g.setColor(new Color(65, 105, 225)); 
            for (int i = 0; i < snakeLength; i++) g.fillRect(snakeX[i], snakeY[i], TILE, TILE);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            String modeTxt = isAIPlaying ? "[AI MODE] " : "[PLAYER] ";
            g.drawString(modeTxt + "Level: " + level + "  |  Score: " + score + " / 50", 10, 25);

        } else if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics fm = getFontMetrics(g.getFont());
            g.drawString("GAME OVER", (WIDTH - fm.stringWidth("GAME OVER")) / 2, HEIGHT / 2 - 20);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            FontMetrics fm2 = getFontMetrics(g.getFont());
            g.drawString("Press SPACE to Retry", (WIDTH - fm2.stringWidth("Press SPACE to Retry")) / 2, HEIGHT / 2 + 30);
            
        } else {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics fm = getFontMetrics(g.getFont());
            g.drawString("LEVEL CLEAR!", (WIDTH - fm.stringWidth("LEVEL CLEAR!")) / 2, HEIGHT / 2 - 20);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            FontMetrics fm2 = getFontMetrics(g.getFont());
            g.drawString("Press ENTER for Next Level", (WIDTH - fm2.stringWidth("Press ENTER for Next Level")) / 2, HEIGHT / 2 + 30);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !levelClear) {

            // Xử lý bộ đếm ngược của táo to
            if (isBigFruitActive) {
                bigFruitTimer--;
                if (bigFruitTimer <= 0) {
                    isBigFruitActive = false; // Táo to bốc hơi
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

            if (snakeX[0] < 0 || snakeY[0] < 0 || snakeX[0] >= WIDTH || snakeY[0] >= HEIGHT)
                gameOver = true;

            if (walls.contains(new Point(snakeX[0], snakeY[0])))
                gameOver = true;
                
            for (int i = 1; i < snakeLength; i++) {
                if (snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                    gameOver = true;
                }
            }

            if (snakeX[0] == fruitX && snakeY[0] == fruitY) {
                score += 5;
                snakeLength++;
                smallFruitCount++; 
                spawnFruit();

                if (smallFruitCount == 4) {
                    spawnBigFruit();
                    isBigFruitActive = true;
                    // Đặt thời gian sống cho táo vàng: 50 nhịp (khoảng 6 giây)
                    bigFruitTimer = 50; 
                    smallFruitCount = 0; 
                }

                if (score >= 50) {
                    levelClear = true;
                    timer.stop();
                }
            }

            if (isBigFruitActive && snakeX[0] == bigFruitX && snakeY[0] == bigFruitY) {
                score += 10;
                snakeLength++;
                isBigFruitActive = false; 

                if (score >= 50) {
                    levelClear = true;
                    timer.stop();
                }
            }
        }
        repaint();
    }
}

// =======================================================
// LỚP TRÌNH PHÁT NHẠC
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
                System.out.println("Không tìm thấy file nhạc! Hãy kiểm tra lại tên file exitsign.wav");
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

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
}