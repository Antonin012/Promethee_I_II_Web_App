package model.function;

import model.PreferenceFunction;

/**
 * Represents the V-shape Preference Function (Type 3) in the PROMETHEE method.
 * This function utilizes a strict preference threshold (p). The preference increases 
 * linearly up to this threshold.
 * 
 * @author Developer
 */
public class VShapeFunction implements PreferenceFunction {

    private final double p;

    /**
     * Constructs a V-shape Preference Function with a specified strict preference threshold.
     * 
     * @param p the strict preference threshold, which must be strictly positive
     * @throws IllegalArgumentException if p is less than or equal to zero
     */
    public VShapeFunction(double p) {
        if (p <= 0) throw new IllegalArgumentException("p must be strictly positive");
        this.p = p;
    }

    /**
     * Calculates the preference degree based on the difference between evaluations.
     * 
     * @param num the difference between the evaluations of two alternatives on a specific criterion
     * @return 0.0 if the difference is less than or equal to 0, 1.0 if it is greater than p, 
     *         and a linear interpolation between 0 and 1 otherwise
     */
    @Override
    public double calculate(double num) {
        if (num <= 0) return 0.0;
        if (num <= p) return num / p;
        return 1.0;
    }

    /**
     * Retrieves the strict preference threshold (p) used by this function.
     * 
     * @return the strict preference threshold p
     */
    public double getP() {
        return p;
    }
}