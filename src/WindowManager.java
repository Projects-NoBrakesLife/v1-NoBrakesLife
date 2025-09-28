import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;


class CustomPanel extends JPanel {
    private boolean restHovered = false;
    private float hoverProgress = 0.0f;
    private Timer hoverTimer;
    
    public void setRestHovered(boolean hovered) {
        if (this.restHovered != hovered) {
            this.restHovered = hovered;
            startHoverAnimation();
        }
    }
    
    public boolean isRestHovered() {
        return this.restHovered;
    }
    
    private void startHoverAnimation() {
        if (hoverTimer != null) {
            hoverTimer.stop();
        }
        
        hoverTimer = new Timer(16, e -> {
            float target = restHovered ? 1.0f : 0.0f;
            float diff = target - hoverProgress;
            
            if (Math.abs(diff) < 0.01f) {
                hoverProgress = target;
                hoverTimer.stop();
            } else {
                hoverProgress += diff * 0.2f;
            }
            
            repaint();
        });
        hoverTimer.start();
    }
    
    public float getHoverProgress() {
        return hoverProgress;
    }
    
    protected Color interpolateColor(Color c1, Color c2, float progress) {
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        int a = (int)(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * progress);
        return new Color(r, g, b, a);
    }
}

public class WindowManager {
    private final Map<String, JFrame> openWindows = new HashMap<>();
    
    public void showWindow(String windowType, String title) {
        closeAllWindows();
        
        JFrame window = createWindow(windowType, title);
        if (window != null) {
            openWindows.put(windowType, window);
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    openWindows.remove(windowType);
                }
            });
        }
    }
    
    private JFrame createWindow(String windowType, String title) {
        switch (windowType) {
            case "apartment":
                return createApartmentWindow(title);
            case "bank":
                return createBankWindow(title);
            case "shop":
                return createShopWindow(title);
            case "hospital":
                return createHospitalWindow(title);
            case "clock":
                return createClockWindow(title);
            default:
                return createDefaultWindow(title);
        }
    }
    
    private JFrame createApartmentWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1500, 800);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setUndecorated(true);
        
        CustomPanel panel = new CustomPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                try {
                    Image apartmentImage = ImageIO.read(new File("assets/maps/home/Crappy Apartment Background.png"));
                    g2d.drawImage(apartmentImage, 0, 0, getWidth(), getHeight(), null);
                } catch (IOException e) {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("Image not found: Crappy Apartment Background.png", 50, 50);
                }
                
                float hoverProgress = getHoverProgress();
                int restWidth = 200;
                int restHeight = 70;
                int restX = (getWidth() - restWidth) / 2;
                int restY = getHeight() - 120;
                
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRoundRect(restX, restY, restWidth, restHeight, 8, 8);
                
                if (isRestHovered()) {
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new java.awt.BasicStroke(2.0f));
                    g2d.drawRoundRect(restX, restY, restWidth, restHeight, 8, 8);
                }
                
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
                FontMetrics fm = g2d.getFontMetrics();
                int restTextX = restX + (restWidth - fm.stringWidth("Rest")) / 2;
                int restTextY = restY + (restHeight + fm.getAscent()) / 2 - 2;
                g2d.drawString("Rest", restTextX, restTextY);
            }
        };
        
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
          
                int restWidth = 200;
                int restHeight = 70;
                int restX = (panel.getWidth() - restWidth) / 2;
                int restY = panel.getHeight() - 120;
                
                if (x >= restX && x <= restX + restWidth && y >= restY && y <= restY + restHeight) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            SoundPlayer soundPlayer = new SoundPlayer();
                            soundPlayer.play("./assets/sfx/bubble-pop.wav");
                        } catch (Exception ex) {
                        }
                        
                        int offsetX = (int)(Math.random() * 60) - 30;
                        int offsetY = (int)(Math.random() * 60) - 30;
                        HitEffect hitEffect = new HitEffect(x + offsetX, y + offsetY);
                        if (hitEffect.isActive()) {
                            panel.add(hitEffect);
                            panel.setComponentZOrder(hitEffect, 0);
                        }
                    });
                }
            }
        });
        
        panel.addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                
             
                int restWidth = 200;
                int restHeight = 70;
                int restX = (panel.getWidth() - restWidth) / 2;
                int restY = panel.getHeight() - 120;
                
                boolean newRestHovered = (x >= restX && x <= restX + restWidth && y >= restY && y <= restY + restHeight);
                if (newRestHovered != panel.isRestHovered()) {
                    panel.setRestHovered(newRestHovered);
                }
            }
        });
        
        frame.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                }
            }
        });
        
        frame.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {}
            
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                frame.dispose();
            }
        });
        
        try {
            Image cursorImage = ImageIO.read(new File("assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        frame.add(panel);
        frame.setVisible(true);
        frame.requestFocus();
        
        return frame;
    }
    
    private JFrame createBankWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setUndecorated(true);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Bank", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton depositButton = new JButton("Deposit");
        JButton withdrawButton = new JButton("Withdraw");
        JButton balanceButton = new JButton("Check Balance");
        
        depositButton.addActionListener(e -> showMessage("Deposit successful"));
        withdrawButton.addActionListener(e -> showMessage("Withdraw successful"));
        balanceButton.addActionListener(e -> showMessage("Balance: $1,000"));
        
        centerPanel.add(depositButton);
        centerPanel.add(withdrawButton);
        centerPanel.add(balanceButton);
        
        panel.add(centerPanel, BorderLayout.CENTER);


        frame.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                }
            }
        });

        frame.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {}

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                frame.dispose();
            }
        });

        try {
            Image cursorImage = ImageIO.read(new File("assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        frame.add(panel);
        frame.setVisible(true);
        
        return frame;
    }
    
    private JFrame createShopWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Shop", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton foodButton = new JButton("Food ($50)");
        JButton drinkButton = new JButton("Drink ($30)");
        JButton medicineButton = new JButton("Medicine ($100)");
        JButton closeButton = new JButton("Close");
        
        foodButton.addActionListener(e -> showMessage("Food purchased"));
        drinkButton.addActionListener(e -> showMessage("Drink purchased"));
        medicineButton.addActionListener(e -> showMessage("Medicine purchased"));
        closeButton.addActionListener(e -> frame.dispose());
        
        centerPanel.add(foodButton);
        centerPanel.add(drinkButton);
        centerPanel.add(medicineButton);
        centerPanel.add(closeButton);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        try {
            Image cursorImage = ImageIO.read(new File("assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        frame.add(panel);
        frame.setVisible(true);
        
        return frame;
    }
    
    private JFrame createHospitalWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Hospital", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton healButton = new JButton("Heal ($200)");
        JButton checkButton = new JButton("Health Check");
        JButton closeButton = new JButton("Close");
        
        healButton.addActionListener(e -> showMessage("Healing complete"));
        checkButton.addActionListener(e -> showMessage("Health is good"));
        closeButton.addActionListener(e -> frame.dispose());
        
        centerPanel.add(healButton);
        centerPanel.add(checkButton);
        centerPanel.add(closeButton);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        try {
            Image cursorImage = ImageIO.read(new File("assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        frame.add(panel);
        frame.setVisible(true);
        
        return frame;
    }
    
    private JFrame createClockWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Clock Tower", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton timeButton = new JButton("Check Time");
        JButton closeButton = new JButton("Close");
        
        timeButton.addActionListener(e -> showMessage("Current time: " + java.time.LocalTime.now().toString()));
        closeButton.addActionListener(e -> frame.dispose());
        
        centerPanel.add(timeButton);
        centerPanel.add(closeButton);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        try {
            Image cursorImage = ImageIO.read(new File("assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        frame.add(panel);
        frame.setVisible(true);
        
        return frame;
    }
    
    private JFrame createDefaultWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Window: " + title, JLabel.CENTER);
        panel.add(label, BorderLayout.CENTER);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> frame.dispose());
        panel.add(closeButton, BorderLayout.SOUTH);
        
        try {
            Image cursorImage = ImageIO.read(new File("assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        frame.add(panel);
        frame.setVisible(true);
        
        return frame;
    }
    
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message, "Notification", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void closeAllWindows() {
        for (JFrame window : openWindows.values()) {
            window.dispose();
        }
        openWindows.clear();
    }
}
