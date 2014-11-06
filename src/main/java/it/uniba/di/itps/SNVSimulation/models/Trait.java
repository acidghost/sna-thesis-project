package it.uniba.di.itps.SNVSimulation.models;

/**
 * Created by acidghost on 13/09/14.
 */
public class Trait {
    public double agreableness;
    public double extroversion;
    public Interests[] interests;

    public Trait(double agreableness, double extroversion, Interests... interests) {
        this.agreableness = agreableness;
        this.extroversion = extroversion;
        this.interests = interests;
    }

    public Trait() {
        agreableness = Math.random();
        extroversion = Math.random();
        interests = new Interests[3];
        interests[0] = Interests.randomInterest();
        interests[1] = Interests.randomInterest();
        interests[2] = Interests.randomInterest();
    }

    public Object[] toObjectArray() {
        return new Object[] {
                agreableness,
                extroversion,
                interests
        };
    }
}
