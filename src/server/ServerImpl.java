package server;

import api.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Peter Cappello
 */
public final class ServerImpl extends UnicastRemoteObject implements Server
{
    private final Map<Client, ClientProxy> clientProxies = Collections.synchronizedMap( new HashMap<Client,ClientProxy>() );

    /**
     * Chat Server
     * @throws RemoteException when unable to open sockets to listen for
     * remotely invoked methods
     */
    ServerImpl() throws RemoteException {}

    /**
     * Used to instantiate a chat Server
     * @param args unused
     * @throws Exception if, for any reason, construction or registration fail
     */
    public static void main(String args[]) throws Exception
    {
        System.setSecurityManager( new SecurityManager() );
        Server server = new ServerImpl();
        Registry registry = LocateRegistry.createRegistry( Server.PORT );
        registry.bind( Server.SERVICE_NAME, server);
        System.out.println("Server ready.");
    }

    @Override
    public void login( Client client, String name )
    {
        assert client != null;
        assert name   != null;
        
        ClientProxy clientProxy = new ClientProxy( client, name, this );
        clientProxy.start();
        clientProxies.put( client, clientProxy );
        update( new Message( name, "Signed on.") );

        assert clientProxies.get( client ) == clientProxy; // postcondition
    }

    @Override
    public void logout( Client client )
    {
        assert client != null;

        ClientProxy clientProxy = clientProxies.remove( client );
        if ( null != clientProxy )
        {
            update( new Message( clientProxy.getClientName(), "Signed off.") );
        }

        assert clientProxies.get( client ) == null; // postcondition
    }

    @Override
    public List<String> list()
    {
        List<String> clientNameList = new LinkedList<>();

        Collection<ClientProxy> clientProxyCollection = clientProxies.values();
        for ( ClientProxy clientProxy : clientProxyCollection )
        {
            clientNameList.add( clientProxy.getClientName() );
        }
        return clientNameList;
    }

    @Override
    synchronized public void update( Message message )
    {
        assert message != null;

        for ( Client clientProxy : clientProxies.values() )
        {
            try
            {
                clientProxy.update( message );
            }
            catch ( RemoteException ignore ) {} // handled by clientProxy
        }
    }

    /**
     * Only for unit testing.
     */
    Map<Client, ClientProxy> getClientProxies() { return clientProxies; }


    /**
     * The Thread that invokes Remote methods on the Client - one per Client
     */
    private class ClientProxy extends Thread implements Client
    {
        private final Client client;
        private final String clientName;
        private final Server server;
        private final BlockingQueue<Message> q;

        /**
         *
         * @param client the client for which this is a remote proxy
         * @param clientName cached value of client.getName()
         * @param server the chat server that has a reference to this
         */
        public ClientProxy( Client client, String clientName, Server server )
        {
            this.q = new LinkedBlockingQueue<>();
            assert client != null;
            assert clientName != null;
            assert server != null;

            this.client = client;
            this.clientName = clientName;
            this.server = server;
        }

        @Override
        public void update( Message message )
        {
            assert message != null;
            q.add( message );
        }

        /**
         * When its queue of messages is nonempty, it updates
         * its corresponding Remote client with the message.
         */
        @Override
        public void run()
        {
            while ( true )
            {
                try
                {
                    client.update( q.take() );
                }
                catch (RemoteException exception)
                {
                    try
                    {
                        server.logout( this );
                        return; // let this thread die
                    }
                    catch ( RemoteException ignore ) {} // local method
                }
                catch ( InterruptedException ignore ) {}
            }
        }

        public String getClientName() { return clientName; }
    }
}
