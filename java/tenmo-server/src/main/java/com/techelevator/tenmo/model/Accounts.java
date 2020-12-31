package com.techelevator.tenmo.model;

public class Accounts {
	
	private Long accountId;
	private Long userId;
	private double balance;
	
	public Accounts(Long accountId2, Long userId2, double balance) {
		this.accountId = accountId2;
		this.userId = userId2;
		this.balance = balance;
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}
}
