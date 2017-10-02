/**
 * 
 */
package com.tazhi.rose.example.account.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tazhi.rose.example.account.entity.Account;
import com.tazhi.rose.repository.EventSourcingRepository;

/**
 * 一个对外提供转账接口的Controller。
 * @author Evan Wu
 *
 */
@Controller
public class AccountController {
	private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
	
	@Autowired
	EventSourcingRepository repository;
	
	@RequestMapping(path = "/account/create")
    @ResponseBody
	public Object createAccount(HttpServletRequest request, HttpServletResponse response) {
		String accountId = request.getParameter("accountId");
		String balance = request.getParameter("balance");
		Assert.notNull(accountId);
		Assert.notNull(balance);
		
		logger.info("Create account with accountId " + accountId + " with balance " + balance);
		Account account = new Account(accountId, Integer.valueOf(balance));
		repository.save(account);
		
		return account;
	}
	
	@RequestMapping(path = "/account/get")
    @ResponseBody
    public Object getAccount(HttpServletRequest request, HttpServletResponse response) {
        String accountId = request.getParameter("accountId");
        Assert.notNull(accountId);
        
        return repository.get(Account.class, accountId);
    }
}
