import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {

	private static Logger log = Logger.getLogger(WebServer.class);

	private static final int DEFAULT_PORT = 8080;

	private static final int N_THREADS = 3;

	public static int portSelection;
	
	public static void main(String args[]) {

		Scanner input = new Scanner(System.in);

		boolean isportvalid = false;
		
		while(!isportvalid) {
			System.out.println("Please enter the port number. \n");
			portSelection = input.nextInt();
			isportvalid = isValidPort(portSelection);

		}
		
		try {
			
			new WebServer().start(portSelection);
		} catch (Exception e) {
			log.error("Startup Error", e);
		}
	}

	public void start(int port) throws IOException {
		ServerSocket s = new ServerSocket(port);
		System.out.println("Web server listening on port " + port + " (press CTRL-C to quit)");
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		while (true) {
			executor.submit(new RequestHandler(s.accept()));

		}
	}

	/**
	 * Parse command line arguments (string[] args) for valid port number
	 * 
	 * @return int valid port number or default value (8080)
	 */
	static int getValidPortParam(String args[]) throws NumberFormatException {
		if (args.length > 0) {
			int port = Integer.parseInt(args[0]);
			if (port > 0 && port < 65535) {
				return port;
			} else {
				throw new NumberFormatException("Invalid port! Port value is a number between 0 and 65535");
			}
		}
		return DEFAULT_PORT;
	}
	
	public static boolean isValidPort(int port) {
		if (port > 0 && port < 65535) {
			return true;
		} else {
			System.out.println("Invalid port number. \n");
			return false;
		}
	}
}