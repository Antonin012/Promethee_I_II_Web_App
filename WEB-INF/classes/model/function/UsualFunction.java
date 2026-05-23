package model.function;

import model.PreferenceFunction;

public class UsualFunction implements PreferenceFunction {

    @Override
    public double calculate(double num) {
        return (num > 0) ? 1.0 : 0.0;
    }    
}