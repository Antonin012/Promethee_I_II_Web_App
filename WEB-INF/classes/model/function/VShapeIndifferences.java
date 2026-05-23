package model.function;

import model.PreferenceFunction;

public class VShapeIndifferences implements PreferenceFunction {
    
    private final double q;
    private final double p;
    
    public VShapeIndifferences(double q, double p) {
        if (q < 0 || p <= q) {
            throw new IllegalArgumentException("We must have 0 <= q < p.");
        }
        this.q = q;
        this.p = p;
    }
    
    @Override
    public double calculate(double num) {
        return (num<=q) ? 0 : (num<=p) ? (num-q)/(p-q):1;
    }

    public double getQ() { return q; }
    public double getP() { return p; }
}
