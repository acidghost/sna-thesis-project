package it.uniba.di.itps.SNVSimulation;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by acidghost on 05/11/14.
 */
public class SimulationGUI extends JFrame {
    private JPanel rootPanel;
    private JSlider sldNodes;
    private JButton btnGenerate;
    private JButton btnExportPDF;
    private JButton btnPreview;
    private JTextField txtNodes;
    private JButton btnStart;
    private JTextField txtTicks;
    private JButton txtExportFull;
    private JProgressBar progressBar;
    private JLabel lblFeedback;
    private JCheckBox chkHomophily;
    private JCheckBox chkCharts;
    private JTextField txtTiming;
    private JSlider sldTiming;

    private boolean simulationStarted = false;
    private int generatedNodes = 0;
    private int generatedEdges = 0;

    public SimulationGUI(final Simulation simulation) {
        super("Simulation Manager");

        setContentPane(rootPanel);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setVisible(true);

        btnGenerate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Map<String, Object> graphData = simulation.generateGraph(sldNodes.getValue(), chkHomophily.isSelected());
                generatedNodes = (Integer) graphData.get("nodes");
                generatedEdges = (Integer) graphData.get("edges");
                lblFeedback.setText("Successfully generated graph with " + generatedNodes + " nodes and " + generatedEdges + " edges\n\nNow start the simulation...");
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

        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(!simulationStarted) {
                    simulation.startSimulation(sldTiming.getValue());
                    simulationStarted = true;
                    btnGenerate.setEnabled(false);
                    sldNodes.setEnabled(false);
                    btnStart.setText("Stop simulation");
                    lblFeedback.setText("Nodes: " + generatedNodes + "\nEdges: " + generatedEdges + "\n\nSimulation started...");
                } else {
                    simulation.stopSimulation();
                    simulationStarted = false;
                    btnGenerate.setEnabled(true);
                    sldNodes.setEnabled(true);
                    btnStart.setText("Start simulation");
                    lblFeedback.setText("Nodes: " + generatedNodes + "\nEdges: " + generatedEdges + "\n\nSimulation stopped...");
                }
            }
        });

        txtExportFull.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                simulation.exportGraphFull();
            }
        });

        chkCharts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                simulation.setDisplayCharts(chkCharts.isSelected());
            }
        });

        sldTiming.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                txtTiming.setText("" + sldTiming.getValue());
            }
        });
    }

    public void updateTicks(int ticks, int maxTicks) {
        txtTicks.setText(ticks + "");
        progressBar.setMinimum(0);
        progressBar.setMaximum(maxTicks);
        progressBar.setValue(ticks);
        if(ticks > maxTicks) {
            btnStart.doClick();
        }
    }

    private void showPreview() {
        JFrame frame = new JFrame("Network Preview");
        JPanel panel = new JPanel();
        frame.setContentPane(panel);
        JLabel label = new JLabel();

        byte[] arr = new byte[0];
        try {
            File file = new File(Simulation.EXPORT_PATH + "graph.png");
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
