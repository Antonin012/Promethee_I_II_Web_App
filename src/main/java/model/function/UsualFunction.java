package model.function;

import model.PreferenceFunction;

/**
 * Represents the Usual Preference Function (Type 1) in the PROMETHEE method.
 * This function returns strict preference (1) as soon as there is a positive difference,
 * and no preference (0) otherwise.
 * 
 * @author Developer
 */
public class UsualFunction implements PreferenceFunction {

    /**
     * Calculates the preference degree based on the difference between evaluations.
     * 
     * @param num the difference between the evaluations of two alternatives on a specific criterion
     * @return 1.0 if the difference is strictly positive, 0.0 otherwise
     */
    @Override
    public double calculate(double num) {
        return (num > 0) ? 1.0 : 0.0;
    }    
}