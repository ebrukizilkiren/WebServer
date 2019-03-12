import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HttpRequest {

	private static Logger log = Logger.getLogger(HttpRequest.class);

	List<String> headers = new ArrayList<String>();

	Method method;

	String uri;

	String version;
	
	int fsize;
	
	public HttpRequest(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String str = reader.readLine();
		parseRequestLine(str);

		while (!str.equals("")) {
			str = reader.readLine();
			parseRequestHeader(str);
		}
	}
	
	public HttpRequest(String request) {
		parseRequestLine(request);
	}

	private void parseRequestLine(String str) {
		log.info(str);
		String[] split = str.split("\\s+");
		String[] secSplit;
		try {
			method = Method.valueOf(split[0]);
		} catch (Exception e) {
			method = Method.UNRECOGNIZED;
		}
		uri = split[1];

		secSplit = split[1].split(" ");
		secSplit[0] = secSplit[0].replace("/" , "");
		try {
			fsize = Integer.parseInt(secSplit[0]);
		} catch (NumberFormatException e) {
			fsize = 0;
		}
	
		version = split[2];
	}

	private void parseRequestHeader(String str) {
		log.info(str);
		headers.add(str);
	}
}