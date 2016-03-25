package cz.muni.fi.pv168.utils;

/**
 * This exception indicates service failure
 * @author Miroslav Kubus
 */
public class ServiceFailureException extends RuntimeException {

    /**
     * Creates a new instance of <code>ServiceFailureException</code> without
     * detail message.
     */
    public ServiceFailureException() {
    }

    /**
     * Constructs an instance of <code>ServiceFailureException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public ServiceFailureException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>ServiceFailureException</code> with the
     * specified throwable which cause ServiceFailureException
     *
     * @param cause the reason why ServiceFailureException is thrown
     */
    public ServiceFailureException(Throwable cause) {
        super(cause);
    }
    
     /**
     * Constructs an instance of <code>ServiceFailureException</code> with the
     * specified throwable which cause ServiceFailureException and specified 
     * detail message
     *
     * @param msg the detail message.
     * @param cause the reason why ServiceFailureException is thrown
     */
    public ServiceFailureException(String msg, Throwable cause) {
        super(msg,cause);
    }
}