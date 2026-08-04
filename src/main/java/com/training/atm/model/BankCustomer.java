package com.training.atm.model;

public class BankCustomer {
    private String customerId;
    private String customerName;
    private String address;
    private String email;
    private String cardId;
    private String accountNumber;

    public BankCustomer(String customerId, String customerName, String address,
                        String email, String cardId, String accountNumber) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.address = address;
        this.email = email;
        this.cardId = cardId;
        this.accountNumber = accountNumber;
    }

    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getAddress() { return address; }
    public String getEmail() { return email; }
    public String getCardId() { return cardId; }
    public String getAccountNumber() { return accountNumber; }
}
