package com.techelevator.tenmo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import com.techelevator.tenmo.dao.ServiceDAO;
import com.techelevator.tenmo.dao.UserDAO;
import com.techelevator.tenmo.model.Accounts;
import com.techelevator.tenmo.model.Transfers;
import com.techelevator.tenmo.model.User;

@RestController
@RequestMapping(path="/tenmo")
@PreAuthorize("isAuthenticated()")
public class ServiceController {
	
	@Autowired
	ServiceDAO transferDAO;
	
	@Autowired
	UserDAO userDAO;
	
	@RequestMapping(path="/get-balance", method=RequestMethod.GET)
	public double getBalance(Principal principal) {
		
		int userId = userDAO.findIdByUsername(principal.getName());
		Accounts userAccount = transferDAO.getUserAccount((long)userId);
		return userAccount.getBalance();
	}
	
	@RequestMapping(path="/get-all", method=RequestMethod.GET)
	public User[] getUsers(Principal principal) {
		
		List<User> temp = userDAO.findAll(principal.getName());
		
		if (temp == null || temp.isEmpty()) {
			return null;
		}
		return temp.toArray(new User[temp.size()]);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(path="/transfer-request", method=RequestMethod.POST)
	public Transfers transferRequest(@RequestBody Transfers request) {
		return transferDAO.transferRequest(request);
	}
	
	@RequestMapping(path="users/transfers/{term}", method=RequestMethod.GET)
	public Transfers[] transferSearch(@PathVariable String term, Principal principal) {
		
		if (term.equals("pending")) {
			term = "AND transfers.transfer_status_id = 1";
		}
		else {
			term = "";
		}
		
		Long id = (long)userDAO.findIdByUsername(principal.getName());
		List<Transfers> temp = transferDAO.transferSearch(id, principal.getName(), term);
		
		if (temp == null || temp.isEmpty()) {
			return null;
		}
		return temp.toArray(new Transfers[temp.size()]);
	}
	
}
