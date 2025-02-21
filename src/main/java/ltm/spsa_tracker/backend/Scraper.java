package ltm.spsa_tracker.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.*;
import org.jsoup.nodes.*; 
import org.jsoup.select.*;

public class Scraper {
    private String instanceURL;
    private int testID;
    
    public Scraper (String instanceURL, int testID) {
	this.instanceURL = instanceURL;
	this.testID = testID;
    }
    
    public ParameterSet getCurrentParameters() {
	List<Parameter> list = new ArrayList<Parameter>();
	int iteration = 0;
	
	Document doc;
	var url = String.format("%s/tune/%s/", instanceURL, testID);
	
	try {
	    doc = Jsoup.connect(url).get(); 
	} catch (IOException e) { 
	    System.err.println("Failed connecting to " + url + ": " + e.getMessage());
	    return null;
	}
	
	var statBlock = doc.selectXpath("//*[@id=\"long-statblock\"]").first();
	if (statBlock == null) {
	    //	OpenBench currently redirects to .../index/ for non-existent workloads
	    System.err.println("Failed to locate tune statblock! Is the test ID correct?");
	    return null;
	}
	
	Pattern patt = Pattern.compile(".+?(\\d+)/(\\d+).+");
	Matcher matcher = patt.matcher(statBlock.text());
	if (!matcher.find()) {
	    System.out.println("Failed to find iteration number!");
	}
	else {
	    iteration = Integer.parseInt(matcher.group(1));
	}
	
	var paramDiv = doc.selectXpath("//div[contains(@id, 'parameters')]").first();
	var table = paramDiv.selectFirst("table");
        var tbody = table.selectFirst("tbody");

        int nRows = tbody.childrenSize();
        for (int i = 1; i < nRows; i += 2) {
	    var row = tbody.child(i);
	    
	    var name = row.child(0).text();
	    var current = Float.parseFloat(row.child(1).text());
	    var start = Float.parseFloat(row.child(2).text());
	    var min = Float.parseFloat(row.child(3).text());
	    var max = Float.parseFloat(row.child(4).text());
	    var c = Float.parseFloat(row.child(5).text());
	    var c_end = Float.parseFloat(row.child(6).text());
	    var r = Float.parseFloat(row.child(7).text());
	    var r_end = Float.parseFloat(row.child(8).text());
	    
	    Parameter p = new Parameter(name, current, start, min, max, c, c_end, r, r_end);
	    list.add(p);
	}

	return new ParameterSet(list, iteration);
    }

}
