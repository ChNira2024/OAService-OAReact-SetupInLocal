package com.mashreq.oa.service;

import java.util.Date;
import java.util.List;

import com.mashreq.oa.entity.AuditTrailLog;
import com.mashreq.oa.entity.BudgetDetailsInput;
import com.mashreq.oa.entity.BudgetDetailsOutput;
import com.mashreq.oa.entity.Reversal;

public interface BudgetDetailsService {
	
	public List<BudgetDetailsOutput> getBudgetDetails(BudgetDetailsInput bdInput);

	public void updateBudgetDetails(BudgetDetailsOutput bdOutput,String username);

	public List<Integer> getBudgetYears();
	
	public List<BudgetDetailsOutput> updateBudgetItems(Reversal reversal,String username);// for reversal
	
	public List<AuditTrailLog> getDataFromAuditTrailLog(String serviceCode, String userName,Date updatedFrom,Date updatedTo,String mgmtCompId,String bildingId); //for fetchAudit Trail Log
	
	public List<BudgetDetailsOutput> updateServiceCode(Reversal reversalEdit,String username);//for update service code

}
