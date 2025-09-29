package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import core.Config;

public class CharacterSelection {

    public static String showCharacterSelection(JFrame parentFrame) {
        List<String> characterFiles = new ArrayList<>();

        File playersDir = new File("assets/players");
        if (playersDir.exists() && playersDir.isDirectory()) {
            for (File file : playersDir.listFiles()) {
                if (file.getName().toLowerCase().endsWith(".png")) {
                    characterFiles.add(file.getPath());
                }
            }
        }

        if (characterFiles.isEmpty()) {
            return "assets/players/Male-01.png";
        }

        JWindow loadingWindow = new JWindow(parentFrame);
        JPanel loadingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "Loading Game...";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = getHeight() / 2;
                g2d.drawString(text, x, y);
            }
        };
        loadingWindow.add(loadingPanel);
        loadingWindow.setSize(parentFrame.getSize());
        loadingWindow.setLocation(parentFrame.getLocation());

        JDialog dialog = new JDialog(parentFrame, "@v1-NoBrakesLife", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setUndecorated(true);
        dialog.setSize(parentFrame.getSize());
        dialog.setLocation(parentFrame.getLocation());

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("@v1 - No Brakes Life", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel subtitleLabel = new JLabel("Select Your Character", JLabel.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitleLabel.setForeground(Color.GRAY);
        topPanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel characterPanel = new JPanel(new GridLayout(1, characterFiles.size(), 50, 50));
        characterPanel.setBorder(BorderFactory.createEmptyBorder(100, 100, 100, 100));

        final String[] selectedCharacter = { null };

        for (String characterFile : characterFiles) {
            try {
                ImageIcon originalIcon = new ImageIcon(characterFile);
                Image scaledImage = originalIcon.getImage().getScaledInstance(120, 180, Image.SCALE_SMOOTH);
                JLabel characterLabel = new JLabel(new ImageIcon(scaledImage), JLabel.CENTER);

                characterLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

                characterLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedCharacter[0] = characterFile;

                        loadingWindow.setVisible(true);

                        Timer fadeTimer = new Timer(20, new ActionListener() {
                            private float opacity = 1.0f;

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                opacity -= 0.05f;
                                loadingWindow.setOpacity(Math.max(0.0f, opacity));

                                if (opacity <= 0.0f) {
                                    ((Timer) e.getSource()).stop();
                                    loadingWindow.dispose();
                                    dialog.dispose();
                                }
                            }
                        });
                        fadeTimer.start();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        characterLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        characterLabel.setBorder(null);
                    }
                });

                characterPanel.add(characterLabel);
            } catch (Exception e) {
                System.err.println("Error loading character image: " + characterFile);
            }
        }

        mainPanel.add(characterPanel, BorderLayout.CENTER);

        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });

        dialog.add(mainPanel);
        dialog.setVisible(true);

        return selectedCharacter[0] != null ? selectedCharacter[0] : characterFiles.get(0);
    }
}