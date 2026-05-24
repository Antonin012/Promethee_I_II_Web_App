package model;

public class Criterion {
    private String name;
    private double weight;
    private boolean isMaximize;
    private PreferenceFunction preferenceFunction;
    
    public Criterion(String name, double weight, boolean isMaximize, PreferenceFunction preferenceFunction) {
        this.name = name;
        this.weight = weight;
        this.isMaximize = isMaximize;
        this.preferenceFunction = preferenceFunction;
    }

    public double getPreference(double diff) {
        if (preferenceFunction == null) return 0.0;
        return preferenceFunction.calculate(diff);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public boolean isMaximize() { return isMaximize; }
    public void setMaximize(boolean maximize) { isMaximize = maximize; }

    public PreferenceFunction getPreferenceFunction() { return preferenceFunction; }
    public void setPreferenceFunction(PreferenceFunction preferenceFunction) { this.preferenceFunction = preferenceFunction; }
}
