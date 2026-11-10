package com.bfsi.payment;

public final class Enums {
    private Enums() {}
    public enum PaymentMode { NEFT, UPI, CARD }
    /** PENDING = outcome unknown (e.g. core banking timed out) and needs reconciliation. */
    public enum PaymentStatus { PENDING, SUCCESS, FAILED }
}
