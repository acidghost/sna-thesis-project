package it.uniba.di.itps.SNVSimulation;

import it.uniba.di.itps.SNVSimulation.models.Interests;
import it.uniba.di.itps.SNVSimulation.models.MessageContent;
import it.uniba.di.itps.SNVSimulation.models.Person;
import it.uniba.di.itps.SNVSimulation.models.Trait;
import it.uniba.di.itps.SNVSimulation.network.SocialNetwork;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
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

import java.util.*;

/**
 * Created by acidghost on 05/11/14.
 */
public class Simulation extends Agent {

    private static final String AGENTS_PREFIX = "Person-00";
    public static final String AGENT_NAME = "SimAgent";
    private static final String EXPORT_PATH = "./export/";

    private Logger logger = Logger.getJADELogger(getClass().getName());

    private SimulationGUI gui;
    private SocialNetwork socialNetwork;
    private int nNodes = 0;
    private Map<AID, Integer> nodesMap;
    private Map<AID, AgentController> agentsMap;
    private int hubsFound = 0;
    private boolean simulationStarted = false;

    private List<MessageContent> messages;

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

    public void generateGraph(int nodes, double wiringProb) {
        nNodes = nodes;
        socialNetwork.generateGraph(nodes, wiringProb);
    }

    public void startSimulation() {
        logger.info("Start simulation " + socialNetwork.getNodes().length);
        nodesMap = new HashMap<AID, Integer>();
        agentsMap = new HashMap<AID, AgentController>();
        messages = new ArrayList<MessageContent>();
        for(int i : socialNetwork.getNodes()) {
            try {
                Trait trait = new Trait();
                if(socialNetwork.getBetweennessCentrality(i) > 0.1) {
                    trait.extroversion = 1.0;
                    trait.agreableness = 0.8;
                    hubsFound++;
                }
                Object[] args = trait.toObjectArray();

                //  Because the labels start from 0
                //  and the ids from 1
                String name = AGENTS_PREFIX + (i-1);
                AID aid = new AID(name, AID.ISLOCALNAME);
                AgentController agent = getContainerController().createNewAgent(name, Person.class.getName(), args);
                agent.start();
                agentsMap.put(aid, agent);
                nodesMap.put(aid, i);
                socialNetwork.addNodeAttributes(i, trait.agreableness, trait.extroversion);
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
        MessageContent max = messages.get(0);
        List<MessageContent> thMessages = new ArrayList<MessageContent>();
        for(MessageContent m : messages) {
            if(m.nodes.size() > 3) {
                thMessages.add(m);
            }
        }
        System.out.println("Thresholded messages (with #forwards > 1): " + thMessages.size());
        for(MessageContent m : thMessages) {
            System.out.println("Message " + m.interests() + ": " + m.forwards());
            if(m.nodes.size() > max.nodes.size()) {
                max = m;
            }
        }
        System.out.println("Longest message: " + max.forwards());
        for(Interests interest : max.interests) {
            System.out.println(" - " + interest);
        }
        System.out.println("Hops:");
        for(AID hop : max.nodes) {
            System.out.println(" - " + hop.getLocalName());
        }
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
                gui.updateTicks(Person.ticks/nNodes);
            }
        }
    }
}
