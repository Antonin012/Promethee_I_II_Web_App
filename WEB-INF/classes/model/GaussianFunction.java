package model;

public class GaussianFunction implements PreferenceFunction {
    private double s;

    public GaussianFunction(double s) { this.s = s; }

    public double calculate(double num){
        return (num<=0) ? 0 : 1-Math.exp(-Math.pow(num, 2)/(2* Math.pow(s, 2)));
    }
}
