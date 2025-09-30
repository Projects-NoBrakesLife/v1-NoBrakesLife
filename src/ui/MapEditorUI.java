package ui;

import core.Config;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.*;

public class MapEditorUI extends JFrame {
    private MapEditor mapEditor;
    private JTextArea outputArea;

    public MapEditorUI() {
        setTitle("Map Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        mapEditor = new MapEditor("./assets/maps/background-iceland.png");
        mapEditor.loadRoadPoints(Config.ROAD_POINTS);

        setupUI();
        setupKeyBindings();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton addPngButton = new JButton("Add PNG");
        addPngButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser("./assets/maps/obj/");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG files", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    mapEditor.addPng(fc.getSelectedFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
                }
            }
        });

        JButton roadEditButton = new JButton("Toggle Road Edit");
        roadEditButton.addActionListener(e -> mapEditor.toggleRoadEditMode());

        JButton showRoadsButton = new JButton("Toggle Show Roads");
        showRoadsButton.addActionListener(e -> mapEditor.toggleShowRoads());

        JButton exportObjectsButton = new JButton("Export Objects");
        exportObjectsButton.addActionListener(e -> {
            String code = mapEditor.exportAsJavaList();
            outputArea.setText(code);
            copyToClipboard(code);
        });

        JButton exportRoadsButton = new JButton("Export Roads");
        exportRoadsButton.addActionListener(e -> {
            String code = mapEditor.exportRoadPoints();
            outputArea.setText(code);
            copyToClipboard(code);
        });

        JButton clearOutputButton = new JButton("Clear Output");
        clearOutputButton.addActionListener(e -> outputArea.setText(""));

        controlPanel.add(addPngButton);
        controlPanel.add(roadEditButton);
        controlPanel.add(showRoadsButton);
        controlPanel.add(exportObjectsButton);
        controlPanel.add(exportRoadsButton);
        controlPanel.add(clearOutputButton);

        outputArea = new JTextArea(10, 50);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapEditor, scrollPane);
        splitPane.setDividerLocation(800);

        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Map Editor - Use mouse to edit objects, R for road edit mode");
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void setupKeyBindings() {
        InputMap inputMap = mapEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mapEditor.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("R"), "toggleRoadEdit");
        actionMap.put("toggleRoadEdit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.toggleRoadEditMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("V"), "toggleShowRoads");
        actionMap.put("toggleShowRoads", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.toggleShowRoads();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("E"), "exportObjects");
        actionMap.put("exportObjects", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String code = mapEditor.exportAsJavaList();
                outputArea.setText(code);
                copyToClipboard(code);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("T"), "exportRoads");
        actionMap.put("exportRoads", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String code = mapEditor.exportRoadPoints();
                outputArea.setText(code);
                copyToClipboard(code);
            }
        });
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this, "Code copied to clipboard!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            new MapEditorUI().setVisible(true);
        });
    }
}