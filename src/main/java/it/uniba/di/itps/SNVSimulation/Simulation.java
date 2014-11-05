package it.uniba.di.itps.SNVSimulation;

import jade.core.Agent;
import jade.util.Logger;

/**
 * Created by acidghost on 05/11/14.
 */
public class Simulation extends Agent {

    private Logger logger = Logger.getJADELogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Simulation setup...");
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }
}
