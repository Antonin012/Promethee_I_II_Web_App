package model;

import java.util.ArrayList;
import java.util.List;

public class PrometheeCalcul {
    public double calculateNormalization(ArrayList<Alternative> listA,ArrayList<Criterion> listC){
        return 0.0;
    }

    public double calculatePreference(double num,Criterion criterion){
        return 0.0;
    }

    public double[][] computeGlobalPreferenceMatrix(ArrayList<Alternative> listA,ArrayList<Criterion> listC){
        return new double[0][0];
    }

    public void computeFlows(List<Alternative> alternatives, double[][] matrix){}

    public void computeNetFlows(Alternative alternative){}
}
