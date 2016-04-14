package api;

import java.rmi.*;

/**
 *
 * @author Pete Cappello
 */
public interface Client extends Remote
{
    /**
     * Receive and display a Message from the Server.
     * @param message the Message sent by the Server.
     * @throws RemoteException when remote invocation of this method fails.
     */
    public void update( Message message ) throws RemoteException;
}


