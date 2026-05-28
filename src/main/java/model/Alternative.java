package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;

public class Alternative {

    private String id;
    private String name;
    
    @JsonIgnore
    private Map<Criterion, Double> values;
    private double phiPlus;
    private double phiMinus;
    private double phiNet;

    public Alternative(String id, String name) {
        this(id, name, new HashMap<>());
    }

    public Alternative(String id, String name, Map<Criterion, Double> values) {
        this.id = id;
        this.name = name;
        this.values = (values != null) ? values : new HashMap<>();
        this.phiPlus = 0.0;
        this.phiMinus = 0.0;
        this.phiNet = 0.0;
    }

    public void addValue(Criterion criterion, double value) {
        this.values.put(criterion, value);
    }

    public Double getValue(Criterion criterion) {
        return this.values.getOrDefault(criterion, 0.0);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<Criterion, Double> getValues() { return values; }
    public void setValues(Map<Criterion, Double> values) { this.values = values; }

    public double getPhiPlus() { return phiPlus; }
    public void setPhiPlus(double phiPlus) { this.phiPlus = phiPlus; }

    public double getPhiMinus() { return phiMinus; }
    public void setPhiMinus(double phiMinus) { this.phiMinus = phiMinus; }

    public double getPhiNet() { return phiNet; }
    public void setPhiNet(double phiNet) { this.phiNet = phiNet; }
}