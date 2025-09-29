package ui;

import java.awt.*;
import javax.swing.*;
import core.Config;

public class MapEditorUI extends JFrame {
    private final MapCanvas canvas;
    private final DefaultListModel<MapCanvas.Obj> layerModel;
    private final JList<MapCanvas.Obj> layerList;

    public MapEditorUI() {
        super("Map Editor Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        canvas = new MapCanvas("./assets/maps/background-iceland.png");

        layerModel = new DefaultListModel<>();
        layerList = new JList<>(layerModel);
        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                canvas.setSelected(layerList.getSelectedValue());
            }
        });
        JScrollPane layerScroll = new JScrollPane(layerList);
        layerScroll.setPreferredSize(new Dimension(220, 0));

        JButton addBtn = new JButton("Add PNG");
        addBtn.addActionListener(e -> {
            MapCanvas.Obj o = canvas.openFileAndAdd();
            if (o != null) {
                layerModel.addElement(o);
                layerList.setSelectedValue(o, true);
            }
        });

        JButton happinessBtn = new JButton("Add Happiness");
        happinessBtn.addActionListener(e -> {
            canvas.addHappinessIcon();
            layerModel.clear();
            for (MapCanvas.Obj obj : canvas.getObjects()) {
                layerModel.addElement(obj);
            }
        });

        JButton clockBtn = new JButton("Add Clock Tower");
        clockBtn.addActionListener(e -> {
            canvas.addClockTower();
            layerModel.clear();
            for (MapCanvas.Obj obj : canvas.getObjects()) {
                layerModel.addElement(obj);
            }
        });

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> {
            MapCanvas.Obj sel = layerList.getSelectedValue();
            if (sel != null) {
                canvas.removeObj(sel);
                layerModel.removeElement(sel);
            }
        });

        JButton duplicateBtn = new JButton("Duplicate");
        duplicateBtn.addActionListener(e -> {
            canvas.duplicateSelected();
            MapCanvas.Obj sel = canvas.openFileAndAdd();
            if (sel != null) {
                layerModel.addElement(sel);
                layerList.setSelectedValue(sel, true);
            }
        });

        JButton rotateL = new JButton("⟲ Left");
        rotateL.addActionListener(e -> canvas.rotateSelected(-5));

        JButton rotateR = new JButton("⟳ Right");
        rotateR.addActionListener(e -> canvas.rotateSelected(5));

        JButton zoomIn = new JButton("＋");
        zoomIn.addActionListener(e -> canvas.scaleSelected(1.1));

        JButton zoomOut = new JButton("－");
        zoomOut.addActionListener(e -> canvas.scaleSelected(0.9));

        JButton exportBtn = new JButton("Export Java");
        exportBtn.addActionListener(e -> {
            String code = canvas.exportAsJavaList();
            JTextArea area = new JTextArea(code, 12, 70);
            area.setFont(new Font("Monospaced", Font.PLAIN, 14));
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Exported Java Code", JOptionPane.PLAIN_MESSAGE);
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(addBtn);
        toolbar.add(happinessBtn);
        toolbar.add(clockBtn);
        toolbar.add(deleteBtn);
        toolbar.add(duplicateBtn);
        toolbar.add(rotateL);
        toolbar.add(rotateR);
        toolbar.add(zoomIn);
        toolbar.add(zoomOut);
        toolbar.add(exportBtn);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        add(layerScroll, BorderLayout.EAST);

        int windowWidth = Math.min(1400, Config.GAME_WIDTH + 200);
        int windowHeight = Math.min(900, Config.GAME_HEIGHT + 150);
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapEditorUI::new);
    }
}
