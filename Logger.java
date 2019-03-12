import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class Logger {

	java.util.logging.Logger logger;

	public Logger(String name) {
		logger = java.util.logging.Logger.getLogger(name);
		logger.setLevel(Level.INFO);
		logger.addHandler(new ConsoleHandler());
	}

	@SuppressWarnings("rawtypes")
	public static Logger getLogger(Class cl) {
		return new Logger(cl.getName());
	}

	public void error(String msg, Throwable thrown) {
		logger.log(Level.SEVERE, msg, thrown);
	}

	public void info(String msg) {
		logger.info(msg);
	}
}