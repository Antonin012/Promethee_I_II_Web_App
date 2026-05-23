package model;

import java.util.HashMap;
import java.util.Map;

public class Alternative {

    private String ID;
    private String name;
    private Map<Criterion,Double> values;
    private Double phiPlus;
    private Double phiMinus;
    private Double phiNet;

    public Alternative(String ID, String name, Map<Criterion, Double> values) {
        this.ID=ID;
        this.name=name;
        this.values=(values!=null) ? values : new HashMap<>();
        this.phiPlus=0.0;
        this.phiMinus=0.0;
        this.phiNet=0.0;
    }
    
}