package org.mastodon.geff.imglib2;

// TODO: move to separate file
public class GeffException extends Exception {

    public GeffException(Throwable cause) {
        super(cause);
    }

    public GeffException(String message) {
        super(message);
    }

    public GeffException(String message, Throwable cause) {
        super(message, cause);
    }
}
