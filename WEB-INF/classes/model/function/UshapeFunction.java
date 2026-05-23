package model.function;

import model.PreferenceFunction;

public class UshapeFunction implements PreferenceFunction {
    
    private final double q;

    public UshapeFunction(double q) {
        if (q < 0) throw new IllegalArgumentException("q must be positive or null");
        this.q = q;
    }

    @Override
    public double calculate(double num) {
        return (num > q) ? 1.0 : 0.0;
    }

    public double getQ() {
        return q;
    }
}
