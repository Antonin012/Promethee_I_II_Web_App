package model;

import java.util.List;

public class PrometheeCalcul {


    public double calculatePreference(double num, Criterion criterion) {
        return criterion.getPreference(num);
    }


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

    public double[][] calculate(List<Alternative> alternatives, List<Criterion> criteria) {
        if (alternatives == null || criteria == null || alternatives.size() < 2) {
            return new double[0][0];
        }
        double[][] matrix = computeGlobalPreferenceMatrix(alternatives, criteria);
        computeFlows(alternatives, matrix);
        return matrix;
    }
}
