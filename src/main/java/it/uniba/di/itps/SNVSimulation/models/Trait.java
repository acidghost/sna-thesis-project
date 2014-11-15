package it.uniba.di.itps.SNVSimulation.models;

/**
 * Created by acidghost on 13/09/14.
 */
public class Trait {
    public double agreableness;
    public double extroversion;
    public double openness;
    public Interests[] interests;

    public Trait(double agreableness, double extroversion, double openness, Interests... interests) {
        this.agreableness = agreableness;
        this.extroversion = extroversion;
        this.openness = openness;
        this.interests = interests;
    }

    public Trait() {
        agreableness = Math.random();
        extroversion = Math.random();
        openness = Math.random() * 0.8;
        interests = new Interests[3];
        interests[0] = Interests.randomInterest();
        interests[1] = Interests.randomInterest();
        interests[2] = Interests.randomInterest();
    }

    public Trait(Interests... includeInterest) {
        this();
        for (int i = 0; i < interests.length; i++) {
            if(Math.random() <= 0.8) {
                interests[i] = Interests.randomInterestIncl(includeInterest);
            }
        }
    }

    public Object[] toObjectArray() {
        return new Object[] {
                agreableness,
                extroversion,
                openness,
                interests
        };
    }
}
