package model;

import java.util.UUID;

/**
 * Represents a criterion used to evaluate alternatives in the PROMETHEE method.
 * A criterion has a weight, an optimization direction (maximize or minimize), 
 * and a specific preference function to calculate pairwise preference degrees.
 * 
 * @author Developer
 */
public class Criterion {
    private String id;
    private String name;
    private double weight;
    private boolean isMaximize;
    private PreferenceFunction preferenceFunction;
    
    /**
     * Constructs a new Criterion and automatically generates a unique identifier.
     * 
     * @param name the descriptive name of the criterion
     * @param weight the relative importance weight of the criterion
     * @param isMaximize true if higher values are better, false if lower values are better
     * @param preferenceFunction the function used to evaluate preference degrees
     */
    public Criterion(String name, double weight, boolean isMaximize, PreferenceFunction preferenceFunction) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.weight = weight;
        this.isMaximize = isMaximize;
        this.preferenceFunction = preferenceFunction;
    }

    /**
     * Constructs a new Criterion with a specific identifier.
     * 
     * @param id the unique identifier for the criterion
     * @param name the descriptive name of the criterion
     * @param weight the relative importance weight of the criterion
     * @param isMaximize true if higher values are better, false if lower values are better
     * @param preferenceFunction the function used to evaluate preference degrees
     */
    public Criterion(String id, String name, double weight, boolean isMaximize, PreferenceFunction preferenceFunction) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.isMaximize = isMaximize;
        this.preferenceFunction = preferenceFunction;
    }

    /**
     * Calculates the preference degree for a given difference using this criterion's function.
     * 
     * @param diff the numerical difference between the evaluations of two alternatives
     * @return the calculated preference degree, or 0.0 if no preference function is set
     */
    public double getPreference(double diff) {
        if (preferenceFunction == null) return 0.0;
        return preferenceFunction.calculate(diff);
    }

    /**
     * Retrieves the unique identifier of this criterion.
     * 
     * @return the criterion's ID
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier of this criterion.
     * 
     * @param id the new criterion ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Retrieves the descriptive name of this criterion.
     * 
     * @return the criterion's name
     */
    public String getName() { return name; }

    /**
     * Sets the descriptive name of this criterion.
     * 
     * @param name the new criterion name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Retrieves the relative importance weight of this criterion.
     * 
     * @return the weight value
     */
    public double getWeight() { return weight; }

    /**
     * Sets the relative importance weight of this criterion.
     * 
     * @param weight the new weight value
     */
    public void setWeight(double weight) { this.weight = weight; }

    /**
     * Checks if this criterion should be maximized.
     * 
     * @return true if higher values are preferred, false otherwise
     */
    public boolean isMaximize() { return isMaximize; }

    /**
     * Sets the optimization direction for this criterion.
     * 
     * @param maximize true to maximize, false to minimize
     */
    public void setMaximize(boolean maximize) { isMaximize = maximize; }

    /**
     * Retrieves the preference function associated with this criterion.
     * 
     * @return the preference function
     */
    public PreferenceFunction getPreferenceFunction() { return preferenceFunction; }

    /**
     * Sets the preference function for this criterion.
     * 
     * @param preferenceFunction the new preference function
     */
    public void setPreferenceFunction(PreferenceFunction preferenceFunction) { this.preferenceFunction = preferenceFunction; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Criterion criterion = (Criterion) o;
        return java.util.Objects.equals(id, criterion.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}