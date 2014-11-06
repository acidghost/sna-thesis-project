package it.uniba.di.itps.SNVSimulation;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by acidghost on 05/11/14.
 */
public class SimulationGUI extends JFrame {
    private JPanel rootPanel;
    private JSlider sldNodes;
    private JSlider sldWiringProb;
    private JButton btnGenerate;
    private JButton btnExportPDF;
    private JButton btnPreview;
    private JTextField txtNodes;
    private JTextField txtWiringProb;
    private JButton btnStart;
    private JTextField txtTicks;
    private JButton txtExportFull;

    private boolean simulationStarted = false;

    public SimulationGUI(final Simulation simulation) {
        super("Simulation Manager");

        setContentPane(rootPanel);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setVisible(true);

        btnGenerate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                simulation.generateGraph(sldNodes.getValue(), ((double) sldWiringProb.getValue()) / 100);
            }
        });
        btnExportPDF.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                simulation.exportGraphPDF();
            }
        });
        btnPreview.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                simulation.exportGraphPNG();
                showPreview();
            }
        });
        sldNodes.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                txtNodes.setText(sldNodes.getValue() + "");
            }
        });
        sldWiringProb.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                txtWiringProb.setText(sldWiringProb.getValue() + "%");
            }
        });
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(!simulationStarted) {
                    simulation.startSimulation();
                    simulationStarted = true;
                    btnGenerate.setEnabled(false);
                    sldNodes.setEnabled(false);
                    sldWiringProb.setEnabled(false);
                    btnStart.setText("Stop simulation");
                } else {
                    simulation.stopSimulation();
                    simulationStarted = false;
                    btnGenerate.setEnabled(true);
                    sldNodes.setEnabled(true);
                    sldWiringProb.setEnabled(true);
                    btnStart.setText("Start simulation");
                }
            }
        });
        txtExportFull.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                simulation.exportGraphFull();
            }
        });
    }

    public void updateTicks(int ticks) {
        txtTicks.setText(ticks + "");
    }

    private void showPreview() {
        JFrame frame = new JFrame("Network Preview");
        JPanel panel = new JPanel();
        frame.setContentPane(panel);
        JLabel label = new JLabel();

        byte[] arr = new byte[0];
        try {
            File file = new File("./graph.png");
            FileInputStream fileStream = new FileInputStream(file);
            arr= new byte[(int)file.length()];
            fileStream.read(arr,0,arr.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        label.setIcon(new ImageIcon(arr));
        panel.add(label);
        frame.pack();
        frame.setVisible(true);
    }
}
