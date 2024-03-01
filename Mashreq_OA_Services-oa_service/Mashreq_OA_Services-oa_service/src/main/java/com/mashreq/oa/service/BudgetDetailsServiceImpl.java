package com.mashreq.oa.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mashreq.oa.dao.BudgetDetailsDao;
import com.mashreq.oa.entity.AuditTrail2;
import com.mashreq.oa.entity.AuditTrailLog;
import com.mashreq.oa.entity.BudgetDetailsInput;
import com.mashreq.oa.entity.BudgetDetailsOutput;
import com.mashreq.oa.entity.Reversal;

@Service
public class BudgetDetailsServiceImpl implements BudgetDetailsService {

	@Autowired
	private BudgetDetailsDao budgetDetailsDao;
	@Autowired
	public HttpSession session;
	
	private static final Logger logger = (Logger) LoggerFactory.getLogger(BudgetDetailsServiceImpl.class);
	
	@Override
	public List<BudgetDetailsOutput> getBudgetDetails(BudgetDetailsInput bdInput) {
		
		logger.info("Inside getBudgetDetails() in BudgetDetailsServiceImpl");
		
		List<BudgetDetailsOutput> bdDetails = budgetDetailsDao.getBudgetDetails(bdInput);
		/*Set<BudgetDetailsOutput> s=new HashSet<BudgetDetailsOutput>();
		s.addAll(bdDetails);
		bdDetails=new ArrayList<BudgetDetailsOutput>();
		bdDetails.addAll(s);*/
		return bdDetails;
		
	}

	@Override
	public void updateBudgetDetails(BudgetDetailsOutput bdOutput,String username) {
		
		logger.info("Inside updateBudgetDetails() in BudgetDetailsServiceImpl");
		
		
		budgetDetailsDao.updateBudgetDetails(bdOutput,username);
		
	}

	@Override
	public List<Integer> getBudgetYears() {
		try {
			logger.info("Calling getBudgetYears() in BudgetDetailsService");
			List<Integer> budgetYears=budgetDetailsDao.getBudgetYears();
			return budgetYears;
		}
		catch(Exception e)
		{
			logger.info("Exception in getBudgetYears() in BudgetDetailsService :: "+e.getCause());
			return null;
		}
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<BudgetDetailsOutput> updateBudgetItems(Reversal reversal,String username) 
	{
		logger.info("inside com.mashreq.oa.service.BudgetItemsDetailsUpdateServiceImpl class having updateBudgetItems()-Reversal Data: "+reversal);
		
		List<BudgetDetailsOutput> budgetItemData = null;

		// Fetch all records from OA_AUDIT_TRAIL based on payment ID
		List<AuditTrail2> auditTrailRecords = budgetDetailsDao.getAuditTrailRecords(reversal);
		logger.info("fetched auditTrailRecords: " + auditTrailRecords);
		
		String uploadedBy = username;
		Map<String, Double> serviceCodeDifferencesAmount = auditTrailRecords.stream()
		        .collect(Collectors.groupingBy(AuditTrail2::getServiceCode,
		                Collectors.summingDouble(record -> {
		                    double oldValue = Double.parseDouble(record.getOldValue());
		                    double newValue = Double.parseDouble(record.getNewValue());
		                    return Math.abs(oldValue - newValue);
		                })));
        // Summing up differences amount  from all service codes
        Double consumedAmount = serviceCodeDifferencesAmount.values().stream().mapToDouble(Double::doubleValue).sum();
        logger.info("consumedAmount:"+consumedAmount);
        logger.info("inVoiceAmount:"+reversal.getInvoiceAmount());


        BudgetDetailsOutput budgetItem = null;
        if (consumedAmount.equals(reversal.getInvoiceAmount())) 
        {
            System.out.println("Invoice amount matches calculated value (consumedAmount).");

           
            
            // Iterate over all AuditTrail2 records and update BudgetItem data
            for (AuditTrail2 auditTrailRecord : auditTrailRecords) 
            {
            	budgetItemData = budgetDetailsDao.getBudgetItemById(auditTrailRecord.getId());

                // Assuming there is only one BudgetDetailsOutput object for each AuditTrail2 record
                if (!budgetItemData.isEmpty()) 
                {
                     budgetItem = budgetItemData.get(0);
                    
                    // Calculate consumedAmount separately for each AuditTrail2 service code 
                    Double consumedAmountt = serviceCodeDifferencesAmount.get(auditTrailRecord.getServiceCode());

                    Double updatedConsumerAmount = budgetItem.getConsumedAmount() - consumedAmountt;
                    Double updatedBalanceAmount = budgetItem.getBalanceAmount() + consumedAmountt;

                    budgetItem.setConsumedAmount(updatedConsumerAmount);
                    budgetItem.setBalanceAmount(updatedBalanceAmount);
                    budgetItem.setServiceCode(auditTrailRecord.getServiceCode());

                    // Save the updated BudgetItem data
                    budgetDetailsDao.updateBudgetItemTable(budgetItem);
                    
                    budgetDetailsDao.logAuditTrail(budgetItem, auditTrailRecord, reversal.getInvoiceAmount(), uploadedBy, "Reversal Action Done");
                }
            }
        }
        else 
        {
            System.out.println("Invoice amount does not match calculated value (consumedAmount).");
        }	
		return budgetItemData;
	}
	
	@Override
	public List<AuditTrailLog> getDataFromAuditTrailLog(String serviceCode, String userName, Date updatedFrom,Date updatedTo,String mgmtCompId,String bildingId) 
	{
		List<AuditTrailLog> listOfData = null;
		logger.info("BudgetItemsDetailsUpdateServiceImpl class having getDataFromAuditTrailLog() method ");
		listOfData = budgetDetailsDao.fetchAuditTrailLog(serviceCode, userName, updatedFrom,updatedTo, mgmtCompId, bildingId);
		logger.info("BudgetItemsDetailsUpdateServiceImpl class having getDataFromAuditTrailLog() method listOfData"+listOfData);
		return listOfData;
	}
	
	@Override
	public List<BudgetDetailsOutput> updateServiceCode(Reversal reversalEdit, String username) 
	{
	    logger.info("BudgetDetailsServiceImpl class having updateServiceCode() method ");
	    List<BudgetDetailsOutput> data = new ArrayList<>();
	    List<AuditTrail2> auditTrailRecords = budgetDetailsDao.getAuditTrailRecords(reversalEdit);

	    
	    logger.info("auditTrailRecords Size is: "+auditTrailRecords.size());
	    if (auditTrailRecords.size() == 1) 
	    {
	        logger.info("auditTrailRecords size is 1");
	        List<BudgetDetailsOutput> result = updateBudgetItems(reversalEdit, username);//payment revisal method

	        if (result != null) 
	        {
	            data.addAll(result);
	            logger.info("DATA:" + data);
	        }
	    } 
	    else 
	    {
	    	logger.info("auditTrailRecords size is not 1");
	       // call existing payment processing method
	    	
	    }
	    return data;	
	}
}