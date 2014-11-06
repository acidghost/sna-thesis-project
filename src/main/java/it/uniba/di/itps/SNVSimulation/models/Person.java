package it.uniba.di.itps.SNVSimulation.models;

import it.uniba.di.itps.SNVSimulation.Simulation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by acidghost on 13/09/14.
 */
public class Person extends Agent {
    private Logger logger = Logger.getJADELogger(getClass().getName());

    private double agreableness;
    private double extroversion;
    private Interests[] interests;

    private AID[] neighbors;

    private int receivedMessages = 0;
    private int startedMessages = 0;
    private Map<AID, Integer> forwardedFrom = new HashMap<AID, Integer>();
    public static int ticks = 0;

    private Set<String> seenThreads = new HashSet<String>();

    @Override
    protected void setup() {
        logger.info("Person agent started: " + getLocalName());
        logger.setLevel(Logger.SEVERE);

        Object[] args = getArguments();
        agreableness = (Double) args[0];
        extroversion = (Double) args[1];
        interests = (Interests[]) args[2];

        SequentialBehaviour sequential = new SequentialBehaviour(this);
        sequential.addSubBehaviour(new UpdateNeighbors(this));
        ParallelBehaviour parallel = new ParallelBehaviour();
        parallel.addSubBehaviour(new ConsiderSendNewMessage(this, 100));
        parallel.addSubBehaviour(new ReceiveAndForward());
        sequential.addSubBehaviour(parallel);
        addBehaviour(sequential);
    }

    @Override
    protected void takeDown() {
        NumberFormat formatter = new DecimalFormat("#0.00");
        String txt = "\n\n\n" +
                "Agent " + getLocalName() + "\n" +
                "Ext. " + formatter.format(extroversion) + " Agr. " + formatter.format(agreableness) + "\n" +
                "started: " + startedMessages + "\n" +
                "received: " + receivedMessages + "\n" +
                "forwardedFrom:\n";
        for(AID a : forwardedFrom.keySet()) {
            txt += " - " + a.getLocalName() + ": " + forwardedFrom.get(a) + "\n";
        }
        System.out.println(txt);
    }

    protected class UpdateNeighbors extends OneShotBehaviour {
        public UpdateNeighbors(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("social-network");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setSender(getAID());
                for (DFAgentDescription aResult : result) {
                    msg.addReceiver(aResult.getName());
                }
                msg.setOntology("get-neighbors");
                msg.setConversationId(myAgent.getName());
                myAgent.send(msg);
                ACLMessage reply = myAgent.blockingReceive(MessageTemplate.MatchConversationId(myAgent.getName()));
                String[] agentsNames = reply.getContent().split("#");
                neighbors = new AID[agentsNames.length];
                for (int i = 0; i < agentsNames.length; i++) {
                    neighbors[i] = new AID(agentsNames[i], AID.ISLOCALNAME);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    protected class ConsiderSendNewMessage extends TickerBehaviour {
        public ConsiderSendNewMessage(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ticks++;
            double random = Math.random();
            if(random <= extroversion) {
                ACLMessage message = new ACLMessage(ACLMessage.PROPAGATE);
                message.setSender(getAID());
                try {
                    MessageContent messageContent = new MessageContent(new Interests[]{ interests[(int) (Math.random()*interests.length)] }, getAID());
                    message.setContentObject(messageContent);
                    for(AID neighbor : neighbors) {
                        message.addReceiver(neighbor);
                    }
                    myAgent.send(message);
                    seenThreads.add(message.getConversationId());
                    startedMessages++;
                    logger.info("Sent message from " + getLocalName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected class ReceiveAndForward extends CyclicBehaviour {
        private void considerAcquireInterest(List<Interests> msgInterests) {

        }

        private double interestImpact(List<Interests> msgInterests) {
            List<Interests> myInterests = Arrays.asList(interests);
            List<Interests> matched = new ArrayList<Interests>();
            for (Interests interest : msgInterests) {
                if (myInterests.contains(interest)) {
                    matched.add(interest);
                }
            }
            //return matched.size() / interests.length;
            return matched.size() / msgInterests.size();
        }

        @Override
        public void action() {
            ACLMessage message = receive();
            if(message != null) {
                if(seenThreads.contains(message.getConversationId())) {
                    return;
                }

                try {
                    receivedMessages++;
                    MessageContent content = (MessageContent) message.getContentObject();
                    logger.info(getLocalName() + " received from " + message.getSender().getLocalName());

                    double interestImpact = interestImpact(Arrays.asList(content.interests));
                    double prob = (agreableness*extroversion) + ((1-agreableness*extroversion) * interestImpact);
                    double random = Math.random();
                    logger.info(getLocalName() + "     PROB:  " + prob + "   RAND:   " + random);
                    if(random <= prob) {
                        // Forward message
                        ACLMessage newMessage = new ACLMessage(ACLMessage.INFORM);
                        newMessage.setSender(getAID());
                        for(AID neighbor : neighbors) {
                            if(!neighbor.equals(message.getSender())) {
                                newMessage.addReceiver(neighbor);
                            }
                        }
                        content.nodes.add(getAID());
                        newMessage.setContentObject(content);
                        send(newMessage);
                        if(forwardedFrom.containsKey(message.getSender())) {
                            forwardedFrom.put(message.getSender(), forwardedFrom.get(message.getSender())+1);
                        } else {
                            forwardedFrom.put(message.getSender(), 1);
                        }
                        logger.info(getLocalName() + " forwarded a message from " + message.getSender().getLocalName());
                    } else {
                        //  MESSAGE, your journey to Miss Italy ends here!
                        ACLMessage newMessage = new ACLMessage(ACLMessage.INFORM);
                        newMessage.setOntology("ending-thread");
                        newMessage.setSender(getAID());
                        newMessage.addReceiver(new AID(Simulation.AGENT_NAME, AID.ISLOCALNAME));
                        content.nodes.add(getAID());
                        newMessage.setContentObject(content);
                        send(newMessage);
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }
}
