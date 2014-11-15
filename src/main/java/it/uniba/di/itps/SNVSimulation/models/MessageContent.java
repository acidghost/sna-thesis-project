package it.uniba.di.itps.SNVSimulation.models;

import jade.core.AID;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by acidghost on 06/11/14.
 */
public class MessageContent implements Serializable, Comparable<MessageContent> {

    public String ID;
    public Interests[] interests;
    public List<AID> nodes;

    public MessageContent(String ID, Interests[] interests, AID sender) {
        this.ID = ID;
        this.interests = interests;
        nodes = new LinkedList<AID>();
        nodes.add(sender);
    }

    public String interests() {
        String str = "";
        for (int i = 0; i < interests.length; i++) {
            Interests interest = interests[i];
            str += interest;
            if (i < interests.length-1) str += " ";
        }
        return str;
    }

    public int forwards() {
        return nodes.size()-2;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MessageContent) {
            MessageContent msg = (MessageContent) obj;
            return ID.equals(msg.ID);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public int compareTo(MessageContent msg) {
        return msg.ID.compareTo(ID);
    }
}
