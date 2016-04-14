package client;

import api.*;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Chat Client application
 * @author Peter Cappello
 */
public final class ClientApp extends UnicastRemoteObject implements Client
{
    private static final Color LIGHT_BLUE  = new Color( 233, 233, 255 );
    private static final Color LIGHT_GREEN = new Color( 233, 255, 233 );

    private static final int DISCONNECTED = 0;
    private static final int CONNECTED    = 1;
    private static final int LOGGEDIN     = 2;

    // declare view components
    private final JFrame frame = new JFrame( "Chat Client" );
    
    private final JPanel northPanel = new JPanel();
        private final JButton connectButton = new JButton( "Connect" );
        private final JTextField connectTextField = new JTextField( "localhost", 20 );
        private final JLabel padLabel = new JLabel("  ");
        private final JTextField loginTextField = new JTextField( 10 );
        private final JButton loginButton  = new JButton( "Login" );

    private final JPanel centerPanel = new JPanel();
        private final JPanel chatPanel = new JPanel();
            private final JLabel chatLabel = new JLabel( "Chat Record" );
            private final JTextArea  chatTextArea   = new JTextArea( 100, 40 );
        private final JPanel messagePanel = new JPanel();
            private final JLabel messageLabel = new JLabel( "Message Area" );
            private final JTextArea messageTextArea = new JTextArea( 3, 40 );

    private final JPanel southPanel = new JPanel();
        private final JButton clearButton = new JButton( "Clear Chat Record" );
        private final JButton listButton  = new JButton( "List Current Chatters" );
        private final JButton sendButton  = new JButton( "Send Message" );

    // Client attributes
    private String myClientName;
    private Server server;
    private ServerProxy serverProxy;

    private int userState;

    public ClientApp() throws RemoteException { initComponents(); }

    private void initComponents()
    {
        // layout view
        frame.setSize( 500, 600 );
        frame.setLayout( new BorderLayout() );
        
        northPanel.setLayout( new GridLayout( 1, 5 ) );
        northPanel.add( connectButton );
        northPanel.add( connectTextField );
        northPanel.add( padLabel );
        northPanel.add( loginTextField );
        northPanel.add( loginButton );
        frame.add( northPanel, BorderLayout.NORTH );

        centerPanel.setLayout( new BorderLayout() );
            chatPanel.setLayout( new BorderLayout() );
            chatPanel.add( chatLabel, BorderLayout.NORTH );
            chatPanel.add( chatTextArea, BorderLayout.CENTER );
        centerPanel.add( chatPanel, BorderLayout.CENTER );
            messagePanel.setLayout( new BorderLayout() );
            messagePanel.add( messageLabel, BorderLayout.NORTH );
            messagePanel.add( messageTextArea, BorderLayout.CENTER );
        centerPanel.add( messagePanel, BorderLayout.SOUTH );
        frame.add( centerPanel, BorderLayout.CENTER );

        southPanel.setLayout( new GridLayout( 1, 3 ) );
        southPanel.add( clearButton );
        southPanel.add( listButton );
        southPanel.add( sendButton );
        frame.add( southPanel, BorderLayout.SOUTH );
        
        chatLabel.setForeground( Color.BLUE );
        chatLabel.setOpaque( true );
        chatLabel.setBackground( LIGHT_BLUE );
        chatTextArea.setBackground( LIGHT_BLUE );
        chatPanel.setBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ) );
        messageLabel.setForeground( Color.GREEN );
        messageLabel.setOpaque( true );
        messageLabel.setBackground( LIGHT_GREEN );
        messageTextArea.setBackground( LIGHT_GREEN );
        messagePanel.setBorder( BorderFactory.createEtchedBorder( EtchedBorder.LOWERED ) );
        
        // initial user state == disconnected
        goFromLoggedinToConnected();
        goFromConnectedToDisconnected();

        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        //  _______________________________________
        //  contoller TEMPLATE CODE for each action
        //  _______________________________________

        connectButton.addActionListener(this::connectButtonActionPerformed);

        loginButton.addActionListener(this::loginButtonActionPerformed);

        clearButton.addActionListener(this::clearButtonActionPerformed);

        listButton.addActionListener(this::listButtonActionPerformed);
        
        sendButton.addActionListener(this::sendButtonActionPerformed);

        System.setSecurityManager( new SecurityManager() );
    }

    //  _________________________
    //  contoller for each action
    //  _________________________
    private void connectButtonActionPerformed( ActionEvent actionEvent )
    {
        if ( connectButton.getText().equals( "Connect" ) )
        {
            if ( connectTextField.getText().length() == 0 )
            {
                return; // Nothing to connect to.
            }
            goFromDisconnectedToConnected();
        }
        else
        {
            assert connectButton.getText().equals( "Disconnect" );
            goToDisconnected();
        }
        connectTextField.setEditable( true );
    }

    private void loginButtonActionPerformed( ActionEvent actionEvent )
    {
        if ( loginButton.getText().equals( "Login" ) )
        {
            goFromConnectedToLoggedin();
        }
        else // logout
        {
            assert loginButton.getText().equals( "Logout" );
            goFromLoggedinToConnected();
        }
    }

    private void clearButtonActionPerformed( ActionEvent actionEvent )
    {
        chatTextArea.setText("");
        messageTextArea.requestFocus();
    }

    private void listButtonActionPerformed( ActionEvent actionEvent )
    {
        chatTextArea.append("SYSTEM: Current chatters:\n");
        serverProxy.list().stream().forEach(clientName -> {
            chatTextArea.append("   Name: " + clientName + "\n");
        });
        messageTextArea.requestFocus();
    }

    private void sendButtonActionPerformed( ActionEvent actionEvent )
    {
        String input = messageTextArea.getText().trim();
        if ( input.equals( "" ) ) // Is there a message?
        {
            return;               // No.
        }
        serverProxy.update( new Message( myClientName, input ));
        messageTextArea.setText( "" );
        messageTextArea.requestFocus();
    }

    /*
     * User has 3 states: DISCONNECTED, CONNECTED, LOGGED_IN
     * The initial state is DISCONNECTED.
     * Each user state transition has a corresponding method
     */
    private void goFromDisconnectedToConnected()
    {
        userState = CONNECTED;
        connectButton.setText( "Disconnect" );
        enableConnect( false );
        String domainName = connectTextField.getText().trim();
        connect( domainName );

        loginTextField.requestFocus();
    }

    private void goFromConnectedToDisconnected()
    {
        userState = DISCONNECTED;
        server = null; // disconnect
        connectButton.setText( "Connect" );
        enableConnect( true );

        connectTextField.requestFocus();
    }

    private void goFromConnectedToLoggedin()
    {
        userState = LOGGEDIN;
        myClientName = loginTextField.getText().trim();
        if ( myClientName.equals("") )
        {
            return;
        }
        loginButton.setText( "Logout" );
        enableMessageActions( true );
        login( true );
        
        messageTextArea.requestFocus();
    }

    private void goFromLoggedinToConnected()
    {
        userState = CONNECTED;
        login( false );
        myClientName = null;
        enableMessageActions( false );
        loginButton.setText( "Login" );
        enableConnect( false );

        // clear chat session
        chatTextArea.setText("");
        messageTextArea.setText("");
        
        loginTextField.requestFocus();
    }

    private void goToDisconnected()
    {
        if ( userState == LOGGEDIN )
        {
            goFromLoggedinToConnected();
        }
        assert userState == CONNECTED;
        goFromConnectedToDisconnected();
    }

    private void enableConnect( boolean isEnabled )
    {
        connectButton.setEnabled( true );
        connectTextField.setEnabled( isEnabled );
        loginButton.setEnabled( ! isEnabled );
        loginTextField.setEnabled( ! isEnabled );
    }

    private void enableMessageActions( boolean isEnabled )
    {
        loginTextField.setEnabled( ! isEnabled );
        messageTextArea.setEnabled( isEnabled );
        clearButton.setEnabled( isEnabled );
        listButton.setEnabled( isEnabled );
        sendButton.setEnabled( isEnabled );
    }

    private JFrame getFrame() { return frame; }

    private void connect( String serverMachineDomainName )
    {
        String url = "//" + serverMachineDomainName + ":" + Server.PORT + "/" + Server.SERVICE_NAME;
        try
        {
            server = (Server) Naming.lookup( url );
        }
        catch ( NotBoundException exception )
        {
            update( new Message( "SYSTEM", "No server is registered on this machine.") );
        }
        catch ( RemoteException exception )
        {
            update( new Message( "SYSTEM", "Server is not responding." ) );
        }
        catch ( MalformedURLException exception )
        {
            update( new Message( "SYSTEM", "Cannot proceed. Unexpected exception: \n" + exception ) );
        }
    }

    private void login( boolean isLogin )
    {
        if ( isLogin )
        {
            serverProxy = new ServerProxy( server, this, myClientName );
            serverProxy.start();
            serverProxy.login( this, myClientName );
        }
        else // logout
        {
            if ( serverProxy == null )
            {
                return;
            }
            serverProxy.logout( this );
            serverProxy = null; // ignore subsequent server requests
        }
    }

    /**
     * Append message to the view of the chat
     * @param message the message to be appended
     */
    @Override
    public synchronized void update( Message message )
    {
        chatTextArea.append( message.getName() + ": " + message.getMessage() + "\n" );
    }

    /**
     * Get the name this client uses in this chat session
     * @return client name
     */
    public String getClientName() { return myClientName; }


    /**
     * Used to instantiate a client
     * @param args is unused
     */
    public static void main( String[] args )
    {
        EventQueue.invokeLater(() -> {
            try
            {
                new ClientApp().getFrame().setVisible( true );
            }
            catch ( RemoteException ignore ) {}
        });
    }

    /**
     * Only to support unit testing
     * @return 
     */
    public JTextArea getChatTextArea() { return chatTextArea; }

    public void setClientName( String clientName ) { this.myClientName = clientName; }

    /**
     * The Thread that invokes Remote methods on the server.
     */
    private class ServerProxy extends Thread implements Server
    {
        private final Message ERROR_MESSAGE = new Message( "SYSTEM", "Server is not responding." );

        private final Server server;
        private final Client client;
        private final BlockingQueue<Message> q = new LinkedBlockingQueue<>();

        public ServerProxy( Server server, Client client, String name )
        {
            this.server = server;
            this.client = client;
            q.add( new Message( "SYSTEM", "Connected " + name ) );
//            start();
        }

        @Override
        public void login( Client client, String name )
        {
            assert client != null;
            assert name != null;
            try
            {
                server.login( client, name );
            }
            catch ( RemoteException exception )
            {
                disconnect();
            }
        }

        @Override
        public void logout( Client client )
        {
            assert client != null;

            try
            {
                server.logout( client );
            }
            catch ( RemoteException exception )
            {
                disconnect();
            }
        }

        @Override
        public void update( Message message )
        {
            assert message != null;
            q.add( message );
        }

        @Override
        public java.util.List<String> list()
        {
            try
            {
                return server.list();
            }
            catch ( RemoteException exception )
            {
                disconnect();
            }
            return null;
        }

        @Override
        public void run()
        {
            while ( true )
            {
                try
                {
                    server.update( (Message) q.take() );
                }
                catch ( RemoteException exception )
                {
                    disconnect();
                    return;
                }
                catch ( InterruptedException ignore ) {}
            }
        }

        private void disconnect()
        {
            try
            {
                client.update( ERROR_MESSAGE );
            }
            catch ( RemoteException ignore ) {} // impossible: local method
        }
    }
}
