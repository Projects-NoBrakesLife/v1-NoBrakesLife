package ui;

import core.Config;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import javax.swing.*;

public class MapEditorUI extends JFrame {
    private static final long serialVersionUID = 1L;
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
                    JOptionPane optionPane = new JOptionPane("Error loading file: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
                    optionPane.setFont(util.FontManager.getFontForText("Error loading file", 12));
                    JDialog dialog = optionPane.createDialog(this, "Error");
                    dialog.setFont(util.FontManager.getFontForText("Error", 12));
                    dialog.setVisible(true);
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

        JButton rotateLeftButton = new JButton("Rotate Left");
        rotateLeftButton.addActionListener(e -> mapEditor.rotateSelectedObject(-15));

        JButton rotateRightButton = new JButton("Rotate Right");
        rotateRightButton.addActionListener(e -> mapEditor.rotateSelectedObject(15));

        JButton zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> mapEditor.zoomIn());

        JButton zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> mapEditor.zoomOut());

        JButton resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> mapEditor.resetZoom());

        controlPanel.add(addPngButton);
        controlPanel.add(roadEditButton);
        controlPanel.add(showRoadsButton);
        controlPanel.add(exportObjectsButton);
        controlPanel.add(exportRoadsButton);
        controlPanel.add(clearOutputButton);
        controlPanel.add(rotateLeftButton);
        controlPanel.add(rotateRightButton);
        controlPanel.add(zoomInButton);
        controlPanel.add(zoomOutButton);
        controlPanel.add(resetZoomButton);

        outputArea = new JTextArea(10, 50);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapEditor, scrollPane);
        splitPane.setDividerLocation(800);

        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Map Editor - Mouse: edit objects | R: road edit | Q/W: rotate | +/-: zoom | 0: reset zoom");
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

        inputMap.put(KeyStroke.getKeyStroke("Q"), "rotateLeft");
        actionMap.put("rotateLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.rotateSelectedObject(-15);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("W"), "rotateRight");
        actionMap.put("rotateRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.rotateSelectedObject(15);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("PLUS"), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.zoomIn();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("MINUS"), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.zoomOut();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("0"), "resetZoom");
        actionMap.put("resetZoom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapEditor.resetZoom();
            }
        });
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        JOptionPane optionPane = new JOptionPane("Code copied to clipboard!", JOptionPane.INFORMATION_MESSAGE);
        optionPane.setFont(util.FontManager.getFontForText("Code copied to clipboard!", 12));
        JDialog dialog = optionPane.createDialog(this, "Success");
        dialog.setFont(util.FontManager.getFontForText("Success", 12));
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            new MapEditorUI().setVisible(true);
        });
    }
}
