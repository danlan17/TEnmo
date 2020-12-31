package com.techelevator.tenmo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.techelevator.tenmo.dao.ServiceSqlDAO;
import com.techelevator.tenmo.model.Accounts;
import com.techelevator.tenmo.model.Transfers;


// For testing purposes, at the bottom are copied methods from 
// ServiceSqlDAO with the "Begin transaction and Commit" sql 
// string portions removed to prevent database changes
class ServiceSqlDAOIntegrationTest {
	
	private static SingleConnectionDataSource dataSource;
	private ServiceSqlDAO dao;
	private JdbcTemplate jdbcT;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
		dataSource = new SingleConnectionDataSource();
		dataSource.setUrl("jdbc:postgresql://localhost:5432/tenmo");
		dataSource.setUsername("postgres");
		dataSource.setPassword("postgres1");
		dataSource.setAutoCommit(false);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		dataSource.destroy();
	}

	@BeforeEach
	void setUp() throws Exception {
		dao = new ServiceSqlDAO(dataSource);
		jdbcT = new JdbcTemplate(dataSource);
	}

	@AfterEach
	void tearDown() throws Exception {
		dataSource.getConnection().rollback();
	}

	@Test
	void getUserAccountTest() {
		// User id should return proper account
		
		Accounts account = dao.getUserAccount(1L);
		Accounts account2 = dao.getUserAccount(2L);
		
		assertTrue(account.getAccountId() != account2.getAccountId());
		assertTrue(account.getUserId() == 1);
		assertTrue(account2.getUserId() == 2);
	}
	
	@Test
	void transferRequestTest() {
		// Should return filled out Transfers object
		
		Transfers transfer = new Transfers(2L, 2L, 1L, "user", 2L, "admin", 100.00);
		transfer = transferRequest(transfer);
		
		assertTrue(transfer.getTransferId() > 0);
		assertTrue(transfer.getTypeId() > 0);
		assertFalse(transfer.getTypeDesc() == null || transfer.getTypeDesc().isEmpty());
		assertTrue(transfer.getStatusId() > 0);
		assertFalse(transfer.getStatusDesc() == null || transfer.getStatusDesc().isEmpty());
		assertTrue(transfer.getPrimaryId() > 0);
		assertFalse(transfer.getPrimaryUsername() == null || transfer.getPrimaryUsername().isEmpty());
		assertTrue(transfer.getSecondaryId() > 0);
		assertFalse(transfer.getSecondaryUsername() == null || transfer.getSecondaryUsername().isEmpty());
		assertTrue(transfer.getPrimaryAccount() > 0);
		assertTrue(transfer.getSecondaryAccount() > 0);
		assertTrue(transfer.getAmount() > 0);
	}
	
	@Test
	void createSendTest() {
		// Should update proper account balances and insert new transfer log
		
		Accounts primaryBefore = dao.getUserAccount(1L);
		Accounts secondaryBefore = dao.getUserAccount(2L);
		
		Transfers transfer = new Transfers(2L, 2L, 1L, "user", 2L, "admin", 100.00);
		transfer = transferRequest(transfer);
		
		Accounts primaryAfter = dao.getUserAccount(1L);
		Accounts secondaryAfter = dao.getUserAccount(2L);
		String sqlTransfer = "SELECT transfer_id FROM transfers WHERE transfer_id = ?";
		SqlRowSet result = jdbcT.queryForRowSet(sqlTransfer, transfer.getTransferId());	
		Long retrievedTransferId = null;
		
		if (result.next()) {
			retrievedTransferId = result.getLong("transfer_id");
		}
		
		assertEquals(primaryBefore.getBalance() - 100.00, primaryAfter.getBalance());
		assertEquals(secondaryBefore.getBalance() + 100.00, secondaryAfter.getBalance());
		assertEquals(transfer.getTransferId(), retrievedTransferId);
	}
	
	@Test
	void handlePendingTest() {
		// Should insert or update account balances and transfer logs
		// based on pending status
		
		Transfers transfer = new Transfers(1L, 1L, 1L, "user", 2L, "admin", 100.00);
		Transfers transfer2 = new Transfers(1L, 1L, 1L, "user", 2L, "admin", 150.00);
		Transfers transfer3 = new Transfers(1L, 3L, 1L, "user", 2L, "admin", 100.00);
		transfer3.setStatusDesc("Rejected");
		Transfers transfer4 = new Transfers(1L, 2L, 1L, "user", 2L, "admin", 150.00);
		transfer4.setStatusDesc("Approved");
		
		transfer = transferRequest(transfer);
		transfer2 = transferRequest(transfer2);
		
		String sqlTransfer = "SELECT * FROM transfers WHERE transfer_id = ?";
		SqlRowSet result = jdbcT.queryForRowSet(sqlTransfer, transfer.getTransferId());	
		Transfers retrieved = mapRowToSimpleTransfers(result);
		
		//Checks if new pending request is inserted
		assertEqualsSimpleTransfers(transfer, retrieved);
		
		transfer3.setTransferId(transfer.getTransferId());
		transfer4.setTransferId(transfer2.getTransferId());
		transfer3 = transferRequest(transfer3);
		transfer4 = transferRequest(transfer4);
		
		String sqlTransfer3 = "SELECT * FROM transfers WHERE transfer_id = ?";
		String sqlTransfer4 = "SELECT * FROM transfers WHERE transfer_id = ?";
			
		SqlRowSet result3 = jdbcT.queryForRowSet(sqlTransfer3, transfer3.getTransferId());	
		SqlRowSet result4 = jdbcT.queryForRowSet(sqlTransfer4, transfer4.getTransferId());	
		
		
		Transfers retrieved3 = mapRowToSimpleTransfers(result3);
		Transfers retrieved4 = mapRowToSimpleTransfers(result4);
		
		//Checks for proper update after rejecting request
		assertEqualsSimpleTransfers(transfer3, retrieved3);
		
		//Checks for proper update after accepting request
		assertEqualsSimpleTransfers(transfer4, retrieved4);
		
	}
	
	@Test
	void transferHistoryTest() {
		// Should return a list of transfers associated with user
		
		Transfers transfer = new Transfers(2L, 2L, 1L, "user", 2L, "admin", 100.00);
		transfer = transferRequest(transfer);
		List<Transfers> history = dao.transferSearch(1L, "user", "");
		
		boolean transfersInvolveUser = true;
		
		for (Transfers tran : history) {
			if (tran.getPrimaryAccount() != transfer.getPrimaryAccount() && 
					tran.getSecondaryAccount() != transfer.getPrimaryAccount()) {
				transfersInvolveUser = false;
			}
		}
		assertTrue(history.size() > 0);
		assertTrue(transfersInvolveUser);
	}
	
	Transfers mapRowToSimpleTransfers(SqlRowSet result) {
		
		Transfers retrieved = new Transfers(); 
		
		if (result.next()) {
			retrieved.setTransferId(result.getLong("transfer_id"));
			retrieved.setTypeId(result.getLong("transfer_type_id"));
			retrieved.setStatusId(result.getLong("transfer_status_id"));
			retrieved.setPrimaryAccount(result.getLong("account_from"));
			retrieved.setSecondaryAccount(result.getLong("account_to"));
			retrieved.setAmount(result.getDouble("amount"));
		}
		
		return retrieved;
	}
	
	void assertEqualsSimpleTransfers(Transfers expected, Transfers actual) {
		
		assertEquals(expected.getTransferId(), actual.getTransferId());
		assertEquals(expected.getTypeId(), actual.getTypeId());
		assertEquals(expected.getStatusId(), actual.getStatusId());
		assertEquals(expected.getPrimaryAccount(), actual.getPrimaryAccount());
		assertEquals(expected.getSecondaryAccount(), actual.getSecondaryAccount());
		assertEquals(expected.getAmount(), actual.getAmount());
	}
	
	
	// For testing purposes, below are copied methods from ServiceSqlDAO with
	// the "Begin transaction and Commit" sql string portions removed 
	// to prevent database changes
	
	Transfers transferRequest(Transfers request) {
		
		Long transferId = null;
		Accounts primary = dao.getUserAccount(request.getPrimaryId());
		Accounts secondary = dao.getUserAccount(request.getSecondaryId());
		
		request.setPrimaryAccount(primary.getAccountId());
		request.setSecondaryAccount(secondary.getAccountId());
		
		String status = "";
		String type = "";
		
		if (request.getTypeId() == 2L) {
			
			transferId = dao.getNextTransferId();
			status = "Approved";
			type = "Send";
			request.setTransferId(transferId);
			request.setStatusDesc(status);
			request.setTypeDesc(type);
			createSend(request, primary, secondary);
		}
		else {
			
			if (request.getStatusId() == 1L) {
				transferId = dao.getNextTransferId();
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
			request.setTypeDesc(type);
			request.setStatusDesc(status);
			handlePending(request, primary, secondary);
		}
		return request;
	}
	
	void createSend(Transfers request, Accounts primary, Accounts secondary) {
			
		double newPrimaryBalance = primary.getBalance() - request.getAmount();
		double newSecondaryBalance = secondary.getBalance() + request.getAmount();
		
		String sqlRequest = "UPDATE accounts SET balance = ? WHERE account_id = ?; "
				+ "UPDATE accounts SET balance = ? WHERE account_id = ?; "
				+ "INSERT INTO transfers (transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount) "
				+ "VALUES(?, ?, ?, ?, ?, ?)";
		
		jdbcT.update(sqlRequest, newPrimaryBalance, primary.getAccountId(), newSecondaryBalance, 
				secondary.getAccountId(), request.getTransferId(), request.getTypeId(), request.getStatusId(), primary.getAccountId(), 
				secondary.getAccountId(), request.getAmount());
	}
	
	void handlePending(Transfers request, Accounts primary, Accounts secondary) {
		
		String sqlRequest = "";
		
		if (request.getStatusId() == 1L) {
			
			sqlRequest = "INSERT INTO transfers (transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount) "
					+ "VALUES(?, ?, ?, ?, ?, ?)";
			
			jdbcT.update(sqlRequest, request.getTransferId(), request.getTypeId(), request.getStatusId(), 
					request.getPrimaryAccount(), request.getSecondaryAccount(), request.getAmount());
		}
		else if (request.getStatusId() == 3L) {
			
			sqlRequest = "UPDATE transfers SET transfer_status_id = ? WHERE transfer_id = ?";
			jdbcT.update(sqlRequest, request.getStatusId(), request.getTransferId());
		}
		else {
			
			double newPrimaryBalance = primary.getBalance() + request.getAmount();
			double newSecondaryBalance = secondary.getBalance() - request.getAmount();
			
			sqlRequest = "UPDATE accounts SET balance = ? WHERE account_id = ?; "
					+ "UPDATE accounts SET balance = ? WHERE account_id = ?; "
					+ "UPDATE transfers SET transfer_status_id = ? WHERE transfer_id = ?";
			
			jdbcT.update(sqlRequest, newPrimaryBalance, primary.getAccountId(), newSecondaryBalance, 
					secondary.getAccountId(), request.getStatusId(), request.getTransferId());
		}
	}

}
