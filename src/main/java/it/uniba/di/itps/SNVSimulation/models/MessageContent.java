package it.uniba.di.itps.SNVSimulation.models;

import jade.core.AID;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by acidghost on 06/11/14.
 */
public class MessageContent implements Serializable {

    public Interests[] interests;
    public List<AID> nodes;

    public MessageContent(Interests[] interests, AID sender) {
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
}
