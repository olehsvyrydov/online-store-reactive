package org.javaprojects.payment.services;

import org.javaprojects.payment.configuration.ApplicationProperties;
import org.javaprojects.payment.exceptions.LowBalanceException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService
{
    private float balance;

    public PaymentService(ApplicationProperties applicationProperties)
    {
        this.balance = applicationProperties.initialBalance();
    }

    public float processPayment(Float amount)
    {
        if (amount == null || amount <= 0)
        {
            throw new IllegalArgumentException("Invalid payment amount");
        }
        if (amount > balance)
        {
            throw new LowBalanceException("Insufficient balance", balance);
        }

        balance = balance - amount;
        return balance;
    }

    public float getBalance()
    {
        return balance;
    }
}
