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
    private double openness;
    private Interests[] interests;

    private AID[] neighbors;

    private int receivedMessages = 0;
    private int startedMessages = 0;
    private Map<AID, Integer> forwardedFrom = new HashMap<AID, Integer>();
    public static int ticks = 0;

    private Set<String> seenThreads = new HashSet<String>();
    private Map<Interests, Integer> interestsCountMap = new HashMap<Interests, Integer>();

    @Override
    protected void setup() {
        logger.info("Person agent started: " + getLocalName());
        logger.setLevel(Logger.SEVERE);

        Object[] args = getArguments();
        agreableness = (Double) args[0];
        extroversion = (Double) args[1];
        openness = (Double) args[2];
        interests = (Interests[]) args[3];
        int timingOfBehaviours = (Integer) args[4];

        SequentialBehaviour sequential = new SequentialBehaviour(this);
        sequential.addSubBehaviour(new UpdateNeighbors(this));
        ParallelBehaviour parallel = new ParallelBehaviour();
        parallel.addSubBehaviour(new ConsiderSendNewMessage(this, timingOfBehaviours));
        parallel.addSubBehaviour(new ReceiveAndForward(this, timingOfBehaviours));
        sequential.addSubBehaviour(parallel);
        addBehaviour(sequential);
    }

    @Override
    protected void takeDown() {
        /*
        NumberFormat formatter = new DecimalFormat("#0.00");
        String txt = "\n\n\n" +
                "Agent " + getLocalName() + "\n" +
                "Ext. " + formatter.format(extroversion) + " Agr. " + formatter.format(agreableness) + "\n" +
                "Ope. " + formatter.format(openness) + "\n" +
                "started: " + startedMessages + "\n" +
                "received: " + receivedMessages + "\n" +
                "forwardedFrom:\n";
        for(AID a : forwardedFrom.keySet()) {
            txt += " - " + a.getLocalName() + ": " + forwardedFrom.get(a) + "\n";
        }
        System.out.println(txt);
        */
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
                    MessageContent messageContent = new MessageContent(
                            UUID.randomUUID().toString(),
                            new Interests[]{
                                    //interests[(int) (Math.random()*interests.length)],
                                    //interests[(int) (Math.random()*interests.length)],
                                    interests[(int) (Math.random()*interests.length)]
                            }, getAID());
                    message.setContentObject(messageContent);
                    for(AID neighbor : neighbors) {
                        message.addReceiver(neighbor);
                    }
                    myAgent.send(message);
                    seenThreads.add(messageContent.ID);
                    startedMessages++;
                    logger.info("Sent message from " + getLocalName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected class ReceiveAndForward extends TickerBehaviour {
        public ReceiveAndForward(Agent a, long period) {
            super(a, period);
        }

        private void considerAcquireInterest(List<Interests> msgInterests) {
            List<Interests> myInterests = Arrays.asList(interests);
            for(Interests interest : msgInterests) {
                if(Math.random() <= ((0.8*openness) + (agreableness*0.2)) && !myInterests.contains(interest) && interestsCountMap.get(interest) > neighbors.length) {
                    Interests[] tmp = new Interests[interests.length + 1];
                    System.arraycopy(interests, 0, tmp, 0, interests.length);
                    tmp[interests.length] = interest;
                    interests = tmp;
                    logger.severe("Interest infected!\n" + getLocalName() + " agent now has " + interests.length + " interests");
                }
            }
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
        protected void onTick() {
            ACLMessage message = receive();
            if(message != null) {
                try {
                    receivedMessages++;
                    MessageContent content = (MessageContent) message.getContentObject();
                    logger.info(getLocalName() + " received from " + message.getSender().getLocalName());

                    String conversationID = content.ID;
                    if(seenThreads.contains(conversationID)) {
                        return;
                    } else {
                        seenThreads.add(conversationID);
                    }

                    double interestImpact = interestImpact(Arrays.asList(content.interests));
                    double prob = ((0.7*agreableness) + (0.3*extroversion)) + ((1 - ((0.7*agreableness) + (0.3*extroversion))) * interestImpact);
                    double random = Math.random();
                    logger.info(getLocalName() + "     PROB:  " + prob + "   RAND:   " + random);
                    if(random <= prob) {
                        // Forward message
                        ACLMessage newMessage = new ACLMessage(ACLMessage.INFORM);
                        newMessage.setSender(getAID());
                        for(AID neighbor : neighbors) {
                            if(!neighbor.equals(message.getSender()) && !content.nodes.contains(neighbor)) {
                                newMessage.addReceiver(neighbor);
                            }
                        }
                        content.nodes.add(getAID());
                        newMessage.setContentObject(content);
                        //newMessage.setConversationId(conversationID);
                        send(newMessage);
                        if(forwardedFrom.containsKey(message.getSender())) {
                            forwardedFrom.put(message.getSender(), forwardedFrom.get(message.getSender())+1);
                        } else {
                            forwardedFrom.put(message.getSender(), 1);
                        }
                        logger.info(getLocalName() + " forwarded a message from " + message.getSender().getLocalName());

                        //  Increase counter for messages seen about a topic
                        //  and then consider acquire new topic as interest
                        for(Interests interest : content.interests) {
                            Integer pastCount = interestsCountMap.get(interest);
                            if(pastCount == null) {
                                pastCount = 0;
                            }
                            interestsCountMap.put(interest, pastCount+1);
                        }
                        //considerAcquireInterest(Arrays.asList(content.interests));
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
