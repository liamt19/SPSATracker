package ltm.spsa_tracker.backend;

import java.util.Arrays;

public record Parameter(String name, Float current, Float start, Float min, Float max, Float C, Float C_end, Float R, Float R_end) {

    public Parameter(String name, Float current) {
	this(name, current, null, null, null, null, null, null, null);
    }
    
    public Object[] toDisplayable() {
	return new Object[]{ name, current, start, min, max, C, C_end, R, R_end };
    }
    
    public String toCSV() {
	var objs = toDisplayable();
	var stringArr = Arrays.stream(objs).toArray(String[]::new);
	return String.join(",", stringArr);
    }
    
}
