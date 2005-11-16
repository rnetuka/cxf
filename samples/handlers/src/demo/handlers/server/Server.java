package demo.handlers.server;


import javax.xml.ws.Endpoint;

public class Server {

    protected Server() throws Exception {
        System.out.println("Starting AddNumbers Server");

        Object implementor = new AddNumbersImpl();
        String address = "http://localhost:9000/handlers/AddNumbersService/AddNumbersPort";
        Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) throws Exception {
        Server server = new Server();
        System.out.println("Server ready..."); 
        
        Thread.sleep(5 * 60 * 1000); 
        System.out.println("Server exitting");
        System.exit(0);
    }
}