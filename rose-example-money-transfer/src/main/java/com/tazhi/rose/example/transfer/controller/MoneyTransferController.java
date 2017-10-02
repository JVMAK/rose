/**
 * 
 */
package com.tazhi.rose.example.transfer.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tazhi.rose.entity.IdGenerator;
import com.tazhi.rose.example.transfer.entity.MoneyTransfer;
import com.tazhi.rose.repository.EventSourcingRepository;

/**
 * 一个对外提供转账接口的Controller。
 * @author Evan Wu
 *
 */
@Controller
public class MoneyTransferController {
	private static final Logger logger = LoggerFactory.getLogger(MoneyTransferController.class);
	
	@Autowired
	EventSourcingRepository repository;
	
	@RequestMapping(path = "/transfer/create")
    @ResponseBody
	public Object createMoneyTransfer(HttpServletRequest request, HttpServletResponse response) {
		String fromAccountId = request.getParameter("fromAccountId");
		String toAccountId = request.getParameter("toAccountId");
		String strAmount = request.getParameter("amount");
		Assert.notNull(fromAccountId);
		Assert.notNull(toAccountId);
		Assert.notNull(strAmount);
		
		String transactionId = IdGenerator.getInstance().generateId();
		logger.info("Transfering " + strAmount + " from " + fromAccountId + " to " + toAccountId + ", transactionId: " + transactionId);
		MoneyTransfer transfer = new MoneyTransfer(transactionId, fromAccountId, toAccountId, Integer.parseInt(strAmount));
		repository.save(transfer);
		
		return transfer;
	}
	
	@RequestMapping(path = "/transfer/get")
    @ResponseBody
    public Object getMoneyTransfer(HttpServletRequest request, HttpServletResponse response) {
        String transactionId = request.getParameter("transactionId");
        Assert.notNull(transactionId);
        
        return repository.get(MoneyTransfer.class, transactionId);
    }
}
