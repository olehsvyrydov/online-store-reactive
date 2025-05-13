package org.javaprojects.payment.exceptions;

import java.io.Serial;

public class LowBalanceException extends RuntimeException
{
    @Serial
    private static final long serialVersionUID = 1L;
    private final float balance;
    public LowBalanceException(String insufficientBalance, float balance)
    {
        super(insufficientBalance);
        this.balance = balance;
    }

    public float getBalance()
    {
        return balance;
    }
}
