package model;

/**
 * Defines the contract for a preference function used in the PROMETHEE method.
 * Implementing classes provide specific calculation logic for different preference types.
 * 
 * @author Developer
 */
public interface PreferenceFunction {

    /**
     * Calculates the preference degree given a difference in evaluations.
     * 
     * @param num the numerical difference between the evaluations of two alternatives
     * @return the preference degree, typically a value between 0.0 and 1.0
     */
    public double calculate(double num);
}