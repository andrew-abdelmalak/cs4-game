package exceptions;

@SuppressWarnings("serial")
public class NoAvailableResourcesException extends GameActionException {
    public NoAvailableResourcesException() {

    }

    public NoAvailableResourcesException(String s) {
        super(s);
    }

}
