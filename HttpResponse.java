import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import javax.naming.spi.DirStateFactory.Result;

public class HttpResponse {

	private static Logger log = Logger.getLogger(HttpResponse.class);

	public static final String VERSION = "HTTP/1.0";

	List<String> headers = new ArrayList<String>();

	byte[] body;

	public HttpResponse(HttpRequest req) throws IOException {

		switch (req.method) {
			case HEAD:
				fillHeaders(Status._200);
				break;
			case GET:
				try {
					if(req.uri.contains("favicon")||req.fsize==0) {
						fillHeaders(Status._400);
						fillResponse(Status._400.toString());
						break;
					}
					
					String fileName = req.uri.replace("/", "");
					
					File file = new File(fileName + ".html");

					if(req.fsize==0) {
						log.info("URI contains non-integer value.");
						break;
					}
					
					if(req.fsize<100||req.fsize>20000) {
						if(req.fsize<100) {
							log.info("URI is less than 100. \n");
						}else {
							log.info("URI is greater than 20000. \n");
						}
						
						fillHeaders(Status._400);
						fillResponse(Status._400.toString());
						break;
					}
					
					createHTMLfile(req.fsize , req.uri);
					
					if (file.isDirectory()) {
					    fillHeaders(Status._200);
					    
						headers.add(ContentType.HTML.toString());
						StringBuilder result = new StringBuilder("<html><head><title>Index of ");
						result.append(req.uri);
						result.append("</title></head><body><h1>Index of ");
						result.append(req.uri);
						result.append("</h1><hr><pre>");

						// TODO add Parent Directory
						File[] files = file.listFiles();
						for (File subfile : files) {
							result.append(" <a href=\"" + subfile.getPath() + "\">" + subfile.getPath() + "</a>\n");
						}
						result.append("<hr></pre></body></html>");
						fillResponse(result.toString());
					} else if (file.exists()) {
					    fillHeaders(Status._200);
						setContentType("HTML", headers);
						fillResponse(getBytes(file));
					} else {
						log.info("File not found:" + req.uri);
						fillHeaders(Status._404);
						fillResponse(Status._404.toString());
					}
				} catch (Exception e) {
					log.error("Response Error", e);
					fillHeaders(Status._400);
					fillResponse(Status._400.toString());
				}

				break;
			case UNRECOGNIZED:
				fillHeaders(Status._400);
				fillResponse(Status._400.toString());
				break;
			default:
				fillHeaders(Status._501);
				fillResponse(Status._501.toString());
		}

	}

	private byte[] getBytes(File file) throws IOException {
		int length = (int) file.length();
		byte[] array = new byte[length];
		InputStream in = new FileInputStream(file);
		int offset = 0;
		while (offset < length) {
			int count = in.read(array, offset, (length - offset));
			offset += count;
		}
		in.close();
		return array;
	}

	private void fillHeaders(Status status) {
		headers.add(HttpResponse.VERSION + " " + status.toString());
		headers.add("Connection: close");
		headers.add("Server: HTTPServer");
	}

	private void fillResponse(String response) {
		body = response.getBytes();
	}

	private void fillResponse(byte[] response) {
		body = response;
	}

	public void write(OutputStream os) throws IOException {
		DataOutputStream output = new DataOutputStream(os);
		for (String header : headers) {
			output.writeBytes(header + "\r\n");
		}
		output.writeBytes("\r\n");
		if (body != null) {
			output.write(body);
		}
		output.writeBytes("\r\n");
		output.flush();
	}

	private void setContentType(String uri, List <String> list) {
		try {
			String ext = uri.substring(uri.indexOf(".") + 1);
			list.add(ContentType.valueOf(ext.toUpperCase()).toString());
		} catch (Exception e) {
			log.error("ContentType not found: " + e, e);
		}
	}
	
	public void createFile(int size, String fname) throws IOException {
		
		BufferedWriter output = null;

		int num=0;
		try {
			fname=fname.replace("/", "");
            File file = new File(fname+".txt");
            output = new BufferedWriter(new FileWriter(file));
            output.write("I am " + size + " bytes long.\n");
            while(num<size) {
            	if(num%2==0) {
            		output.write("a");
            	}else {
            		output.write('\u00A0');
            	}
            	num++;
            }
 
        } catch ( IOException e ) {
            e.printStackTrace();
        } finally {
          if ( output != null ) {
 //       	 output.flush();
            output.close();
          }
        }
	}
	
	public void createHTMLfile(int size, String fname) throws IOException {
		
		StringBuilder contentBuilder = new StringBuilder();
		File htmlTemplateFile = new File("template.html");
		File newHtmlFile;
		BufferedWriter output = null;
		BufferedReader reader = null;
		String htmlString = "";
		String line = "";
		String title = "";
		String body = "";
		int num=0;
		
		title = "I am " + size + " bytes long.";
		reader = new BufferedReader(new FileReader(htmlTemplateFile));
		fname=fname.replace("/", "");
		fname=fname.replaceAll("\\\\", "");	
		
		newHtmlFile = new File(fname + ".html");
		
		while ((line = reader.readLine()) != null) {
			contentBuilder.append(line);
		}
		reader.close();
		
		
		htmlString = contentBuilder.toString();
		
		contentBuilder.setLength(0);
		
        while(num<(size-74)) {
        	if(num%2==0) {
        		contentBuilder.append('a');
        	}else {
        		contentBuilder.append('\u00A0');
        	}
        	num++;
        }
        
        body = contentBuilder.toString();
		
		htmlString = htmlString.replace("$title", title);
		htmlString = htmlString.replace("$body", body);
	
		output = new BufferedWriter(new FileWriter(newHtmlFile));
		
		output.write(htmlString);
		output.close();
	}
	
}