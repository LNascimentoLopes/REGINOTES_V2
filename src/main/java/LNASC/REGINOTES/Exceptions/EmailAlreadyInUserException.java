package LNASC.REGINOTES.Exceptions;

public class EmailAlreadyInUserException extends RuntimeException {
    public EmailAlreadyInUserException(String message) {
        super(message);
    }
}
