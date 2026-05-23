package model;

public class VShapeFunction implements PreferenceFunction{

    private double p;

    public VShapeFunction(double p){this.p=p;}

    public double calculate(double num){
        return (num<=0) ? 0 : (num<=p) ? num/p:1;
    }
}