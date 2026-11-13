package exceptions;

@SuppressWarnings("serial")
public class MovementException extends GameActionException {
    public MovementException() {

    };

    public MovementException(String s) {
        super(s);
    }
}
