package model.function;

import model.PreferenceFunction;

public class GaussianFunction implements PreferenceFunction {
    
    private final double s;

    public GaussianFunction(double s) {
        if (s <= 0) throw new IllegalArgumentException("The parameter s must be positive");
        this.s = s;
    }

    @Override
    public double calculate(double num) {
        return (num<=0) ? 0 : 1-Math.exp(-Math.pow(num, 2)/(2* Math.pow(s, 2)));
    }

    public double getS() {
        return s;
    }
}
