package it.uniba.di.itps.SNVSimulation.models;

import jade.core.AID;

import java.util.Set;

/**
 * Created by acidghost on 11/11/14.
 */
public class MessageCascade implements Comparable<MessageCascade> {

    public MessageContent content;
    public Set<AID> cascade;
    public int startingDegree;
    public AID startingNode;
    public double averageCascadeExtroversion;
    public double averageCascadeAgreableness;

    public MessageCascade(MessageContent content, Set<AID> cascade, int startingDegree) {
        this.content = content;
        this.cascade = cascade;
        this.startingDegree = startingDegree;
        startingNode = content.nodes.get(0);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MessageCascade) {
            MessageCascade msg = (MessageCascade) obj;
            return content.equals(msg.content);
        }
        return false;
    }

    @Override
    public int compareTo(MessageCascade messageCascade) {
        if(cascade.size() > messageCascade.cascade.size()) {
            return 1;
        } else if(cascade.size() < messageCascade.cascade.size()) {
            return -1;
        } else {
            return 0;
        }
    }
}
