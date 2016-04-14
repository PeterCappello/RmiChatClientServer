package api;

import java.rmi.*;
import java.util.List;

/**
 *
 * @author Pete Cappello
 */
public interface Server extends Remote
{
    /**
     * The String used to identify the chat server in the RMIRegistrty
     */
    public static String SERVICE_NAME = "SimpleChatService";
    
    /**
     * The port used to access the RMIRegistry
     */
    public final static int PORT = 5051;

    /**
     * Login to the server.
     * @param client a remote reference to a Client object
     * @param name the Client's name (used in the chat session)
     * @throws RemoteException when remote invocation of this method fails.
     */
    public void login( Client client, String name ) throws RemoteException;

    /**
     * Logout from the server.
     * @param client a remote reference of the Client requesting to logout
     * @throws RemoteException when remote invocation of this method fails.
     */
    public void logout( Client client ) throws RemoteException;

    /**
     * Broadcasts message to all Clients
     * @param message the message to be broadcast
     * @throws RemoteException when remote invocation of this method fails.
     */
    public void update( Message message ) throws RemoteException;

    /**
     * List the names of all Clients currently logged in.
     * @return a list of these names
     * @throws RemoteException when remote invocation of this method fails.
     */
    public List<String> list() throws RemoteException;
}