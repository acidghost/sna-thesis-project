package it.uniba.di.itps.SNVSimulation;

import it.uniba.di.itps.SNVSimulation.models.*;
import it.uniba.di.itps.SNVSimulation.network.SocialNetwork;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Created by acidghost on 05/11/14.
 */
public class Simulation extends Agent {

    private static final String AGENTS_PREFIX = "Person-00";
    public static final String AGENT_NAME = "SimAgent";
    public static final String EXPORT_PATH = "./export/";
    private static final int MAX_TICKS = 400;
    private static final double SCALE_NODE_RANKED = 1.0;

    private Logger logger = Logger.getJADELogger(getClass().getName());

    private SimulationGUI gui;
    private SocialNetwork socialNetwork;
    private boolean displayCharts = true;
    private int nNodes = 0;
    private int nEdges = 0;
    private double averageDegree = 0;
    private Map<AID, Integer> nodesMap;
    private Map<AID, AgentController> agentsMap;
    private Map<AID, Trait> traitsMap;
    private int hubsFound;
    private boolean simulationStarted = false;

    private List<MessageContent> messages;

    private Map<Integer, Integer> pageRankMap;
    private Map<Integer, Integer> customRankMap;

    @Override
    protected void setup() {
        logger.info("Simulation setup...");

        gui = new SimulationGUI(this);
        socialNetwork = new SocialNetwork();

        addBehaviour(new TimedUpdates(this, 1000));

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("social-network");
        sd.setName("Social-Network-Agent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    public void setDisplayCharts(boolean displayCharts) {
        this.displayCharts = displayCharts;
    }

    public Map<String, Object> generateGraph(int iterations, boolean homophily) {
        Map<String, Object> graphData = socialNetwork.generateGraph(iterations);
        nNodes = (Integer) graphData.get("nodes");
        nEdges = (Integer) graphData.get("edges");
        averageDegree = (2 * nEdges) / nNodes;

        if(displayCharts) {
            //  Calculate a plot degree distribution
            Map<Integer, Integer> degreeDistribution = new HashMap<Integer, Integer>();
            for(int node : socialNetwork.getNodes()) {
                int degree = socialNetwork.getNodeDegree(node);
                Integer count = degreeDistribution.get(degree);
                if(count == null) {
                    count = 0;
                }
                degreeDistribution.put(degree, count+1);
            }
            List<Point2D> degreeDistributionData = new ArrayList<Point2D>();
            for(Integer degree : degreeDistribution.keySet()) {
                degreeDistributionData.add(new Point(degree, degreeDistribution.get(degree)));
            }
            new ScatterPlot("Degree distribution", degreeDistributionData, "Degree", "Count", "Agents", true);
        }

        int degreeThreshold = (int) (socialNetwork.getMaximumDegree()*0.3 + averageDegree);
        hubsFound = 0;
        traitsMap = new HashMap<AID, Trait>();
        for(int nodeID : socialNetwork.getNodes()) {
            Trait trait = new Trait();
            if(homophily) {
                int nodeDegree = socialNetwork.getNodeDegree(nodeID);
                if (nodeDegree > degreeThreshold) {
                    //if(socialNetwork.getBetweennessCentrality(nodeID) > 0.1) {
                    trait.extroversion = (Math.random() * 0.3) + 0.7;   //[0.7, 1.0]
                    trait.agreableness = (Math.random() * 0.4) + 0.4;   //[0.4, 0.8]
                    hubsFound++;
                }
            }
            AID aid = new AID(AGENTS_PREFIX + (nodeID-1), AID.ISLOCALNAME);
            traitsMap.put(aid, trait);
        }

        findInfluencers();

        return graphData;
    }

    public void findInfluencers() {
        int nNodesToRank;
        if(SCALE_NODE_RANKED > 1.0) {
            nNodesToRank = (int) SCALE_NODE_RANKED;
        } else {
            nNodesToRank = (int) (SCALE_NODE_RANKED * nNodes);
        }
        int[] pageRankedNodes = socialNetwork.getFirstPageRanked(nNodesToRank);
        System.out.println("\n\nPageRank ranking:");
        for (int i = 0; i < pageRankedNodes.length; i++) {
            int node = pageRankedNodes[i];
            System.out.println((i + 1) + " - " + AGENTS_PREFIX + (node - 1) + " " + socialNetwork.getPageRank(node));
        }

        int ticks = Person.ticks / nNodes;
        if (ticks == 0) {
            ticks = MAX_TICKS;
        }
        List<AbstractMap.SimpleEntry<Integer, Double>> customRanks = new ArrayList<AbstractMap.SimpleEntry<Integer, Double>>();
        for (int node : pageRankedNodes) {
            AID aid = new AID(AGENTS_PREFIX + (node - 1), AID.ISLOCALNAME);
            Trait trait = traitsMap.get(aid);
            double rank = 0.0;
            double denominator = (1-trait.extroversion) * ticks;
            Map<Integer, Double> distances = socialNetwork.computeShortestPathDistances(node);
            for (Interests interest : trait.interests) {
                double nodeTopicRank = 0.0;
                for (int vNode : socialNetwork.getNodes()) {
                    if (vNode != node) {
                        double distance = distances.get(vNode);
                        double numerator = 0;
                        List<Integer> shortestPath = socialNetwork.getShortestPath(vNode);
                        for (Integer nodeID : shortestPath) {
                            AID vAid = new AID(AGENTS_PREFIX + (nodeID - 1), AID.ISLOCALNAME);
                            Trait vTrait = traitsMap.get(vAid);
                            double personalityImpact = (0.7 * vTrait.agreableness) + (0.3 * vTrait.extroversion);
                            List<Interests> interests = Arrays.asList(vTrait.interests);
                            double topicInfluence = interests.contains(interest) ? 1 - personalityImpact : 0;
                            numerator += personalityImpact + topicInfluence;
                        }
                        numerator = numerator / distance;
                        nodeTopicRank += numerator / denominator;
                    }
                }
                rank += nodeTopicRank;
            }
            rank = (rank / trait.interests.length) * socialNetwork.getPageRank(node);
            customRanks.add(new AbstractMap.SimpleEntry<Integer, Double>(node, rank));
        }

        Collections.sort(customRanks, new Comparator<AbstractMap.SimpleEntry<Integer, Double>>() {
            @Override
            public int compare(AbstractMap.SimpleEntry<Integer, Double> e1, AbstractMap.SimpleEntry<Integer, Double> e2) {
                Double v1 = e1.getValue();
                Double v2 = e2.getValue();
                if (v1 > v2)
                    return -1;
                else if (v1 < v2)
                    return 1;
                else
                    return 0;
            }
        });
        System.out.println("\n\nCustom ranking system:");
        for (int i = 0; i < customRanks.size(); i++) {
            AbstractMap.SimpleEntry<Integer, Double> entry = customRanks.get(i);
            System.out.println((i + 1) + " - " + AGENTS_PREFIX + (entry.getKey() - 1) + " " + entry.getValue());
        }


        //  Build two maps containing nodes and rank-position
        pageRankMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < pageRankedNodes.length; i++) {
            pageRankMap.put(pageRankedNodes[i], i + 1);
        }
        customRankMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < customRanks.size(); i++) {
            customRankMap.put(customRanks.get(i).getKey(), i + 1);
        }

        double correlation1 = kendallCorrelation(pageRankedNodes, pageRankMap, customRankMap);
        System.out.println("\nKendall's correlation between PageRank and CustomRank: " + correlation1);
        double correlation2 = spearmanCorrelation(pageRankedNodes, pageRankMap, customRankMap);
        System.out.println("Spearman's correlation between PageRank and CustomRank: " + correlation2);
    }

    private double kendallCorrelation(int[] nodes, Map<Integer, Integer> rankings1, Map<Integer, Integer> rankings2) {
        //  Compute number of concordant and discordant pairs
        int nConcordant = 0;
        int nDiscordant = 0;
        for (int i = 0; i < nodes.length; i++) {
            int nodeA = nodes[i];
            Integer rank1A = rankings1.get(nodeA);
            Integer rank2A = rankings2.get(nodeA);
            for (int j = i + 1; j < nodes.length; j++) {
                int nodeB = nodes[j];
                Integer rank1B = rankings1.get(nodeB);
                Integer rank2B = rankings2.get(nodeB);
                if(rank1A==null || rank2A== null || rank1B==null || rank2B==null) {
                    nDiscordant++;
                    continue;
                }
                if ((rank1A < rank1B && rank2A > rank2B) || (rank1A > rank1B && rank2A < rank2B)) {
                    nDiscordant++;
                } else {
                    nConcordant++;
                }
            }
        }

        return (nConcordant - nDiscordant) / (0.5 * nodes.length * (nodes.length-1));
    }

    private double spearmanCorrelation(int[] nodes, Map<Integer, Integer> rankings1, Map<Integer, Integer> rankings2) {
        double correlation = 0.0;
        for(int node : nodes) {
            Integer rank1 = rankings1.get(node);
            Integer rank2 = rankings2.get(node);
            int diff;
            if(rank1==null || rank2==null) {
                diff = 1;
            } else {
                diff = rankings1.get(node) - rankings2.get(node);
            }
            correlation += Math.pow(diff, 2);
        }
        return 1 - ((6*correlation) / (nodes.length * (Math.pow(nodes.length, 2)-1)));
    }

    public void startSimulation(int timingOfAgents) {
        logger.info("Start simulation " + socialNetwork.getNodes().length);
        nodesMap = new HashMap<AID, Integer>();
        agentsMap = new HashMap<AID, AgentController>();

        messages = new ArrayList<MessageContent>();
        for(int nodeID : socialNetwork.getNodes()) {
            try {
                //  Because the labels start from 0
                //  and the ids from 1
                String name = AGENTS_PREFIX + (nodeID-1);
                AID aid = new AID(name, AID.ISLOCALNAME);
                Trait trait = traitsMap.get(aid);
                Object[] traitAsArray = trait.toObjectArray();
                Object[] args = new Object[traitAsArray.length+1];
                System.arraycopy(traitAsArray, 0, args, 0, traitAsArray.length);
                args[traitAsArray.length] = timingOfAgents;
                AgentController agent = getContainerController().createNewAgent(name, Person.class.getName(), args);
                agent.start();
                agentsMap.put(aid, agent);
                nodesMap.put(aid, nodeID);
                socialNetwork.addNodeAttributes(nodeID, trait.agreableness, trait.extroversion, trait.openness);
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        socialNetwork.startStreaming();

        addBehaviour(new ServeNeighbors(this));
        addBehaviour(new ReceiveMessages());
        simulationStarted = true;
    }

    public void stopSimulation() {
        for(Object aid : agentsMap.keySet().toArray()) {
            try {
                agentsMap.get(aid).kill();
                agentsMap.remove(aid);
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String txt = "\n\n\n" +
                "Taking down the simulation\n" +
                "total ticks: " + Person.ticks/nNodes + "\n" +
                "hubs found: " + hubsFound;
        System.out.println(txt);

        MessageContent maxHops = messages.get(0);
        Map<MessageContent, Set<AID>> messageSpread = new HashMap<MessageContent, Set<AID>>();
        for(MessageContent msg : messages) {
            Set<AID> set = messageSpread.get(msg);
            if(set == null) {
                set = new HashSet<AID>();
            }
            for(AID agent : msg.nodes) {
                set.add(agent);
            }
            messageSpread.put(msg, set);

            if(msg.nodes.size() > maxHops.nodes.size()) {
                maxHops = msg;
            }
        }

        System.out.println("Max hopped message: " + maxHops.ID + " " + maxHops.interests() + " - " + maxHops.nodes.size());
        System.out.println("Hops:");
        for(AID hop : maxHops.nodes) {
            System.out.println(" - " + hop.getLocalName());
        }

        List<MessageCascade> cascades = new LinkedList<MessageCascade>();
        MessageContent maxSpread = (MessageContent) messageSpread.keySet().toArray()[0];
        for(MessageContent msg : messageSpread.keySet()) {
            Set<AID> set = messageSpread.get(msg);
            //System.out.println("Message " + msg.ID + " " + msg.interests() + " - " + set.size());
            if(messageSpread.get(msg).size() > messageSpread.get(maxSpread).size()) {
                maxSpread = msg;
            }
            cascades.add(new MessageCascade(msg, set, socialNetwork.getNodeDegree(nodesMap.get(msg.nodes.get(0)))));
        }

        System.out.println("Max spread message: " + maxSpread.ID + " " + maxSpread.interests() + " startedFrom: " + maxSpread.nodes.get(0).getLocalName() + " - " + messageSpread.get(maxSpread).size());
        System.out.println("Nodes:");
        for(AID node : messageSpread.get(maxSpread)) {
            System.out.println(" - " + node.getLocalName());
        }

        for(MessageCascade msg : cascades) {
            double sumExtroversion = 0;
            double sumAgreableness = 0;
            for(AID agent : msg.cascade) {
                sumExtroversion += traitsMap.get(agent).extroversion;
                sumAgreableness += traitsMap.get(agent).agreableness;
            }
            int cascadeSize = msg.cascade.size();
            msg.averageCascadeExtroversion = sumExtroversion / cascadeSize;
            msg.averageCascadeAgreableness = sumAgreableness / cascadeSize;
        }

        if (displayCharts) {
            //  Index messages by cascade size
            Collections.sort(cascades);
            List<Point2D> degreeCascadeSize = new ArrayList<Point2D>();
            for(MessageCascade msg : cascades) {
                degreeCascadeSize.add(new Point(msg.cascade.size(), msg.startingDegree));
            }
            new ScatterPlot("Cascade size by starting node degree", degreeCascadeSize, "Cascade size", "Starting node degree", "Cascades");

            Map<Integer, Integer> cascadeSizeDistribution = new HashMap<Integer, Integer>();
            for(MessageCascade msg : cascades) {
                Integer count = cascadeSizeDistribution.get(msg.cascade.size());
                if(count == null) {
                    count = 0;
                }
                cascadeSizeDistribution.put(msg.cascade.size(), count+1);
            }
            List<Point2D> cascadeSizeDistributionData = new ArrayList<Point2D>();
            for(Integer cascadeCluster : cascadeSizeDistribution.keySet()) {
                cascadeSizeDistributionData.add(new Point(cascadeCluster, cascadeSizeDistribution.get(cascadeCluster)));
            }
            new ScatterPlot("Cascade size distribution", cascadeSizeDistributionData, "Cascade size", "Count", "Cascades", true);

            List<Point2D> cascadeSizeByExtroversion = new ArrayList<Point2D>();
            for(MessageCascade msg : cascades) {
                double normalizedExtroversion = traitsMap.get(msg.startingNode).extroversion * 100;
                cascadeSizeByExtroversion.add(new Point(msg.cascade.size(), (int) normalizedExtroversion));
            }
            new ScatterPlot("Cascade size by starting node ext.", cascadeSizeByExtroversion, "Cascade size", "Extroversion", "Cascades");

            List<Point2D> cascadeSizeByAvgExt = new ArrayList<Point2D>();
            for(MessageCascade msg : cascades) {
                double normalizedExtroversion = msg.averageCascadeExtroversion * 100;
                cascadeSizeByAvgExt.add(new Point(msg.cascade.size(), (int) normalizedExtroversion));
            }
            new ScatterPlot("Cascade size by average ext.", cascadeSizeByAvgExt, "Cascade size", "Avg. Extroversion", "Cascades");

            List<Point2D> cascadeSizeByAvgAgr = new ArrayList<Point2D>();
            for(MessageCascade msg : cascades) {
                double normalizedExtroversion = msg.averageCascadeAgreableness * 100;
                cascadeSizeByAvgAgr.add(new Point(msg.cascade.size(), (int) normalizedExtroversion));
            }
            new ScatterPlot("Cascade size by average agr.", cascadeSizeByAvgAgr, "Cascade size", "Avg. Agreableness", "Cascades");
        }

        Map<AID, Integer> aidsByInfluence = new HashMap<AID, Integer>();
        for(MessageCascade msg : cascades) {
            AID seed = msg.startingNode;
            Integer influence = aidsByInfluence.get(seed);
            if(influence == null) {
                influence = msg.cascade.size();
            } else {
                influence += msg.cascade.size();
            }
            aidsByInfluence.put(seed, influence);
        }
        List<Map.Entry<AID, Integer>> orderedAidsByInfluence = new ArrayList<Map.Entry<AID, Integer>>(aidsByInfluence.entrySet());
        Collections.sort(orderedAidsByInfluence, new Comparator<Map.Entry<AID, Integer>>() {
            @Override
            public int compare(Map.Entry<AID, Integer> e1, Map.Entry<AID, Integer> e2) {
                Integer i1 = e1.getValue();
                Integer i2 = e2.getValue();
                if(i1 > i2)
                    return -1;
                else if(i1 < i2)
                    return 1;
                else
                    return 0;
            }
        });
        aidsByInfluence = new LinkedHashMap<AID, Integer>();
        for(Map.Entry<AID, Integer> entry : orderedAidsByInfluence) {
            aidsByInfluence.put(entry.getKey(), entry.getValue());
        }

        //  Print top actual influencers and build a map containing actual nodes ranked by cumulative cascades' size
        Map<Integer, Integer> actualInfluencers = new HashMap<Integer, Integer>();
        System.out.println("\n\nActual top cascades by size;");
        int pos=1;
        for(Map.Entry<AID, Integer> entry : aidsByInfluence.entrySet()) {
            actualInfluencers.put(nodesMap.get(entry.getKey()), pos);
            System.out.println("- " + pos + " " + entry.getKey().getLocalName() + " " + entry.getValue());
            pos++;
        }
        //  Complete the actual influencers map with the remaining "unseen" nodes
        Set<Integer> seenNodes = actualInfluencers.keySet();
        List<Integer> unseenNodes = new ArrayList<Integer>();
        for(int node : socialNetwork.getNodes()) {
            if(!seenNodes.contains(node)) {
                unseenNodes.add(node);
            }
        }
        for(int i=0; i<unseenNodes.size(); i++) {
            actualInfluencers.put(unseenNodes.get(i), i+pos);
        }

        //  Build the considered node-set
        int[] consideredNodes = new int[customRankMap.size()];
        int pointer = 0;
        for (Integer node : customRankMap.keySet()) {
            consideredNodes[pointer] = node;
            pointer++;
        }

        double kCorrelation1 = kendallCorrelation(consideredNodes, pageRankMap, actualInfluencers);
        System.out.println("\nKendall's correlation between PageRank and actual: " + kCorrelation1);
        double kCorrelation2 = kendallCorrelation(consideredNodes, customRankMap, actualInfluencers);
        System.out.println("Kendall's correlation between CustomRank and actual: " + kCorrelation2);

        double sCorrelation1 = spearmanCorrelation(consideredNodes, pageRankMap, actualInfluencers);
        System.out.println("\nSpearman's correlation between PageRank and actual: " + sCorrelation1);
        double sCorrelation2 = spearmanCorrelation(consideredNodes, customRankMap, actualInfluencers);
        System.out.println("Spearman's correlation between CustomRank and actual: " + sCorrelation2);

        System.out.println("\nComposed correlations are\tPA: " + (kCorrelation1*((sCorrelation1+1)/2)) + "\tCA: " + (kCorrelation2*((sCorrelation2+1)/2)));

        Person.ticks = 0;
        simulationStarted = false;
    }

    public void exportGraphPDF() {
        socialNetwork.exportPDF(EXPORT_PATH + "graph.pdf");
    }

    public void exportGraphPNG() {
        socialNetwork.exportPNG(EXPORT_PATH + "graph.png");
    }

    public void exportGraphFull() {
        socialNetwork.exportFull(EXPORT_PATH + "graph.gexf");
    }

    public void showPreview() {
        socialNetwork.showPreview();
    }

    protected class ServeNeighbors extends Behaviour {
        public ServeNeighbors(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchOntology("get-neighbors");
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null) {
                AID sender = msg.getSender();
                int[] neighbors = socialNetwork.getNeighbors(nodesMap.get(sender));
                try {
                    ACLMessage reply = msg.createReply();
                    reply.setConversationId(msg.getConversationId());
                    String content = "";
                    for(int i=0; i < neighbors.length; i++) {
                        content += AGENTS_PREFIX + (neighbors[i]-1);
                        if(i < neighbors.length-1)
                            content += "#";
                    }
                    reply.setContent(content);
                    myAgent.send(reply);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return !simulationStarted;
        }
    }

    protected class ReceiveMessages extends Behaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchOntology("ending-thread");
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null) {
                try {
                    MessageContent messageContent = (MessageContent) msg.getContentObject();
                    messages.add(messageContent);
                    if(messageContent.forwards() > 1) {
                        List<AID> nodes = messageContent.nodes;
                        for(int i=0; i<nodes.size()-1; i++) {
                            socialNetwork.increaseEdgeWeight(nodesMap.get(nodes.get(i)), nodesMap.get(nodes.get(i+1)));
                        }
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return !simulationStarted;
        }
    }

    protected class TimedUpdates extends TickerBehaviour {
        public TimedUpdates(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if(simulationStarted) {
                gui.updateTicks(Person.ticks/nNodes, MAX_TICKS);
            }
        }
    }
}
