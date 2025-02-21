package ltm.spsa_tracker.backend;

import java.lang.reflect.Field;
import java.util.Arrays;

public class InstanceList {

    private static final String LIZARD = "somelizard.pythonanywhere.com";
    private static final String SWE = "chess.swehosting.se";
    private static final String FURY = "furybench.com";
    private static final String ZZZ = "zzzzz151.pythonanywhere.com";
    private static final String N9X = "chess.n9x.co";
    private static final String GRANT = "chess.grantnet.us";
    private static final String LYNX = "openbench.lynx-chess.com";
    private static final String PYRO = "pyronomy.pythonanywhere.com";
    private static final String SIRIUS = "mcthouacbb.pythonanywhere.com";
    private static final String POTENTIAL = "programcidusunur.pythonanywhere.com";
    private static final String ANA = "analoghors.pythonanywhere.com";
    private static final String LEELA = "bench.plutie.ca";
    private static final String CALVIN = "kelseyde.pythonanywhere.com";

    public static String[] getInstanceList() {
	Field[] fields = InstanceList.class.getDeclaredFields();
	var stringFields = Arrays.stream(fields).filter(f -> f.getType().equals(String.class)).map(f -> {
	    try {
		return (String) f.get(null);
	    } catch (IllegalArgumentException | IllegalAccessException e) {
		e.printStackTrace();
	    }
	    return "";
	});
	
	return stringFields.toArray(String[]::new);
    }
}
