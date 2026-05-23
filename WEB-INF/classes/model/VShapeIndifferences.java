package model;

public class VShapeIndifferences implements PreferenceFunction{
    private double q;
    private double p;
    
    public VShapeIndifferences(double q, double p) {
        this.q = q;
        this.p = p;
    }
    
    public double calculate(double num){
        return (num<=q) ? 0 : (num<=p) ? (num-q)/(p-q):1;
    }
}
