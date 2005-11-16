package demo.hwRPCLit.server;

import javax.xml.ws.Endpoint;

public class Server {

    protected Server(String[] args) throws Exception {
        System.out.println("Starting Server");
        Object implementor = new GreeterRPCLitImpl();
        String address = "http://localhost:9000/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) throws Exception {
        Server server = new Server(args);
        System.out.println("Server ready...");
        
        Thread.sleep(5 * 60 * 1000); 
        System.out.println("Server exitting");
        System.exit(0);
    }
}
