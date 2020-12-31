package com.techelevator.tenmo.dao;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import com.techelevator.tenmo.model.Accounts;
import com.techelevator.tenmo.model.Transfers;

@Component
public class ServiceSqlDAO implements ServiceDAO {
	
	private JdbcTemplate jdbcTemplate;
	
	public ServiceSqlDAO(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public Accounts getUserAccount(Long userId) {
		
		String sqlString = "SELECT * FROM accounts WHERE user_id = ?";
		SqlRowSet results = jdbcTemplate.queryForRowSet(sqlString, userId);
		
		Accounts userAccount = null;
		if (results.next()) {
			Long accountId = results.getLong("account_id");
			double balance = results.getDouble("balance");
			userAccount = new Accounts(accountId, userId, balance);
		}
		return userAccount;
	}

	@Override
	public Transfers transferRequest(Transfers request) {
		
		Long transferId = null;
		Accounts primary = getUserAccount(request.getPrimaryId());
		Accounts secondary = getUserAccount(request.getSecondaryId());
		
		request.setPrimaryAccount(primary.getAccountId());
		request.setSecondaryAccount(secondary.getAccountId());
		
		String status = "";
		String type = "";
		
		if (request.getTypeId() == 2L) {
	
			transferId = getNextTransferId();
			status = "Approved";
			type = "Send";
			request.setTransferId(transferId);
			request.setStatusDesc(status);
			request.setTypeDesc(type);
			createSend(request, primary, secondary);
		}
		else {
			if (request.getStatusId() == 1L) {
				
				transferId = getNextTransferId();
				status = "Pending";
				request.setTransferId(transferId);
			}
			else if (request.getStatusId() == 3L) {
				status = "Rejected";
			}
			else {
				status = "Approved";
			}
			type = "Request";
			request.setStatusDesc(status);
			request.setTypeDesc(type);
			handlePending(request, primary, secondary);
		}
		return request;
	}
	
	@Override
	public void createSend(Transfers request, Accounts primary, Accounts secondary) {
		
		try {
			if (request.getAmount() > secondary.getBalance()) {
				throw new IllegalArgumentException("\nInsufficent funds");
			}
			double newPrimaryBalance = primary.getBalance() - request.getAmount();
			double newSecondaryBalance = secondary.getBalance() + request.getAmount();
			
			String sqlRequest = "BEGIN TRANSACTION; "
					+ "UPDATE accounts SET balance = ? WHERE account_id = ?; "
					+ "UPDATE accounts SET balance = ? WHERE account_id = ?; "
					+ "INSERT INTO transfers (transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount) "
					+ "VALUES(?, ?, ?, ?, ?, ?);"
					+ "COMMIT";
			
			jdbcTemplate.update(sqlRequest, newPrimaryBalance, primary.getAccountId(), newSecondaryBalance, 
					secondary.getAccountId(), request.getTransferId(), request.getTypeId(), request.getStatusId(), primary.getAccountId(), 
					secondary.getAccountId(), request.getAmount());
		}
		catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	@Override
	public void handlePending(Transfers request, Accounts primary, Accounts secondary) {
		
		String sqlRequest = "";
		
		if (request.getStatusId() == 1L) {
			
			sqlRequest = "INSERT INTO transfers (transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount) "
					+ "VALUES(?, ?, ?, ?, ?, ?)";
			
			jdbcTemplate.update(sqlRequest, request.getTransferId(), request.getTypeId(), request.getStatusId(), 
					request.getPrimaryAccount(), request.getSecondaryAccount(), request.getAmount());
		}
		else if (request.getStatusId() == 3L) {
			
			sqlRequest = "UPDATE transfers SET transfer_status_id = ? WHERE transfer_id = ?";
			jdbcTemplate.update(sqlRequest, request.getStatusId(), request.getTransferId());
		}
		else {
			try {
				if (request.getAmount() > secondary.getBalance()) {
					throw new IllegalArgumentException("\nInsufficent funds");
				}
				double newPrimaryBalance = primary.getBalance() + request.getAmount();
				double newSecondaryBalance = secondary.getBalance() - request.getAmount();
				
				sqlRequest = "BEGIN TRANSACTION; "
						+ "UPDATE accounts SET balance = ? WHERE account_id = ?; "
						+ "UPDATE accounts SET balance = ? WHERE account_id = ?; "
						+ "UPDATE transfers SET transfer_status_id = ? WHERE transfer_id = ?;"
						+ "COMMIT";
				
				jdbcTemplate.update(sqlRequest, newPrimaryBalance, primary.getAccountId(), newSecondaryBalance, 
						secondary.getAccountId(), request.getStatusId(), request.getTransferId());
			}
			catch (IllegalArgumentException ex) {
				System.out.println(ex.getMessage());
			}
		}
	}
	
	@Override
	public List<Transfers> transferSearch(Long userId, String username, String additionalTerm) {
		
		Accounts userAccount = getUserAccount(userId);
		List<Transfers> transferList = new ArrayList<Transfers>();
		
		String sqlTransferFrom = "SELECT transfers.transfer_id, transfers.transfer_type_id, transfer_types.transfer_type_desc, "
				+ "transfers.transfer_status_id, transfer_statuses.transfer_status_desc, transfers.account_to, "
				+ "transfers.account_from, users.user_id, users.username, transfers.amount "
				+ "FROM transfers "
				+ "JOIN transfer_types ON transfer_types.transfer_type_id = transfers.transfer_type_id "
				+ "JOIN transfer_statuses ON transfer_statuses.transfer_status_id = transfers.transfer_status_id "
				+ "JOIN accounts ON accounts.account_id = transfers.account_to "
				+ "JOIN users ON users.user_id = accounts.user_id "
				+ "WHERE transfers.account_from = ? " + additionalTerm;
		
		String sqlTransferTo = "SELECT transfers.transfer_id, transfers.transfer_type_id, transfer_types.transfer_type_desc, "
				+ "transfers.transfer_status_id, transfer_statuses.transfer_status_desc, transfers.account_to, "
				+ "transfers.account_from, users.user_id, users.username, transfers.amount "
				+ "FROM transfers "
				+ "JOIN transfer_types ON transfer_types.transfer_type_id = transfers.transfer_type_id "
				+ "JOIN transfer_statuses ON transfer_statuses.transfer_status_id = transfers.transfer_status_id "
				+ "JOIN accounts ON accounts.account_id = transfers.account_from "
				+ "JOIN users ON users.user_id = accounts.user_id "
				+ "WHERE transfers.account_to = ? " + additionalTerm;
		
		SqlRowSet resultsFrom = jdbcTemplate.queryForRowSet(sqlTransferFrom, userAccount.getAccountId());
		SqlRowSet resultsTo = jdbcTemplate.queryForRowSet(sqlTransferTo, userAccount.getAccountId());
		
		while (resultsFrom.next()) {
			Transfers transfer = mapRowToTransfer(resultsFrom);
			String otherUser = resultsFrom.getString("username");
			Long otherId = resultsFrom.getLong("user_id");
			
			transfer.setSecondaryId(otherId);
			transfer.setSecondaryUsername(otherUser);
			transfer.setPrimaryId(userId);
			transfer.setPrimaryUsername(username);
			transferList.add(transfer);
		}
		
		while (resultsTo.next()) {
			Transfers transfer = mapRowToTransfer(resultsTo);
			String otherUser = resultsTo.getString("username");
			Long otherId = resultsTo.getLong("user_id");
			
			transfer.setSecondaryId(userId);
			transfer.setSecondaryUsername(username);
			transfer.setPrimaryId(otherId);
			transfer.setPrimaryUsername(otherUser);
			transferList.add(transfer);
		}
		return transferList;
		
	}
	
	@Override
	public Transfers mapRowToTransfer(SqlRowSet sql) {
		
		Long transferId = sql.getLong("transfer_id");
		Long typeId = sql.getLong("transfer_type_id");
		Long statusId = sql.getLong("transfer_status_id");
		Long accountFrom = sql.getLong("account_from");
		Long accountTo = sql.getLong("account_to");
		double amount = sql.getDouble("amount");
			
		String typeDesc = sql.getString("transfer_type_desc");
		String statusDesc = sql.getString("transfer_status_desc");
			
		Transfers transfer = new Transfers(transferId, typeId, statusId, accountFrom, accountTo, amount);
		transfer.setTypeDesc(typeDesc);
		transfer.setStatusDesc(statusDesc);

		return transfer;
	}
	
	@Override
	public Long getNextTransferId() {
		
		SqlRowSet nextId = jdbcTemplate.queryForRowSet("SELECT nextval('seq_transfer_id')");
		
		if (nextId.next()) {
			return nextId.getLong(1);
		}
		else {
			throw new RuntimeException("Could not retrieve next transfer ID");
		}
	}
	
}
