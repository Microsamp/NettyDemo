package de.microsamp.example.netty.exception;

public class NotConnectedException extends RuntimeException {

	@Override
	public String getMessage() {
		return "Not connected yet!";
	}

}
