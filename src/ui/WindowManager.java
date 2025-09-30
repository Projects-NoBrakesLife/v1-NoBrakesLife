package ui;

import core.Config;
import core.Lang;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import util.FontManager;
import util.SoundPlayer;

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
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * progress);
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
            case "culture":
                return createCultureWindow(title);
            case "university":
                return createUniversityWindow(title);
            default:
                return createDefaultWindow(title);
        }
    }

    private JFrame createApartmentWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int windowWidth = Config.WINDOW_WIDTH_APARTMENT;
        int windowHeight = Config.WINDOW_HEIGHT_APARTMENT;
        frame.setSize(windowWidth, windowHeight);
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
                        g2d.setFont(FontManager.getFontForText(Lang.IMAGE_NOT_FOUND + Lang.APARTMENT_BACKGROUND, Config.FONT_SIZE_MEDIUM));
                        g2d.drawString(Lang.IMAGE_NOT_FOUND + Lang.APARTMENT_BACKGROUND, 50, 50);
                }

                float hoverProgress = getHoverProgress();
                int restWidth = Config.BUTTON_WIDTH_REST;
                int restHeight = Config.BUTTON_HEIGHT_REST;
                int restX = (getWidth() - restWidth) / 2;
                int restY = getHeight() - Config.BUTTON_MARGIN_REST;

                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRoundRect(restX, restY, restWidth, restHeight, 8, 8);

                if (isRestHovered()) {
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new java.awt.BasicStroke(2.0f));
                    g2d.drawRoundRect(restX, restY, restWidth, restHeight, 8, 8);
                }

                        g2d.setColor(Color.WHITE);
                        g2d.setFont(FontManager.getFontForText(Lang.REST_BUTTON, Config.FONT_SIZE_LARGE, Font.BOLD));
                        FontMetrics fm = g2d.getFontMetrics();
                        int restTextX = restX + (restWidth - fm.stringWidth(Lang.REST_BUTTON)) / 2;
                        int restTextY = restY + (restHeight + fm.getAscent()) / 2 - 2;
                        g2d.drawString(Lang.REST_BUTTON, restTextX, restTextY);
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
                            soundPlayer.play(Lang.BUBBLE_POP_SOUND);
                        } catch (Exception ex) {
                        }

                        int offsetX = (int) (Math.random() * 60) - 30;
                        int offsetY = (int) (Math.random() * 60) - 30;
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

                boolean newRestHovered = (x >= restX && x <= restX + restWidth && y >= restY
                        && y <= restY + restHeight);
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
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                frame.dispose();
            }
        });

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
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

        int windowWidth = Config.WINDOW_WIDTH_DEFAULT;
        int windowHeight = Config.WINDOW_HEIGHT_DEFAULT;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout());

                JLabel titleLabel = new JLabel(Lang.BANK_TITLE, JLabel.CENTER);
                titleLabel.setFont(FontManager.getFontForText(Lang.BANK_TITLE, Config.FONT_SIZE_TITLE, Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton depositButton = new JButton(Lang.DEPOSIT_BUTTON);
        depositButton.setFont(FontManager.getFontForText(Lang.DEPOSIT_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton withdrawButton = new JButton(Lang.WITHDRAW_BUTTON);
        withdrawButton.setFont(FontManager.getFontForText(Lang.WITHDRAW_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton balanceButton = new JButton(Lang.CHECK_BALANCE_BUTTON);
        balanceButton.setFont(FontManager.getFontForText(Lang.CHECK_BALANCE_BUTTON, Config.FONT_SIZE_BUTTON));

        depositButton.addActionListener(e -> showMessage(Lang.DEPOSIT_SUCCESS));
        withdrawButton.addActionListener(e -> showMessage(Lang.WITHDRAW_SUCCESS));
        balanceButton.addActionListener(e -> showMessage(Lang.BALANCE_AMOUNT));

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
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                frame.dispose();
            }
        });

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
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

        int windowWidth = Config.WINDOW_WIDTH_SHOP;
        int windowHeight = Config.WINDOW_HEIGHT_SHOP;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

                JLabel titleLabel = new JLabel(Lang.SHOP_TITLE, JLabel.CENTER);
                titleLabel.setFont(FontManager.getFontForText(Lang.SHOP_TITLE, Config.FONT_SIZE_TITLE, Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton foodButton = new JButton(Lang.FOOD_BUTTON);
        foodButton.setFont(FontManager.getFontForText(Lang.FOOD_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton drinkButton = new JButton(Lang.DRINK_BUTTON);
        drinkButton.setFont(FontManager.getFontForText(Lang.DRINK_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton medicineButton = new JButton(Lang.MEDICINE_BUTTON);
        medicineButton.setFont(FontManager.getFontForText(Lang.MEDICINE_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton closeButton = new JButton(Lang.CLOSE_BUTTON);
        closeButton.setFont(FontManager.getFontForText(Lang.CLOSE_BUTTON, Config.FONT_SIZE_BUTTON));

        foodButton.addActionListener(e -> showMessage(Lang.FOOD_PURCHASED));
        drinkButton.addActionListener(e -> showMessage(Lang.DRINK_PURCHASED));
        medicineButton.addActionListener(e -> showMessage(Lang.MEDICINE_PURCHASED));
        closeButton.addActionListener(e -> frame.dispose());

        centerPanel.add(foodButton);
        centerPanel.add(drinkButton);
        centerPanel.add(medicineButton);
        centerPanel.add(closeButton);

        panel.add(centerPanel, BorderLayout.CENTER);

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
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

        int windowWidth = Config.WINDOW_WIDTH_DEFAULT;
        int windowHeight = Config.WINDOW_HEIGHT_DEFAULT;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

                JLabel titleLabel = new JLabel(Lang.HOSPITAL_TITLE, JLabel.CENTER);
                titleLabel.setFont(FontManager.getFontForText(Lang.HOSPITAL_TITLE, Config.FONT_SIZE_TITLE, Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton healButton = new JButton(Lang.HEAL_BUTTON);
        healButton.setFont(FontManager.getFontForText(Lang.HEAL_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton checkButton = new JButton(Lang.HEALTH_CHECK_BUTTON);
        checkButton.setFont(FontManager.getFontForText(Lang.HEALTH_CHECK_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton closeButton = new JButton(Lang.CLOSE_BUTTON);
        closeButton.setFont(FontManager.getFontForText(Lang.CLOSE_BUTTON, Config.FONT_SIZE_BUTTON));

        healButton.addActionListener(e -> showMessage(Lang.HEALING_COMPLETE));
        checkButton.addActionListener(e -> showMessage(Lang.HEALTH_GOOD));
        closeButton.addActionListener(e -> frame.dispose());

        centerPanel.add(healButton);
        centerPanel.add(checkButton);
        centerPanel.add(closeButton);

        panel.add(centerPanel, BorderLayout.CENTER);

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
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

        int windowWidth = Config.WINDOW_WIDTH_DEFAULT;
        int windowHeight = Config.WINDOW_HEIGHT_DEFAULT;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

                JLabel titleLabel = new JLabel(Lang.CLOCK_TITLE, JLabel.CENTER);
                titleLabel.setFont(FontManager.getFontForText(Lang.CLOCK_TITLE, Config.FONT_SIZE_TITLE, Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton timeButton = new JButton(Lang.CHECK_TIME_BUTTON);
        timeButton.setFont(FontManager.getFontForText(Lang.CHECK_TIME_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton closeButton = new JButton(Lang.CLOSE_BUTTON);
        closeButton.setFont(FontManager.getFontForText(Lang.CLOSE_BUTTON, Config.FONT_SIZE_BUTTON));

        timeButton.addActionListener(e -> showMessage(Lang.CURRENT_TIME + java.time.LocalTime.now().toString()));
        closeButton.addActionListener(e -> frame.dispose());

        centerPanel.add(timeButton);
        centerPanel.add(closeButton);

        panel.add(centerPanel, BorderLayout.CENTER);

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
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

        int windowWidth = Config.WINDOW_WIDTH_SMALL;
        int windowHeight = Config.WINDOW_HEIGHT_SMALL;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
                JLabel label = new JLabel(Lang.WINDOW_PREFIX + title, JLabel.CENTER);
                label.setFont(FontManager.getFontForText(Lang.WINDOW_PREFIX + title, Config.FONT_SIZE_MEDIUM));
        panel.add(label, BorderLayout.CENTER);

        JButton closeButton = new JButton(Lang.CLOSE_BUTTON);
        closeButton.setFont(FontManager.getFontForText(Lang.CLOSE_BUTTON, Config.FONT_SIZE_BUTTON));
        closeButton.addActionListener(e -> frame.dispose());
        panel.add(closeButton, BorderLayout.SOUTH);

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }

        frame.add(panel);
        frame.setVisible(true);

        return frame;
    }

    private JFrame createCultureWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int windowWidth = Config.WINDOW_WIDTH_DEFAULT;
        int windowHeight = Config.WINDOW_HEIGHT_DEFAULT;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(Lang.CULTURE_TITLE, JLabel.CENTER);
        titleLabel.setFont(FontManager.getFontForText(Lang.CULTURE_TITLE, Config.FONT_SIZE_TITLE, Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton prayButton = new JButton(Lang.PRAY_BUTTON);
        prayButton.setFont(FontManager.getFontForText(Lang.PRAY_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton meditateButton = new JButton(Lang.MEDITATE_BUTTON);
        meditateButton.setFont(FontManager.getFontForText(Lang.MEDITATE_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton blessButton = new JButton(Lang.BLESS_BUTTON);
        blessButton.setFont(FontManager.getFontForText(Lang.BLESS_BUTTON, Config.FONT_SIZE_BUTTON));

        prayButton.addActionListener(e -> showMessage(Lang.PRAY_SUCCESS));
        meditateButton.addActionListener(e -> showMessage(Lang.MEDITATE_SUCCESS));
        blessButton.addActionListener(e -> showMessage(Lang.BLESS_SUCCESS));

        centerPanel.add(prayButton);
        centerPanel.add(meditateButton);
        centerPanel.add(blessButton);

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
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                frame.dispose();
            }
        });

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }

        frame.add(panel);
        frame.setVisible(true);

        return frame;
    }

    private JFrame createUniversityWindow(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int windowWidth = Config.WINDOW_WIDTH_DEFAULT;
        int windowHeight = Config.WINDOW_HEIGHT_DEFAULT;
        frame.setSize(windowWidth, windowHeight);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(Lang.UNIVERSITY_TITLE, JLabel.CENTER);
        titleLabel.setFont(FontManager.getFontForText(Lang.UNIVERSITY_TITLE, Config.FONT_SIZE_TITLE, Font.BOLD));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton studyButton = new JButton(Lang.STUDY_BUTTON);
        studyButton.setFont(FontManager.getFontForText(Lang.STUDY_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton researchButton = new JButton(Lang.RESEARCH_BUTTON);
        researchButton.setFont(FontManager.getFontForText(Lang.RESEARCH_BUTTON, Config.FONT_SIZE_BUTTON));
        JButton examButton = new JButton(Lang.EXAM_BUTTON);
        examButton.setFont(FontManager.getFontForText(Lang.EXAM_BUTTON, Config.FONT_SIZE_BUTTON));

        studyButton.addActionListener(e -> showMessage(Lang.STUDY_SUCCESS));
        researchButton.addActionListener(e -> showMessage(Lang.RESEARCH_SUCCESS));
        examButton.addActionListener(e -> showMessage(Lang.EXAM_SUCCESS));

        centerPanel.add(studyButton);
        centerPanel.add(researchButton);
        centerPanel.add(examButton);

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
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                frame.dispose();
            }
        });

        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        frame.add(panel);
        frame.setVisible(true);

        return frame;
    }

    private void showMessage(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        optionPane.setFont(FontManager.getFontForText(message, Config.FONT_SIZE_MEDIUM));
        
        JDialog dialog = optionPane.createDialog(null, Lang.NOTIFICATION_TITLE);
        dialog.setFont(FontManager.getFontForText(Lang.NOTIFICATION_TITLE, Config.FONT_SIZE_MEDIUM));
        dialog.setVisible(true);
    }

    public void closeAllWindows() {
        for (JFrame window : openWindows.values()) {
            window.dispose();
        }
        openWindows.clear();
    }
}
