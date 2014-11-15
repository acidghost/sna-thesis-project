package it.uniba.di.itps.SNVSimulation.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by acidghost on 13/09/14.
 */
public enum Interests {
    BEACH,
    MOUNTAIN,
    MOTORBIKE,
    YACHT,
    DOGS,
    CATS,
    PENGUINS,
    VEGAN,
    MEAT,
    SCIENCE,
    TENNIS,
    SOCCER,
    BASKET,
    TRAVELS,
    FITNESS,
    PHOTOGRAPHY,
    TECHNOLOGY,
    READING;




    private static final List<Interests> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();

    public static Interests randomInterest()  {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }

    public static Interests randomInterestIncl(Interests... interests) {
        List<Interests> lInterests = Collections.unmodifiableList(Arrays.asList(interests));
        Interests interest;
        do {
            interest = randomInterest();
        } while (!lInterests.contains(interest));
        return interest;
    }
}
