package it.uniba.di.itps.SNVSimulation.network;

import com.itextpdf.text.PageSize;
import org.gephi.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.preview.PDFExporter;
import org.gephi.io.exporter.preview.PNGExporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerFactory;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.preview.api.*;
import org.gephi.preview.types.DependantColor;
import org.gephi.preview.types.DependantOriginalColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.ranking.api.Ranking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.Transformer;
import org.gephi.ranking.plugin.transformer.AbstractColorTransformer;
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
import org.gephi.statistics.plugin.Degree;
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.PageRank;
import org.openide.util.Lookup;
import processing.core.PApplet;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by acidghost on 05/11/14.
 */
public class SocialNetwork {

    private ProjectController pc;
    private Workspace workspace;
    private GraphModel graphModel;
    private AttributeColumn betweennessCol;
    private AttributeColumn degreeCol;
    private AttributeColumn pageRankCol;
    private ImportController importController = Lookup.getDefault().lookup(ImportController.class);

    private DijkstraShortestPathAlgorithm computedShortestPaths;

    public SocialNetwork() {
        pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        //  Get a graph model - it exists because we have a workspace
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
    }

    public int[] getNeighbors(int node) {
        UndirectedGraph graph = graphModel.getUndirectedGraph();
        Node[] nodes = graph.getNeighbors(graph.getNode(node)).toArray();
        int[] ids = new int[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            ids[i] = nodes[i].getId();
        }
        return ids;
    }

    public int[] getNodes() {
        UndirectedGraph graph = graphModel.getUndirectedGraph();
        Node[] nodes = graph.getNodes().toArray();
        int[] ids = new int[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            ids[i] = nodes[i].getId();
        }
        return ids;
    }

    public Map<String, Object> generateGraph(int iterations) {
        clearWorkspace();

        //  Generate a new random graph into a container
        Container container = Lookup.getDefault().lookup(ContainerFactory.class).newContainer();
        /*
        RandomGraph randomGraph = new RandomGraph();
        randomGraph.setNumberOfNodes(nodes);
        randomGraph.setWiringProbability(wiringProb);
        randomGraph.generate(container.getLoader());

        AgeingBAModel baModel = new AgeingBAModel();
        baModel.setN(nodes);
        int m0 = nodes/10;
        baModel.setM0(m0 == 0 ? 1 : m0);
        baModel.setAgeingType(3);
        baModel.generate(container.getLoader());
        */

        BarabasiAlbertGeneralized baModel = new BarabasiAlbertGeneralized();
        baModel.setN(iterations);
        baModel.setm0(1);
        baModel.setM(1);
        baModel.setp(0.4);
        baModel.setq(0.0);
        baModel.generate(container.getLoader());

        /*
        BarabasiAlbert baModel = new BarabasiAlbert();
        baModel.setN(nodes);
        baModel.setm0(1);
        baModel.setM(1);
        baModel.generate(container.getLoader());
        */

        //  Create custom data columns
        AttributeController ac = Lookup.getDefault().lookup(AttributeController.class);
        AttributeModel attributeModel = ac.getModel();
        attributeModel.getNodeTable().addColumn("agreableness", AttributeType.DOUBLE);
        attributeModel.getNodeTable().addColumn("extroversion", AttributeType.DOUBLE);
        attributeModel.getNodeTable().addColumn("openness", AttributeType.DOUBLE);

        //  Import the container into the workspace
        importController.process(container, new DefaultProcessor(), workspace);

        //  Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(false);
        distance.setNormalized(true);
        distance.execute(graphModel, attributeModel);

        //  Get Centrality column created
        betweennessCol = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);

        //  Get degree
        Degree degree = new Degree();
        degree.execute(graphModel, attributeModel);
        //  Get degree column created
        degreeCol = attributeModel.getNodeTable().getColumn(Degree.DEGREE);

        //  Compute PageRank
        PageRank pageRank = new PageRank();
        pageRank.setUseEdgeWeight(false);
        pageRank.setDirected(false);
        pageRank.execute(graphModel, attributeModel);
        pageRankCol = attributeModel.getNodeTable().getColumn(PageRank.PAGERANK);

        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("nodes", graphModel.getUndirectedGraph().getNodeCount());
        ret.put("edges", graphModel.getUndirectedGraph().getEdgeCount());
        return ret;
    }

    public int getMaximumDegree() {
        int max = 0;
        for(Node n : graphModel.getUndirectedGraph().getNodes()) {
            Integer degree = (Integer) n.getNodeData().getAttributes().getValue(degreeCol.getIndex());
            if(degree > max) {
                max = degree;
            }
        }
        return max;
    }

    public int getNodeDegree(int node) {
        Node n = graphModel.getUndirectedGraph().getNode(node);
        return (Integer) n.getNodeData().getAttributes().getValue(degreeCol.getIndex());
    }

    public double getBetweennessCentrality(int node) {
        Node n = graphModel.getUndirectedGraph().getNode(node);
        return (Double) n.getNodeData().getAttributes().getValue(betweennessCol.getIndex());
    }

    public double getPageRank(int node) {
        Node n = graphModel.getUndirectedGraph().getNode(node);
        return (Double) n.getNodeData().getAttributes().getValue(pageRankCol.getIndex());
    }

    public int[] getFirstPageRanked(int k) {
        if(k > getNodes().length) {
            throw new IllegalArgumentException();
        }

        List<AbstractMap.SimpleEntry<Integer, Double>> ranks = new ArrayList<AbstractMap.SimpleEntry<Integer, Double>>();
        for(Node node : graphModel.getUndirectedGraph().getNodes()) {
            Double rank = (Double) node.getNodeData().getAttributes().getValue(pageRankCol.getIndex());
            ranks.add(new AbstractMap.SimpleEntry<Integer, Double>(node.getId(), rank));
        }
        Collections.sort(ranks, new Comparator<AbstractMap.SimpleEntry<Integer, Double>>() {
            @Override
            public int compare(AbstractMap.SimpleEntry<Integer, Double> integerDoubleSimpleEntry, AbstractMap.SimpleEntry<Integer, Double> integerDoubleSimpleEntry2) {
                double value1 = integerDoubleSimpleEntry.getValue();
                double value2 = integerDoubleSimpleEntry2.getValue();
                //  -1 and 1 inverted so that the first are the higher ranked
                if(value1 > value2) {
                    return -1;
                } else if(value1 < value2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        int[] nodes = new int[k];
        for(int i=0; i<k; i++) {
            nodes[i] = ranks.get(i).getKey();
        }
        return nodes;
    }

    public Map<Integer, Double> computeShortestPathDistances(int sourceNode) {
        UndirectedGraph graph = graphModel.getUndirectedGraph();
        computedShortestPaths = new DijkstraShortestPathAlgorithm(graph, graph.getNode(sourceNode));
        computedShortestPaths.compute();
        Map<NodeData, Double> distances = computedShortestPaths.getDistances();
        Map<Integer, Double> retMap = new HashMap<Integer, Double>();
        for(NodeData nodeData : distances.keySet()) {
            retMap.put(Integer.parseInt(nodeData.getId()), distances.get(nodeData));
        }
        return retMap;
    }

    public List<Integer> getShortestPath(int targetNode) {
        UndirectedGraph graph = graphModel.getUndirectedGraph();
        computedShortestPaths.getPredecessor(graph.getNode(targetNode));
        //HashMap<NodeData, Double> distances = computedShortestPaths.getDistances();
        //int distance = (int) Math.round(distances.get(graph.getNode(targetNode).getNodeData()));
        List<Integer> path = new ArrayList<Integer>();
        Node node = graph.getNode(targetNode);
        path.add(node.getId());
        while ((node = computedShortestPaths.getPredecessor(node)) != null) {
            path.add(node.getId());
            //node = computedShortestPaths.getPredecessor(node);
        }
        return path;
    }

    public void addNodeAttributes(int node, double agreableness, double extroversion, double openness) {
        Node n = graphModel.getUndirectedGraph().getNode(node);
        Attributes attrs = n.getNodeData().getAttributes();
        attrs.setValue("agreableness", agreableness);
        attrs.setValue("extroversion", extroversion);
        attrs.setValue("openness", openness);
    }

    public void increaseEdgeWeight(int node1, int node2) {
        UndirectedGraph graph = graphModel.getUndirectedGraph();
        Node n1 = graph.getNode(node1);
        Node n2 = graph.getNode(node2);
        Edge edge = graph.getEdge(n1, n2);
        float actual = edge.getWeight();
        edge.setWeight(actual + 1);
    }

    public void startStreaming() {
        // TODO
        //StreamingController controller = Lookup.getDefault().lookup(StreamingController.class);
    }

    private void clearWorkspace() {
        pc.newProject();
        workspace = pc.getCurrentWorkspace();
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
    }

    public void exportFull(String filename) {
        //  Export full graph
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File(filename));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void exportPDF(String filename) {
        configurePreview();

        configureLayout();

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        //PDF Exporter config and export to Byte array
        PDFExporter pdfExporter = (PDFExporter) ec.getExporter("pdf");
        pdfExporter.setPageSize(PageSize.A0);
        pdfExporter.setWorkspace(workspace);
        try {
            FileOutputStream fileOuputStream = new FileOutputStream(filename);
            ec.exportStream(fileOuputStream, pdfExporter);
            fileOuputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportPNG(String filename) {
        configurePreview();

        configureRanking();

        configureLayout();

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        PNGExporter pngExporter = (PNGExporter) ec.getExporter("png");
        pngExporter.setWorkspace(workspace);
        try {
            ec.exportFile(new File(filename), pngExporter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureLayout() {
        AutoLayout autoLayout = new AutoLayout(3, TimeUnit.SECONDS);
        autoLayout.setGraphModel(graphModel);

        ForceAtlasLayout layout = new ForceAtlasLayout(null);
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f); // True after 10% of layout time
        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", 2500., 0f); // 500 for the complete period
        autoLayout.addLayout(layout, 1f, new AutoLayout.DynamicProperty[]{ adjustBySizeProperty, repulsionProperty });

        autoLayout.execute();
    }

    private void configureRanking() {
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();

        //  Rank size by extroversion
        AttributeColumn exCol = attributeModel.getNodeTable().getColumn("extroversion");
        Ranking exRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, exCol.getId());
        AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
        sizeTransformer.setMinSize(14);
        sizeTransformer.setMaxSize(30);
        rankingController.transform(exRanking, sizeTransformer);

        //  Rank color by agreableness
        AttributeColumn agCol = attributeModel.getNodeTable().getColumn("agreableness");
        Ranking agRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, agCol.getId());
        AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
        colorTransformer.setColors(new Color[]{new Color(0xAAFFAA), new Color(0x006600)});
        rankingController.transform(agRanking,colorTransformer);

        //  Rank label size - set a multiplier size
        Ranking centralityRanking2 = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, exCol.getId());
        AbstractSizeTransformer labelSizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.LABEL_SIZE);
        labelSizeTransformer.setMinSize(1);
        labelSizeTransformer.setMaxSize(1);
        rankingController.transform(centralityRanking2,labelSizeTransformer);
    }

    private void configurePreview() {
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_COLOR, new DependantColor(Color.LIGHT_GRAY));
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_SHOW_BOX, Boolean.FALSE);
        //previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_OPACITY, 50);
        //previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_COLOR, new DependantColor(Color.LIGHT_GRAY));
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_RADIUS, 2f);
        previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.WHITE);
        previewController.refreshPreview();
    }

    public void showPreview() {
        //  Preview configuration
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_COLOR, new DependantColor(Color.LIGHT_GRAY));
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_SHOW_BOX, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_COLOR, new DependantColor(Color.LIGHT_GRAY));
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_RADIUS, 2f);
        previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.WHITE);
        previewController.refreshPreview();

        //  New Processing target, get the PApplet
        ProcessingTarget target = (ProcessingTarget) previewController.getRenderTarget(RenderTarget.PROCESSING_TARGET);
        PApplet applet = target.getApplet();
        applet.init();

        //  Refresh the preview and reset the zoom
        previewController.render(target);
        target.refresh();
        target.resetZoom();

        //  Add the applet to a JFrame and display
        JFrame frame = new JFrame("Test Preview");
        frame.setLayout(new BorderLayout());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(applet, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }
}
