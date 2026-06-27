package model;

import java.util.List;
import java.util.Map;

/**
 * Data wrapper to return alternatives, the global preference matrix,
 * and optionally the GAIA plane projection data.
 */
public class CalculationResult {
    private List<Alternative> alternatives;
    private double[][] matrix;
    private Map<String, Object> gaiaData;

    public CalculationResult(List<Alternative> alternatives, double[][] matrix) {
        this.alternatives = alternatives;
        this.matrix = matrix;
    }

    public List<Alternative> getAlternatives() { return alternatives; }
    public double[][] getMatrix() { return matrix; }

    public Map<String, Object> getGaiaData() { return gaiaData; }
    public void setGaiaData(Map<String, Object> gaiaData) { this.gaiaData = gaiaData; }
}
