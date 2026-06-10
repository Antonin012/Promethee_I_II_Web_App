package service;

import model.Alternative;
import java.util.List;

/**
 * Data wrapper to return both alternatives and the global preference matrix.
 */
public class CalculationResult {
    private List<Alternative> alternatives;
    private double[][] matrix;

    public CalculationResult(List<Alternative> alternatives, double[][] matrix) {
        this.alternatives = alternatives;
        this.matrix = matrix;
    }

    public List<Alternative> getAlternatives() { return alternatives; }
    public double[][] getMatrix() { return matrix; }
}
