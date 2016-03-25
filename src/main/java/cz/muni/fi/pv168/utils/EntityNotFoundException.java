package cz.muni.fi.pv168.utils;

/**
 * This exception is thrown when delete or update operation is performed 
 * with entity which does not exist in the database
 * 
 * @author Miroslav Kubus
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * Creates a new instance of <code>EntityNotFoundException</code> without
     * detail message.
     */
    public EntityNotFoundException() {
    }

    /**
     * Constructs an instance of <code>EntityNotFoundException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public EntityNotFoundException(String msg) {
        super(msg);
    }
}
