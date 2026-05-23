package model;

public class LevelFunction implements PreferenceFunction {

    private double p;
    private double q;

    public LevelFunction(double q, double p) {
        this.q = q;
        this.p = p;
    }

    public double calculate(double num){
        return (num<=q) ? 0 : (num<=p) ? 0.5 : 1;
    }
}