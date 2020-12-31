package com.techelevator.tenmo.dao;

import java.util.List;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.techelevator.tenmo.model.Accounts;
import com.techelevator.tenmo.model.Transfers;


public interface ServiceDAO {
	
	public Accounts getUserAccount(Long userId);
	
	public Transfers transferRequest(Transfers request);
	
	public void createSend(Transfers request, Accounts primary, Accounts secondary);
	
	public void handlePending(Transfers request, Accounts primary, Accounts secondary);
	
	public List<Transfers> transferSearch(Long userId, String username, String additionalTerm);
	
	//public List<Transfers> pendingTransfers(Long userId, String username);
	
	public Transfers mapRowToTransfer(SqlRowSet sql);
	
	public Long getNextTransferId();
	
}
