/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
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
