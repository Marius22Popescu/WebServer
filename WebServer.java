import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class WebServer 
{
    // this is the port the web server listens on
    private static final int PORT_NUMBER = 8080;

    // main entry point for the application
    public static void main(String args[]) 
    {
        try 
        {
            // open socket
            ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);

            // start listener thread
            Thread listener = new Thread(new SocketListener(serverSocket));
            listener.start();

            // message explaining how to connect
            System.out.println("To connect to this server via a web browser, try \"http://127.0.0.1:8080/{url to retrieve}\"");

            // wait until finished
            System.out.println("Press enter to shutdown the web server...");
            Console cons = System.console(); 
            String enterString = cons.readLine();

            // kill listener thread
            listener.interrupt();

            // close the socket
            serverSocket.close();
        } 
        catch (Exception e) 
        {
            System.err.println("WebServer::main - " + e.toString());
        }
    }
}

class SocketListener implements Runnable 
{
    private ServerSocket serverSocket;

    public SocketListener(ServerSocket serverSocket)   
    {
        this.serverSocket = serverSocket;
    }

    // this thread listens for connections, launches a seperate socket connection
    //  thread to interact with them
    public void run() 
    {
        while(!this.serverSocket.isClosed())
        {
            try
            {
                Socket clientSocket = serverSocket.accept();
                Thread connection = new Thread(new SocketConnection(clientSocket));
                connection.start();
                Thread.yield();
            }
            catch(IOException e)
            {
                if (!this.serverSocket.isClosed())
                {
                    System.err.println("SocketListener::run - " + e.toString());
                }
            }
        }
    }
}

class SocketConnection implements Runnable 
{
    private final String HTTP_LINE_BREAK = "\r\n";

    private Socket clientSocket;

    public SocketConnection(Socket clientSocket)   
    {
        this.clientSocket = clientSocket;
    }

    // one of these threads is spawned and used to talk to each connection
    public void run() 
    {       
        try
        {
            BufferedReader request = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter response = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.handleConnection(request, response);
        }
        catch(IOException e)
        {
            System.err.println("SocketConnection::run - " + e.toString());
        }
    }

    private void handleConnection(BufferedReader request, PrintWriter response)
    {
        try
        {
        	// working code
            // code prints the web request
            String message = this.readHTTPHeader(request);
            System.out.println("Message:\r\n" + message);      

         // Read the data sent, stop reading once a blank line is hit, that blank line is the end of the client HTTP header 
  
        String[] urlAndHeaders = message.split("\n");   //Split the client's message by new line
    		String[] words = urlAndHeaders[0].split(" ");   //Split the client's message by space
    		//System.out.println(Arrays.toString(words));          //just for debugging, shows the message sent back
    		//System.out.println("words1: '" + words[1] + "'");    //just for debugging, shows the message sent back
    		
    		// handling the GET request
    		if(message.startsWith("GET"))
    		{
    		// Send the response
    		// The header
    		response.println("HTTP/1.1 200 OK\r");
    		response.println("Date: Mon, 27 Jul 2009 12:28:53 GMT\r");
    		response.println("Server: Apache/2.2.14 (Win32)\r");
    		response.println("Last modified: Wed, 22 Jul 2009 19:15:56 GMT\r");
    		response.println("Content-Lenght: 88\r"); 
    		response.println("Content-Type: text/html\r");  
    		response.println("Connection: closed\r");
    		response.println("\r");  // this blank line signals the end of the header
    		
    		//the path for web root need to be CHANGED below!!!
    		File requestFile = new File("/Users/marius/Documents/webroot" + words[1]); // create the file to keep the web root
    		if (requestFile.exists()) {    //if requested file exists
    		    if (requestFile.isDirectory()) {    //if requested file is directory
    		    		List<File> files = Arrays.asList(requestFile.listFiles(new FilenameFilter() {
    		    		    @Override
    		    		    public boolean accept(File dir, String name) {
    		    		        return name.matches("index.html");
    		    		    }
    		    		}));
    		    		if (!files.isEmpty()) {   // if file is not empty write the file to the output stream
    		    			writeFileToOutputStream(files.get(0), response);
    		    		}
    		    	else {     // if file is empty build the HTML directory 
    		    			String directoryListHtml = buildDirectoryListHtml(requestFile);
    		    			response.write(directoryListHtml);
    		    		}
    		    } else {
    		    		writeFileToOutputStream(requestFile, response);
    		    }
    		} else {  // if file is not found return 404 not found
    			
    			response.println("HTTP/1.1 400 Not Found");
    			/*
    			response.println("Date: Sun, 18 Oct 2012 10:36:20 GMT");
    			response.println("Server: Apache/2.2.14 (Win32)");
    			response.println("Content-Length: 230");
    			response.println("Connection: Closed");		
    			response.println("Content-Type: text/html; charset=iso-8859-1");
    			*/
    		}
    		
    		}
    		
    		// handling the HED request
    		else if (message.startsWith("HEAD"))
    		{
    			response.println("HTTP/1.1 200 OK\r");
        		response.println("Date: Mon, 27 Jul 2009 12:28:53 GMT\r");
        		response.println("Server: Apache/2.2.14 (Win32)\r");
        		response.println("Last modified: Wed, 22 Jul 2009 19:15:56 GMT\r");
        		response.println("Content-Lenght: 88\r"); 
        		response.println("Content-Type: text/html\r");  
        		response.println("Connection: closed\r");
        		response.println("\r");  // this blank line signals the end of the header
    		}
    		
    		// Send the HTML page
    		response.flush();
            // returns text hello world
            
            // close the socket, no keep alive
            this.clientSocket.close();
        }
        catch(IOException e)
        {
            System.err.println("SocketConnection::handleConnection: " + e.toString());
        }
    }
    
    //this method will build the directory
    private String buildDirectoryListHtml(File requestFile) {
		StringBuilder b = new StringBuilder();
		b.append("<html><head><title>Directory list</title></head><body>");
		for (File f : requestFile.listFiles()) {
			b.append("<a href=\"").append(requestFile.getName()).append("/").append(f.getName()).append("\">")
			.append(f.getName()).append("</a>").append("<br>");
		}
		b.append("</body></html>");
		return b.toString();
	}
    
    //this method will write the file to the output stream
	private void writeFileToOutputStream(File file, PrintWriter response) {
    	//read file into stream
    			try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {

    				stream.forEach(line -> response.write(line));
    				

    			} catch (IOException e) {
    				e.printStackTrace();
    			}
		
	}

	private String readHTTPHeader(BufferedReader reader)
    {
        String message = "";
        String line = "";
        while ((line != null) && (!line.equals(this.HTTP_LINE_BREAK)))
        {   
            line = this.readHTTPHeaderLine(reader);
            message += line;
        }
        return message;
    }

    private String readHTTPHeaderLine(BufferedReader reader)
    {
        String line = "";
        try 
        {
            line = reader.readLine() + this.HTTP_LINE_BREAK;
        }
        catch (IOException e) 
        {
            System.err.println("SocketConnection::readHTTPHeaderLine: " + e.toString());
            line = "";
        } 
        return line;
    }
}
