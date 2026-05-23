package model;

public class UshapeFunction implements PreferenceFunction{
    private double q;

    public UshapeFunction(double q) { this.q = q; }

    public double calculate(double num){
        return (num>q) ? 1 : 0;
    }
}
