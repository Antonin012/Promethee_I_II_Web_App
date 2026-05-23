package model;

public class UsualFunction implements PreferenceFunction {

    public double calculate(double num){
        return (num>0) ? 1 : 0;
    }    
}