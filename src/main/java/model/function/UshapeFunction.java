package model.function;

import model.PreferenceFunction;

/**
 * Represents the U-shape Preference Function (Type 2) in the PROMETHEE method.
 * This function utilizes an indifference threshold (q). If the difference is below 
 * or equal to this threshold, there is no preference (0). Otherwise, there is strict preference (1).
 * 
 * @author Developer
 */
public class UshapeFunction implements PreferenceFunction {
    
    private final double q;

    /**
     * Constructs a U-shape Preference Function with a specified indifference threshold.
     * 
     * @param q the indifference threshold, which must be a positive number or zero
     * @throws IllegalArgumentException if q is strictly negative
     */
    public UshapeFunction(double q) {
        if (q < 0) throw new IllegalArgumentException("q must be positive or null");
        this.q = q;
    }

    /**
     * Calculates the preference degree based on the difference between evaluations.
     * 
     * @param num the difference between the evaluations of two alternatives on a specific criterion
     * @return 1.0 if the difference is strictly greater than the threshold q, 0.0 otherwise
     */
    @Override
    public double calculate(double num) {
        return (num > q) ? 1.0 : 0.0;
    }

    /**
     * Retrieves the indifference threshold (q) used by this function.
     * 
     * @return the indifference threshold q
     */
    public double getQ() {
        return q;
    }
}