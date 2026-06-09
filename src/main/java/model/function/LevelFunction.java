package model.function;

import model.PreferenceFunction;

/**
 * Represents the Level Preference Function (Type 4) in the PROMETHEE method.
 * This function utilizes an indifference threshold (q) and a strict preference threshold (p).
 * The preference degree jumps to 0.5 when the difference exceeds q, and to 1 when it exceeds p.
 * 
 * @author Developer
 */
public class LevelFunction implements PreferenceFunction {

    private final double q;
    private final double p;

    /**
     * Constructs a Level Preference Function with specified indifference and strict preference thresholds.
     * 
     * @param q the indifference threshold
     * @param p the strict preference threshold
     * @throws IllegalArgumentException if q is negative or if p is less than or equal to q
     */
    public LevelFunction(double q, double p) {
        if (q < 0 || p <= q) {
            throw new IllegalArgumentException("We must have 0 <= q < p");
        }
        this.q = q;
        this.p = p;
    }

    /**
     * Calculates the preference degree based on the difference between evaluations.
     * 
     * @param num the difference between the evaluations of two alternatives on a specific criterion
     * @return 0.0 if the difference is less than or equal to q, 0.5 if it is between q and p, 
     *         and 1.0 if it is strictly greater than p
     */
    @Override
    public double calculate(double num) {
        return (num<=q) ? 0 : (num<=p) ? 0.5 : 1;
    }

    /**
     * Retrieves the indifference threshold (q) used by this function.
     * 
     * @return the indifference threshold q
     */
    public double getQ() { return q; }

    /**
     * Retrieves the strict preference threshold (p) used by this function.
     * 
     * @return the strict preference threshold p
     */
    public double getP() { return p; }
}