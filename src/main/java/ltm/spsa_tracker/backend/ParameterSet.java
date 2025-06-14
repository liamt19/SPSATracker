package ltm.spsa_tracker.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.stream.Collectors;

public record ParameterSet(List<Parameter> parameters, int iteration) {

	public static final int NO_ITERATION = -1;

	public String toCSV() {
		return iteration + "," + parameters.stream().map(a -> a.current().toString()).collect(Collectors.joining(","));
	}

	public String getCSVHeaders() {
		return "iteration," + parameters.stream().map(a -> a.name()).collect(Collectors.joining(","));
	}

	public Vector<String> toVector() {
		Vector<String> v = new Vector<String>();
		v.addAll(Arrays.asList(toCSV().split(",")));
		return v;
	}

	private List<Entry<String, Float>> calculateDelta(ParameterSet other) {
		var list = new ArrayList<Entry<String, Float>>();
		for (int i = 0; i < parameters.size(); i++) {
			Parameter thisParam = parameters.get(i);
			Parameter otherParam = other.parameters.get(i);

			float delta = otherParam.current() - thisParam.current();
			list.add(Map.entry(thisParam.name(), delta));
		}
		return list;
	}

	public void printDeltaFrom(ParameterSet previousIteration) {

		if (previousIteration.parameters() == null) {
			return;
		}

		System.out.println("==========" + iteration + "==========");

		for (int i = 0; i < parameters.size(); i++) {
			Parameter oldParam = previousIteration.parameters().get(i);
			Parameter newParam = parameters.get(i);
			if (newParam.current() != oldParam.current()) {
				System.out.println(String.format("%-40s%10s -> %10s", oldParam.name(), oldParam.current(), newParam.current()));
			}
		}

		System.out.println();
	}
}
