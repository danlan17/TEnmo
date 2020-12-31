package com.techelevator.tenmo.models;

public class Transfers {
	
	private Long transferId;
	private Long typeId;
	private String typeDesc;
	private Long statusId;
	private String statusDesc;
	private Long primaryId;
	private String primaryUsername;
	private Long secondaryId;
	private String secondaryUsername;
	private Long primaryAccount;
	private Long secondaryAccount;
	private double amount;
	
	public Transfers() {
	}
	
	public Transfers(Long typeId, Long statusId, Long primaryId, String primaryUsername, 
			Long secondaryId, String secondaryUsername, double amount) {
		
		this.typeId = typeId;
		this.statusId = statusId;
		this.primaryId = primaryId;
		this.primaryUsername = primaryUsername;
		this.secondaryId = secondaryId;
		this.secondaryUsername = secondaryUsername;
		this.amount = amount;
	}
	
	public Transfers(Long transferId, Long typeId, Long statusId, Long primaryAccount, 
			Long secondaryAccount, double amount) {
		
		this.transferId = transferId;
		this.typeId = typeId;
		this.statusId = statusId;
		this.primaryAccount = primaryAccount;
		this.secondaryAccount = secondaryAccount;
		this.amount = amount;
	}

	public Long getTransferId() {
		return transferId;
	}

	public void setTransferId(Long transferId) {
		this.transferId = transferId;
	}

	public Long getTypeId() {
		return typeId;
	}

	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	public String getTypeDesc() {
		return typeDesc;
	}

	public void setTypeDesc(String typeDesc) {
		this.typeDesc = typeDesc;
	}

	public Long getStatusId() {
		return statusId;
	}

	public void setStatusId(Long statusId) {
		this.statusId = statusId;
	}

	public String getStatusDesc() {
		return statusDesc;
	}

	public void setStatusDesc(String statusDesc) {
		this.statusDesc = statusDesc;
	}

	public Long getPrimaryId() {
		return primaryId;
	}

	public void setPrimaryId(Long primaryId) {
		this.primaryId = primaryId;
	}

	public String getPrimaryUsername() {
		return primaryUsername;
	}

	public void setPrimaryUsername(String primaryUsername) {
		this.primaryUsername = primaryUsername;
	}

	public Long getSecondaryId() {
		return secondaryId;
	}

	public void setSecondaryId(Long secondaryId) {
		this.secondaryId = secondaryId;
	}

	public String getSecondaryUsername() {
		return secondaryUsername;
	}

	public void setSecondaryUsername(String secondaryUsername) {
		this.secondaryUsername = secondaryUsername;
	}

	public Long getPrimaryAccount() {
		return primaryAccount;
	}

	public void setPrimaryAccount(Long primaryAccount) {
		this.primaryAccount = primaryAccount;
	}

	public Long getSecondaryAccount() {
		return secondaryAccount;
	}

	public void setSecondaryAccount(Long secondaryAccount) {
		this.secondaryAccount = secondaryAccount;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public String toString(String username) {
		String type = "";
		String otherUser = "";
		
		if (username.equals(this.primaryUsername)) {
			type = "To";
			otherUser = this.secondaryUsername;
		}
		else {
			type = "From";
			otherUser = this.primaryUsername;
		}
		
		String display = String.format("%d\t\t%s:\t%s\t\t$%.2f", this.transferId, type, otherUser, this.amount);
		return display; 
	}
}
