package model.function;

import model.PreferenceFunction;

/**
 * Represents the V-shape with Indifference Preference Function (Type 5) in the PROMETHEE method.
 * This function uses both an indifference threshold (q) and a strict preference threshold (p).
 * The preference is 0 up to q, increases linearly from q to p, and is 1 beyond p.
 * 
 * @author Developer
 */
public class VShapeIndifferences implements PreferenceFunction {
    
    private final double q;
    private final double p;
    
    /**
     * Constructs a V-shape with Indifference Preference Function.
     * 
     * @param q the indifference threshold
     * @param p the strict preference threshold
     * @throws IllegalArgumentException if q is negative or if p is less than or equal to q
     */
    public VShapeIndifferences(double q, double p) {
        if (q < 0 || p <= q) {
            throw new IllegalArgumentException("We must have 0 <= q < p.");
        }
        this.q = q;
        this.p = p;
    }
    
    /**
     * Calculates the preference degree based on the difference between evaluations.
     * 
     * @param num the difference between the evaluations of two alternatives on a specific criterion
     * @return 0.0 if the difference is less than or equal to q, 1.0 if it is greater than p, 
     *         and a linear interpolation between q and p otherwise
     */
    @Override
    public double calculate(double num) {
        return (num<=q) ? 0 : (num<=p) ? (num-q)/(p-q):1;
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