package api;

import java.io.*;

/**
 *
 * @author Pete Cappello
 */
final public class Message implements Serializable
{
    final private String name;
    final private String message;

    /**
     * Immutable.
     * Has the name of the Client originating the message and its content.
     * @param name by which a client is known in a chat session
     * @param message the message the client is sending to the group
     */
    public Message( String name, String message )
    {
        assert name != null;
        assert message != null;

        this.name = name;
        this.message = message;
    }

    /**
     *  Get Name
     * @return name
     */
    public String getName() { return name; }

    /**
     * Get Message
     * @return message
     */
    public String getMessage() { return message; }

    @Override
    public String toString()
    {
        StringBuffer string = new StringBuffer();
        string.append( "Name: " );
        string.append( name );
        string.append( " Message: ");
        string.append( message );
        return new String( string );
    }
}