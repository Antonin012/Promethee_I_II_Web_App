package model.function;

import model.PreferenceFunction;

public class VShapeFunction implements PreferenceFunction {

    private final double p;

    public VShapeFunction(double p) {
        if (p <= 0) throw new IllegalArgumentException("p must be strictly positive");
        this.p = p;
    }

    @Override
    public double calculate(double num) {
        if (num <= 0) return 0.0;
        if (num <= p) return num / p;
        return 1.0;
    }

    public double getP() {
        return p;
    }
}