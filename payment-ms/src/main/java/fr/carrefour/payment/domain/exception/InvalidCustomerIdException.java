package fr.carrefour.payment.domain.exception;

public class InvalidCustomerIdException extends RuntimeException {

    public InvalidCustomerIdException() {
        super("Customer id must not be blank");
    }
}