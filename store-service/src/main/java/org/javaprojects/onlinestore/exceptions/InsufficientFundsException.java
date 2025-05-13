package org.javaprojects.onlinestore.exceptions;

import java.io.Serial;

public class InsufficientFundsException extends RuntimeException
{
    @Serial
    private static final long serialVersionUID = 1L;
    private final float balance;
    public InsufficientFundsException(String insufficientBalance, float balance)
    {
        super(insufficientBalance);
        this.balance = balance;
    }

    public float getBalance()
    {
        return balance;
    }
}
