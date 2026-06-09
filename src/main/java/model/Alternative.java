package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an alternative in the PROMETHEE multi-criteria decision analysis.
 * An alternative holds its evaluations across different criteria, as well as 
 * its calculated positive, negative, and net outranking flows.
 * 
 * @author Developer
 */
public class Alternative {

    private String id;
    private String name;
    
    @JsonIgnore
    private Map<Criterion, Double> values;
    private double phiPlus;
    private double phiMinus;
    private double phiNet;

    /**
     * Constructs a new Alternative with a specified ID and name.
     * Initializes the values map and sets the flows to zero.
     * 
     * @param id the unique identifier for the alternative
     * @param name the descriptive name of the alternative
     */
    public Alternative(String id, String name) {
        this(id, name, new HashMap<>());
    }

    /**
     * Constructs a new Alternative with a specified ID, name, and an existing values map.
     * 
     * @param id the unique identifier for the alternative
     * @param name the descriptive name of the alternative
     * @param values a map containing the evaluations of this alternative for various criteria
     */
    public Alternative(String id, String name, Map<Criterion, Double> values) {
        this.id = id;
        this.name = name;
        this.values = (values != null) ? values : new HashMap<>();
        this.phiPlus = 0.0;
        this.phiMinus = 0.0;
        this.phiNet = 0.0;
    }

    /**
     * Adds or updates the evaluation value of this alternative for a specific criterion.
     * 
     * @param criterion the criterion being evaluated
     * @param value the numerical evaluation value
     */
    public void addValue(Criterion criterion, double value) {
        this.values.put(criterion, value);
    }

    /**
     * Retrieves the evaluation value of this alternative for a specific criterion.
     * 
     * @param criterion the criterion to query
     * @return the evaluation value, or 0.0 if not present
     */
    public Double getValue(Criterion criterion) {
        return this.values.getOrDefault(criterion, 0.0);
    }

    /**
     * Retrieves the unique identifier of this alternative.
     * 
     * @return the alternative's ID
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier of this alternative.
     * 
     * @param id the new alternative ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Retrieves the descriptive name of this alternative.
     * 
     * @return the alternative's name
     */
    public String getName() { return name; }

    /**
     * Sets the descriptive name of this alternative.
     * 
     * @param name the new alternative name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Retrieves the map of all criterion evaluations for this alternative.
     * 
     * @return a map of criteria to their evaluation values
     */
    public Map<Criterion, Double> getValues() { return values; }

    /**
     * Sets the map of all criterion evaluations for this alternative.
     * 
     * @param values the new map of criteria to evaluation values
     */
    public void setValues(Map<Criterion, Double> values) { this.values = values; }

    /**
     * Retrieves the positive outranking flow (Phi+) of this alternative.
     * 
     * @return the positive outranking flow
     */
    public double getPhiPlus() { return phiPlus; }

    /**
     * Sets the positive outranking flow (Phi+) of this alternative.
     * 
     * @param phiPlus the new positive outranking flow
     */
    public void setPhiPlus(double phiPlus) { this.phiPlus = phiPlus; }

    /**
     * Retrieves the negative outranking flow (Phi-) of this alternative.
     * 
     * @return the negative outranking flow
     */
    public double getPhiMinus() { return phiMinus; }

    /**
     * Sets the negative outranking flow (Phi-) of this alternative.
     * 
     * @param phiMinus the new negative outranking flow
     */
    public void setPhiMinus(double phiMinus) { this.phiMinus = phiMinus; }

    /**
     * Retrieves the net outranking flow (Phi Net) of this alternative.
     * 
     * @return the net outranking flow
     */
    public double getPhiNet() { return phiNet; }

    /**
     * Sets the net outranking flow (Phi Net) of this alternative.
     * 
     * @param phiNet the new net outranking flow
     */
    public void setPhiNet(double phiNet) { this.phiNet = phiNet; }
}