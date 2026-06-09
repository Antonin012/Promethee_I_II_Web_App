package model.function;

import model.PreferenceFunction;

/**
 * Represents the Gaussian Preference Function (Type 6) in the PROMETHEE method.
 * This function uses a standard deviation parameter (s) to define a continuous 
 * Gaussian curve for preference.
 * 
 * @author Developer
 */
public class GaussianFunction implements PreferenceFunction {
    
    private final double s;

    /**
     * Constructs a Gaussian Preference Function with a specified parameter s.
     * 
     * @param s the standard deviation-like parameter, which must be strictly positive
     * @throws IllegalArgumentException if s is less than or equal to zero
     */
    public GaussianFunction(double s) {
        if (s <= 0) throw new IllegalArgumentException("The parameter s must be positive");
        this.s = s;
    }

    /**
     * Calculates the preference degree based on the difference between evaluations
     * using a Gaussian distribution function.
     * 
     * @param num the difference between the evaluations of two alternatives on a specific criterion
     * @return 0.0 if the difference is less than or equal to 0, otherwise the Gaussian preference value
     */
    @Override
    public double calculate(double num) {
        return (num<=0) ? 0 : 1-Math.exp(-Math.pow(num, 2)/(2* Math.pow(s, 2)));
    }

    /**
     * Retrieves the parameter s used by this function.
     * 
     * @return the parameter s
     */
    public double getS() {
        return s;
    }
}