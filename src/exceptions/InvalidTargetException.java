package exceptions;

@SuppressWarnings("serial")
public class InvalidTargetException extends GameActionException {
    public InvalidTargetException() {

    }

    public InvalidTargetException(String s) {
        super(s);
    }
}
