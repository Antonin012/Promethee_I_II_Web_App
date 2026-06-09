package model;

import java.util.List;

/**
 * Provides the core computational logic for the PROMETHEE method.
 * It evaluates pairwise preferences and calculates outranking flows for alternatives.
 * 
 * @author Developer
 */
public class PrometheeCalcul {

    /**
     * Calculates the preference degree of a numerical difference using a specific criterion.
     * 
     * @param num the numerical difference between two evaluations
     * @param criterion the criterion which defines the preference function to apply
     * @return the calculated preference degree
     */
    public double calculatePreference(double num, Criterion criterion) {
        return criterion.getPreference(num);
    }

    /**
     * Computes the global preference matrix by comparing each alternative against every other
     * alternative across all criteria, considering criteria weights.
     * 
     * @param listA the list of alternatives to compare
     * @param listC the list of criteria to evaluate them against
     * @return a 2D array representing the global preference indices between all pairs of alternatives
     */
    public double[][] computeGlobalPreferenceMatrix(List<Alternative> listA, List<Criterion> listC) {
        int n = listA.size();
        double[][] matrix = new double[n][n];
        
        double totalWeight = 0;
        for (Criterion c : listC) {
            totalWeight += c.getWeight();
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0.0;
                } else {
                    double weightedSum = 0;
                    for (Criterion c : listC) {
                        double valI = listA.get(i).getValue(c);
                        double valJ = listA.get(j).getValue(c);
                        
                        weightedSum += c.getWeight() * c.getPreference(c.isMaximize() ? (valI - valJ) : (valJ - valI));
                    }
                    
                    matrix[i][j] = (totalWeight > 0) ? (weightedSum / totalWeight) : 0.0;
                }
            }
        }
        return matrix;
    }

    /**
     * Computes and assigns the positive, negative, and net outranking flows 
     * for a list of alternatives based on the global preference matrix.
     * 
     * @param alternatives the list of alternatives whose flows will be updated
     * @param matrix the pre-computed global preference matrix
     */
    public void computeFlows(List<Alternative> alternatives, double[][] matrix) {
        int n = alternatives.size();

        for (int i = 0; i < n; i++) {
            double phiPlus = 0;
            double phiMinus = 0;
            
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                phiPlus += matrix[i][j];
                phiMinus += matrix[j][i];
            }
            
            Alternative a = alternatives.get(i);
            a.setPhiPlus(phiPlus / (n - 1));
            a.setPhiMinus(phiMinus / (n - 1));
            a.setPhiNet(a.getPhiPlus() - a.getPhiMinus());
        }
    }

    /**
     * Executes the complete PROMETHEE calculation process: computes the global preference matrix
     * and updates the alternatives with their outranking flows.
     * 
     * @param alternatives the list of alternatives to be evaluated
     * @param criteria the list of criteria to evaluate the alternatives against
     * @return the computed global preference matrix, or an empty matrix if inputs are invalid
     */
    public double[][] calculate(List<Alternative> alternatives, List<Criterion> criteria) {
        if (alternatives == null || criteria == null || alternatives.size() < 2) {
            return new double[0][0];
        }
        double[][] matrix = computeGlobalPreferenceMatrix(alternatives, criteria);
        computeFlows(alternatives, matrix);
        return matrix;
    }
}