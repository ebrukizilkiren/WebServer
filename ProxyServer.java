import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProxyServer extends Thread {
	private static Logger log = Logger.getLogger(ProxyServer.class);

    public static void main(String[] args) {
        (new ProxyServer()).run();
    }

    public ProxyServer() {
        super("Server Thread");
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            Socket socket;
            try {
                while ((socket = serverSocket.accept()) != null) {
                    (new Handler(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();  
            }
        } catch (IOException e) {
            e.printStackTrace();  
            return;
        }
    }

    public static class Handler extends Thread {
        public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])",
                                                                      Pattern.CASE_INSENSITIVE);
    	public static List<String> headers = new ArrayList<String>();
    	public static byte[] res;
    	

        private final Socket clientSocket;
        private boolean previousWasR = false;
        public static File cache;
        public static List<String> cachedSitesArray=new ArrayList<String>();

        public Handler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                String request = readLine(clientSocket);
                System.out.println(request);
                String outputHtml="";
                
                cache = new File("cache.txt");
                
                if(!cache.exists()) {
                	cache.createNewFile();
                }
                
                String line=null;
                
                FileReader fileReader = new FileReader(cache);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                
                while((line = bufferedReader.readLine()) != null) {
                    cachedSitesArray.add(line);
                }
                
                fileReader.close();
                bufferedReader.close();
                
                
                if(isCached(request, cachedSitesArray)) {
               		outputHtml=getFileName(request);
               		File file = new File(outputHtml);
               		fillHeaders(Status._200);
               		fillResponse(getBytes(file));
               		write(clientSocket.getOutputStream());
               		System.out.println("Cached block");
               		
                }else if(!request.contains("favicon")){
                    if(LengthChecker(request)) {
                    	BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(cache, true));
                    	bufferedWriter.write(request);
                    	bufferedWriter.write("\r\n");
                    	bufferedWriter.close();
                    	
                    	HttpRequest req = new HttpRequest(request);
                        HttpResponse response = new HttpResponse(req); 
                        Matcher matcher = CONNECT_PATTERN.matcher(request);
                        
                        if (!matcher.matches()){
                        
                        	response.write(clientSocket.getOutputStream());
                        	
                        } else{
                            String header;
                            do {
                                header = readLine(clientSocket);
                            } while (!"".equals(header));
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                                                                                           "ISO-8859-1");
              //             outputStreamWriter.write(response.body.toString());
                            final Socket forwardSocket;
                            try {
                                forwardSocket = new Socket(matcher.group(1), Integer.parseInt(matcher.group(2)));
                                System.out.println(forwardSocket);
                            } catch (IOException | NumberFormatException e) {
                                e.printStackTrace();  
                                outputStreamWriter.write("HTTP/" + matcher.group(3) + " 502 Bad Gateway\r\n");
                                outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                                outputStreamWriter.write("\r\n");
                                outputStreamWriter.flush();
                                return;
                            }
                            try {
                                outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
                                outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                                outputStreamWriter.write("\r\n");
                                outputStreamWriter.flush();

                                Thread remoteToClient = new Thread() {
                                    @Override
                                    public void run() {
                                        forwardData(forwardSocket, clientSocket);
                                    }
                                };
                                remoteToClient.start();
                                try {
                                    if (previousWasR) {
                                        int read = clientSocket.getInputStream().read();
                                        if (read != -1) {
                                            if (read != '\n') {
                                                forwardSocket.getOutputStream().write(read);
                                            }
                                            forwardData(clientSocket, forwardSocket);
                                        } else {
                                            if (!forwardSocket.isOutputShutdown()) {
                                                forwardSocket.shutdownOutput();
                                            }
                                            if (!clientSocket.isInputShutdown()) {
                                                clientSocket.shutdownInput();
                                            }
                                        }
                                    } else {
                                        forwardData(clientSocket, forwardSocket);
                                    }
                                } finally {
                                    try {
                                        remoteToClient.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();  
                                    }
                                }
                            } finally {
                                forwardSocket.close();
                            }
                        }
                     
                    }else {
                    	fillHeaders(Status._414);
                    	fillResponse(Status._414.toString());
                        write(clientSocket.getOutputStream());

    				}	
                }
                
                
                

                
                
                
            } catch (IOException e) {
                e.printStackTrace();  
            } finally {
            	
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();  
                }
            }
        }

        private static void forwardData(Socket inputSocket, Socket outputSocket) {
            try {
                InputStream inputStream = inputSocket.getInputStream();
                try {
                    OutputStream outputStream = outputSocket.getOutputStream();
                    try {
                        byte[] buffer = new byte[4096];
                        int read;
                        do {
                            read = inputStream.read(buffer);
                            if (read > 0) {
                                outputStream.write(buffer, 0, read);
                                if (inputStream.available() < 1) {
                                    outputStream.flush();
                                }
                            }
                        } while (read >= 0);
                    } finally {
                        if (!outputSocket.isOutputShutdown()) {
                            outputSocket.shutdownOutput();
                        }
                    }
                } finally {
                    if (!inputSocket.isInputShutdown()) {
                        inputSocket.shutdownInput();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  
            }
        }

        private String readLine(Socket socket) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int next;
            readerLoop:
            while ((next = socket.getInputStream().read()) != -1) {
                if (previousWasR && next == '\n') {
                    previousWasR = false;
                    continue;
                }
                previousWasR = false;
                switch (next) {
                    case '\r':
                        previousWasR = true;
                        break readerLoop;
                    case '\n':
                        break readerLoop;
                    default:
                        byteArrayOutputStream.write(next);
                        break;
                }
            }
            return byteArrayOutputStream.toString("ISO-8859-1");
        }
        public static boolean isCached(String url, List<String> cacheList) {
        	boolean iscached = false;
        	
        	for(String str: cacheList) {
        		if(url.equals(str)) {
        			iscached = true;
        		}
        	}
        	
        	return iscached;
        }
        
        public static boolean LengthChecker(String str) {
    		log.info(str);
    		String[] split = str.split("\\s+");
    		String[] secSplit;
    		int fsize;
    		
    		if(!split[1].contains("favicon.ico")) {
    			secSplit = split[1].split(" ");
    			secSplit[0] = secSplit[0].replace("/" , "");
    			fsize = Integer.parseInt(secSplit[0]);
    		}else {
    			fsize = 0;
    		}

    		if(fsize>9999) {
    			return false;
    		}else {
    			return true;
    		}
    		 
    	}
        
        public static String getFileName(String str) {
    		log.info("Cached file " + str);
    		String[] split = str.split("\\s+");
    		String[] secSplit;
    		int fsize;
    		
    		if(!split[1].contains("favicon.ico")) {
    			secSplit = split[1].split(" ");
    			secSplit[0] = secSplit[0].replace("/" , "");
    			fsize = Integer.parseInt(secSplit[0]);
    		}else {
    			fsize = 0;
    		}
    		
    		String fname = fsize + ".html";
    		return fname;
    	}
        
    	public static void write(OutputStream os) throws IOException {
    		DataOutputStream output = new DataOutputStream(os);
    		for (String header : headers) {
    			output.writeBytes(header + "\r\n");
    		}
    		output.writeBytes("\r\n");
    		if (res != null) {
    			output.write(res);
    		}
    		output.writeBytes("\r\n");
    		output.flush();
    	}
    	
    	public static void fillHeaders(Status status) {
    		headers.add(HttpResponse.VERSION + " " + status.toString());
    		headers.add("Connection: close");
    		headers.add("Server: HTTPServer");
    	}

    	public static void fillResponse(String response) {
    		res = response.getBytes();

    	}
    	
    	public static void fillResponse(byte[] response) {
    		res = response;
    	}
    	
    	public static byte[] getBytes(File file) throws IOException {
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
    	
    }
    

	

}