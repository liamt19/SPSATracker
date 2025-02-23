package ltm.spsa_tracker.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ParameterHandler {

    public static final String SAVE_FOLDER = "saved";
    
    public static List<ParameterSet> LoadFromFile(String instanceURL, int testID) {
	List<ParameterSet> paramList = new ArrayList<ParameterSet>();

	String cwd = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
	String subfolder = instanceURL.substring(instanceURL.lastIndexOf("/"));
	String folderPath = Paths.get(cwd, SAVE_FOLDER, subfolder).toString();
	String fileName = testID + ".csv";
	String filePath = Paths.get(folderPath, fileName).toString();
	
	try {
	    Files.createDirectories(Paths.get(folderPath));
	} catch (IOException e) {
	    e.printStackTrace();
	}

	try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

	    String header = br.readLine();
	    var paramNames = header.split(",");

	    String line;
	    while ((line = br.readLine()) != null) {
		var splits = line.split(",");
		List<Parameter> params = new ArrayList<Parameter>();
		int iteration = Integer.parseInt(splits[0]);
		for (int i = 1; i < splits.length; i++) {
		    params.add(new Parameter(paramNames[i], Float.parseFloat(splits[i])));
		}

		paramList.add(new ParameterSet(params, iteration));
	    }
	} catch (IOException e) {
	    System.err.println("Error reading file: " + e.getMessage());
	}

	return paramList;
    }

    public static void SaveToFile(String instanceURL, int testID, ParameterSet paramSet) {

	String cwd = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
	String subfolder = instanceURL.substring(instanceURL.lastIndexOf("/"));
	String folderPath = Paths.get(cwd, SAVE_FOLDER, subfolder).toString();
	String fileName = testID + ".csv";
	String filePath = Paths.get(folderPath, fileName).toString();
	String stepPath = Paths.get(folderPath, testID + ".info.csv").toString();;
	
	try {
	    Files.createDirectories(Paths.get(folderPath));
	} catch (IOException e) {
	    e.printStackTrace();
	}
	
	if (!fileExists(filePath)) {
	    writeFileHeaders(filePath, paramSet);
	}
	
	if (!fileExists(stepPath)) {
	    writeStepSize(stepPath, paramSet);
	}

	if (!headersMatch(filePath, paramSet)) {
	    handleExistingFile(filePath, paramSet);
	    handleExistingFile(stepPath, paramSet);
	}

	//SQLHandler.createParameterListTable(subfolder, "Test" + testID, paramSet);
	//SQLHandler.insertSingleParameterSetIteration(subfolder, "Test" + testID, paramSet);
	
	int lastIteration = getLastSavedIteration(filePath);
	if (lastIteration == paramSet.iteration()) {
	    System.out.println("File already has iteration " + lastIteration);
	    return;
	}

	try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), Charset.defaultCharset(),
		StandardOpenOption.APPEND)) {
	    writer.write(paramSet.toCSV());
	    writer.newLine();
	    System.out.println("Saved");
	} catch (IOException e) {
	    System.err.println("Failed writing: " + e.getMessage());
	}
    }

    private static boolean fileExists(String filePath) {
	File f = new File(filePath);
	return f.isFile();
    }

    private static boolean headersMatch(String filePath, ParameterSet paramSet) {
	File f = new File(filePath);
	if (!f.isFile()) {
	    System.out.println("isFileOK called on non-existant file!");
	    return false;
	}

	try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
	    String line;
	    if ((line = br.readLine()) != null) {
		String expected = paramSet.getCSVHeaders();
		if (line.equals(expected)) {
		    return true;
		}
	    }
	} catch (IOException e) {
	    System.err.println("Error reading file: " + e.getMessage());
	}

	return false;
    }

    private static void handleExistingFile(String filePath, ParameterSet paramSet) {
	Path src = Path.of(filePath);
	Path dst = Path.of(filePath.concat(".bak"));

	try {
	    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
	    System.out.println("Made backup of \"" + filePath + "\"");

	    Files.delete(src);
	    writeFileHeaders(filePath, paramSet);
	} catch (IOException e) {
	    System.out.println("Failed making backup for \"" + filePath + "\", overwriting!");
	}
    }

    private static void writeFileHeaders(String filePath, ParameterSet paramSet) {
	try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
	    writer.write(paramSet.getCSVHeaders());
	    writer.newLine();
	    
	    System.out.println("Wrote headers!");
	} catch (IOException e) {
	    System.err.println("Failed writing: " + e.getMessage());
	}
    }
    
    private static void writeStepSize(String filePath, ParameterSet paramSet) {
	try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
	    writer.write("name,start,step");
	    writer.newLine();
	    
	    var paramList = paramSet.parameters();
	    for (int i = 0; i < paramList.size(); i++) {
		Parameter param = paramList.get(i);
		
		writer.write(param.name());
		writer.write(",");
		writer.write(Float.toString(param.start()));
		writer.write(",");
		writer.write(Float.toString(param.C_end()));
		writer.newLine();
	    }
	    
	} catch (IOException e) {
	    System.err.println("Failed writing: " + e.getMessage());
	}
    }

    private static int getLastSavedIteration(String filePath) {
	int iter = ParameterSet.NO_ITERATION;

	try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
	    String header = br.readLine();
	    if (header == null) {
		System.out.println("Error reading file!");
		return iter;
	    }

	    var lines = br.lines().toArray(String[]::new);
	    System.out.println("File has " + lines.length + " saved iterations");
	    if (lines.length == 0) {
		return iter;
	    }

	    var lastLine = lines[lines.length - 1];
	    var splits = lastLine.split(",");
	    if (splits.length == 1) {
		System.out.println("csv file is malformed?");
		return iter;
	    }

	    try {
		iter = Integer.parseInt(splits[0]);
	    } catch (NumberFormatException nfe) {
		System.err.println("Failed parsing the iteration from \"" + lastLine + "\"!");
	    }
	    return iter;
	} catch (IOException e) {
	    System.err.println("Error reading file: " + e.getMessage());
	}

	return iter;
    }
}
