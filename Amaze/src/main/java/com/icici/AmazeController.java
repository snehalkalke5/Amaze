package com.icici;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.icici.model.TransactionTimeline;
import com.icici.model.UpdateCategory;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@RestController
@RequestMapping("/api")
public class AmazeController {

	private static final Logger logger = Logger
			.getLogger(AmazeController.class);

	@Autowired
	private AmazeService amazeService;

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"dd MMM yy");

	@Value("${server_ip}")
	private String serverIp;

	@RequestMapping("/getDashboard")
	public Map<String, Object> getDashboard(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL) {

		Statistics dashboardStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		Map<String, Object> dashboardMap = new LinkedHashMap<>();
		dashboardMap.put("USERID", USERID);
		dashboardMap.put("RRN", RRN);
		dashboardMap.put("CHANNEL", CHANNEL);
		dashboardMap.put("MOBILE", MOBILE);
		try {
			Double spendingSumForCurrentMonth = amazeService
					.getSpendingSumForCurrentMonth(USERID);
			
			spendingSumForCurrentMonth = BigDecimal.valueOf(spendingSumForCurrentMonth)
					.setScale(0, RoundingMode.HALF_UP).doubleValue();
			dashboardMap.put("spendingSumOverview", spendingSumForCurrentMonth.longValue());

			List<DBObject> upcomingTransactions = amazeService
					.getUpcomingTransaction(USERID);
			// Map<String, Object> upcomingTranctionMap = new HashMap<>();
			// upcomingTranctionMap.put("count", upcomingTransactions.size());
			Double amount = 0.0;
			for (DBObject upcomingTransaction : upcomingTransactions) {
				Object dueAmount = upcomingTransaction.get("DUE_AMT");
				if (dueAmount != null) {
					amount = amount + Double.valueOf(dueAmount.toString());
				}
			}
			amount = BigDecimal.valueOf(amount)
					.setScale(0, RoundingMode.HALF_UP).doubleValue();
			// upcomingTranctionMap.put("amount", amount);
			dashboardMap.put("upcomingTransaction", amount.longValue());
			dashboardMap.put("upcomingTransactionCount",
					upcomingTransactions.size());

			List<DBObject> serviceRequestOverview = amazeService
					.getServiceRequestOverview(USERID);
			dashboardMap.put("serviceRequestOverview", serviceRequestOverview);

			long recommendationsCount = amazeService
					.getRecommendationsCount(USERID);
			dashboardMap.put("recommendationsCount", recommendationsCount);
			dashboardMap.put("STATUS", "200");
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dashboardMap.put("MSG", exception.getMessage());
			dashboardMap.put("STATUS", "500");
		}

		logger.info("total time taken::"
				+ dashboardStatistics.totalTimeRequired());

		return dashboardMap;
	}

	@RequestMapping("/getOverview")
	public Map<String, Object> getOverview(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL) {

		Statistics overviewStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		// logger.info("serverIP::" + serverIp);
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		String keyValueYearMonth = "";
		try {
			// overview
			List<DBObject> totalBudget = amazeService.getTotalBudget(USERID);
			Map<String, Map<String, Object>> overviewFinalObj = new LinkedHashMap<>();
			dataMap.put("STATUS", "200");
			Object totalCalculatedBugetAmount=totalBudget != null && totalBudget.size() > 0 ? totalBudget
					.get(0).get("budget") : 0;
			Double totalCalculatedBugetAmountInDouble = Double.valueOf(totalCalculatedBugetAmount.toString());
			double totalCalculatedBugetAmountRounding = BigDecimal.valueOf(totalCalculatedBugetAmountInDouble)
					.setScale(3, RoundingMode.HALF_UP).doubleValue();
			dataMap.put("TotalBudgetAmount",totalCalculatedBugetAmountRounding);

			List<String> accounts = amazeService.getAccounts(USERID);
			List<DBObject> overviewList = amazeService
					.getIncomeExpenseOverviewByMonth(accounts);
			List<DBObject> budgetList = amazeService.getBugetMaster(USERID);
			List<DBObject> expenseAnalysisOutputList = amazeService
					.getExpenseAnalysisOutput(accounts);
			List<DBObject> incomeAnalysisDBObjects = amazeService
					.getIncomeAnalysisOutput(accounts);

			logger.info("Overview List is " + overviewList.size());
			logger.info("Budget List is " + budgetList.size());
			logger.info("Expense Analysis Output List is "
					+ expenseAnalysisOutputList.size());
			logger.info("Income Analysis List is "
					+ incomeAnalysisDBObjects.size());

			for (DBObject overviewObject : overviewList) {
				Object yearObject = overviewObject.get("TRAN_YEAR");
				Object monthObject = overviewObject.get("MONTH");
				keyValueYearMonth = ""
						+ amazeService.getMonthByInt(Integer
								.parseInt(monthObject.toString())) + " "
						+ yearObject.toString().substring(2, 4);
				// keyValueYearMonth =
				// ""+monthObject+" "+yearObject.toString().substring(2, 4);
				Map<String, Object> incomeAmountObj = new LinkedHashMap<>();
				Map<String, Object> spendsAmountObj = new LinkedHashMap<>();
				Map<String, Object> incomePercentObj = new LinkedHashMap<>();
				Map<String, Object> spendsPercentObj = new LinkedHashMap<>();

				// Double incomeDouble = (Double) overviewObject.get("income");

				// Double incomeDouble =
				// Double.valueOf(overviewObject.get("income"));
				Double incomeDouble = Double.valueOf(overviewObject.get(
						"income").toString());

				incomeDouble = BigDecimal.valueOf(incomeDouble)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();
				incomeAmountObj.put("Income amount", incomeDouble);
				// Double spendsDouble = (Double) overviewObject.get("spends");
				Double spendsDouble = Double.valueOf(overviewObject.get(
						"spends").toString());

				spendsDouble = BigDecimal.valueOf(spendsDouble)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				Double total = incomeDouble + spendsDouble;

				Double percentSpends = (spendsDouble / total) * 100;
				Double percentIncome = (incomeDouble / total) * 100;

				percentSpends = BigDecimal.valueOf(percentSpends)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				percentIncome = BigDecimal.valueOf(percentIncome)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				incomeAmountObj.put("Income amount", incomeDouble);
				spendsAmountObj.put("Spends amount", spendsDouble);
				incomePercentObj.put("Income percentage", percentIncome);
				spendsPercentObj.put("Spends percentage", percentSpends);

				Map<String, Object> innerObjIncome = new LinkedHashMap<>();
				Map<String, Object> outerObjIncome = new LinkedHashMap<>();

				for (DBObject incomeListObj : incomeAnalysisDBObjects) {
					Object yearObjectinc = incomeListObj.get("TRAN_YEAR");
					Object monthObjectinc = incomeListObj.get("MONTH");
					String keyValueYearMonthinc = "" + amazeService.getMonthByInt(Integer
							.parseInt(monthObjectinc.toString())) + " "
							+ yearObjectinc.toString().substring(2, 4);
					logger.info("keyValueYearMonthMain:"+keyValueYearMonthinc);
					logger.info("keyValueYearMonthCheck:"+keyValueYearMonth);
					if (keyValueYearMonthinc
							.equalsIgnoreCase(keyValueYearMonth)) {
						Map<String, Object> intermediate = new LinkedHashMap<>();
						// intermediate.put("amount", (Double)
						// incomeListObj.get("income"));
						intermediate.put("amount", Double.valueOf(incomeListObj
								.get("income").toString()));
						innerObjIncome.put(
								(String) incomeListObj.get("CATEGORY"),
								intermediate);
					}
				}

				outerObjIncome.put("income", innerObjIncome);
				Map<String, Object> innerObjSpends = new LinkedHashMap<>();
				Map<String, Object> outerObjSpends = new LinkedHashMap<>();

				for (DBObject spendsListObj : expenseAnalysisOutputList) {
					Object yearObjectexp = spendsListObj.get("TRAN_YEAR");
					Object monthObjectexp = spendsListObj.get("MONTH");

					Double BudgetAmountValue = 0.0;
					Double spendsValue = (Double) spendsListObj.get("spends");
					String categoryValue = (String) spendsListObj
							.get("CATEGORY");
					String keyValueYearMonthexp = "" + amazeService.getMonthByInt(Integer
							.parseInt(monthObjectexp.toString())) + " "
							+ yearObjectexp.toString().substring(2, 4);
					
					logger.info("keyValueYearMonthMain:"+keyValueYearMonthexp);
					logger.info("keyValueYearMonthCheck:"+keyValueYearMonth);
					
					if (keyValueYearMonthexp
							.equalsIgnoreCase(keyValueYearMonth)) {
						/*
						 * innerObjIncome.put((String)
						 * incomeListObj.get("CATEGORY"), (Double)
						 * incomeListObj.get("income"));
						 * outerObjIncome.put("income", innerObjIncome);
						 */
						for (DBObject categoryListObj : budgetList) {
							String categoryfrombudgetlist = (String) categoryListObj
									.get("BUCKET");

							logger.info("category match to get budget::"
									+ categoryfrombudgetlist
									+ "::"
									+ categoryValue
									+ "::"
									+ categoryfrombudgetlist
											.equalsIgnoreCase(categoryValue));
							if (categoryfrombudgetlist
									.equalsIgnoreCase(categoryValue)) {
								// CHANGE BUDGET_AMOUNT TO NEW_BUDGET
								if (categoryListObj.get("BUDGET_AMOUNT") == null)
								{
									BudgetAmountValue = 0.0;
								}
								else
								{
									BudgetAmountValue = Double
											.valueOf(categoryListObj.get(
													"BUDGET_AMOUNT").toString());
								}
							}
						}

						Boolean boolValue = true;
						if (BudgetAmountValue == null) {
							BudgetAmountValue = 0.0;
						}
						if (spendsValue > BudgetAmountValue) {
							boolValue = true;
						} else {
							boolValue = false;
						}
						Map<String, Object> amountData = new LinkedHashMap<>();
						amountData.put("amount", spendsValue);
						amountData.put("budget", BudgetAmountValue);
						amountData.put("isOverbudget", boolValue);
						innerObjSpends.put(categoryValue, amountData);
					}

				}
				outerObjSpends.put("spends", innerObjSpends);

				Map<String, Object> overviewPreData = new LinkedHashMap<>();
				overviewPreData.put("Income amount", incomeDouble);
				overviewPreData.put("Income percentage", percentIncome);
				overviewPreData.put("Spends amount", spendsDouble);
				overviewPreData.put("Spends percentage", percentSpends);
				overviewPreData.put("income", innerObjIncome);
				overviewPreData.put("spends", innerObjSpends);
				overviewFinalObj.put(keyValueYearMonth, overviewPreData);
				dataMap.put("Spending Overview", overviewFinalObj);

			}
			 Object object = amazeService.getTillDate(accounts);
			 String tillDate = null;
			 if (object != null) {
					tillDate = simpleDateFormat.format(object);
			 }
			dataMap.put("tillDate", tillDate);
			/*
			 * List<DBObject> overviewList =
			 * amazeService.getIncomeExpenseOverviewByMonth(USERID);
			 * //logger.info(overviewList); // creating Overview record
			 * Statistics incomeExpenseOverviewByMonthStatistics = new
			 * Statistics();
			 * 
			 * Map<String, Map<String, Overview>> overviewYearMap = new
			 * LinkedHashMap<>();
			 * 
			 * Map<String, Overview> overviewMap = new LinkedHashMap<>(); for
			 * (DBObject overviewObject : overviewList) { Object yearObject =
			 * overviewObject.get("TRAN_YEAR"); Object monthObject =
			 * overviewObject.get("MONTH"); if (yearObject != null) {
			 * keyValueYearMonth =
			 * ""+monthObject+" "+yearObject.toString().substring(2, 4);
			 * Map<String, Overview> yearMap =
			 * overviewYearMap.get(keyValueYearMonth);
			 * 
			 * if (yearMap == null) {
			 * logger.info(monthObject.toString());
			 * overviewYearMap.put(keyValueYearMonth, new LinkedHashMap<String,
			 * Overview>());
			 * 
			 * }
			 * 
			 * Double spends =
			 * Double.valueOf(overviewObject.get("spends").toString()); Double
			 * income = Double.valueOf(overviewObject.get("income").toString());
			 * Double total = income + spends;
			 * 
			 * Double percentSpends = (spends / total) * 100; Double
			 * percentIncome = (income / total) * 100;
			 * 
			 * OverviewValue incomeOverviewValue = new OverviewValue(income,
			 * percentIncome); OverviewValue spendOverviewValue = new
			 * OverviewValue(spends, percentSpends); Overview overview = new
			 * Overview(incomeOverviewValue, spendOverviewValue);
			 * overviewYearMap.get(keyValueYearMonth).put(""+ monthObject,
			 * overview);
			 * 
			 * } }
			 * 
			 * logger.info(
			 * "total time to construct incomeExpense Overview By Month::" +
			 * incomeExpenseOverviewByMonthStatistics.totalTimeRequired());
			 * 
			 * // total budget
			 * 
			 * // budget list List<DBObject> budgetList =
			 * amazeService.getBugetMaster(USERID);
			 * 
			 * // expense categorywise
			 * 
			 * // match for expense
			 * 
			 * List<DBObject> expenseAnalysisOutputList =
			 * amazeService.getExpenseAnalysisOutput(USERID);
			 * 
			 * // income categorywise
			 * 
			 * // match for expense
			 * 
			 * List<DBObject> incomeAnalysisDBObjects =
			 * amazeService.getIncomeAnalysisOutput(USERID);
			 * 
			 * Statistics expenseAnalysisDBObjectsStatistics = new Statistics();
			 * // calculating over budgeted and creating view for spending
			 * analysis Map<String, Map<String, Map<String,
			 * SpendingCategoryOverview>>> spendingCategoryMapMonYear = new
			 * LinkedHashMap<>(); // Map<String, Map<String,
			 * SpendingCategoryOverview>> // spendingCategoryMap = new
			 * LinkedHashMap<>(); for (DBObject expenseAnalysisDBObject :
			 * expenseAnalysisOutputList) { Object monObject =
			 * expenseAnalysisDBObject.get("MONTH"); Object yearObject =
			 * expenseAnalysisDBObject.get("TRAN_YEAR"); if (yearObject != null
			 * && monObject != null) {
			 * 
			 * Map<String, Map<String, SpendingCategoryOverview>> yearMap =
			 * spendingCategoryMapMonYear .get(keyValueYearMonth); // remove
			 * duplicate code for if else block if (yearMap == null) {
			 * spendingCategoryMapMonYear.put(keyValueYearMonth, new
			 * LinkedHashMap<>()); } Map<String, SpendingCategoryOverview>
			 * innerMap = spendingCategoryMapMonYear.get(keyValueYearMonth)
			 * .get((String) monObject); if (innerMap == null) { Map<String,
			 * SpendingCategoryOverview> map = new LinkedHashMap<>();
			 * spendingCategoryMapMonYear.get(keyValueYearMonth).put((String)
			 * monObject, map); }
			 * 
			 * Object spendsObj = expenseAnalysisDBObject.get("spends"); Object
			 * categoryFromIncomeExpense =
			 * expenseAnalysisDBObject.get("CATEGORY"); if
			 * (categoryFromIncomeExpense != null) { Double spends = spendsObj
			 * != null ? Double.valueOf(spendsObj.toString()) : 0.0; Boolean
			 * isOverbudget = true; Double budget = 0.0; for (DBObject
			 * budgetDBObject : budgetList) { Object categoryFromBudget =
			 * budgetDBObject.get("CATEGORY"); if (categoryFromBudget != null &&
			 * ((String) categoryFromIncomeExpense).equals((String)
			 * categoryFromBudget)) { isOverbudget = budget < spends ?
			 * Boolean.TRUE : Boolean.FALSE; }
			 * 
			 * }
			 * 
			 * SpendingCategoryOverview spendingCategoryOverview = new
			 * SpendingCategoryOverview(spends, budget, isOverbudget);
			 * spendingCategoryMapMonYear.get(keyValueYearMonth).get((String)
			 * monObject) .put((String) categoryFromIncomeExpense,
			 * spendingCategoryOverview); }
			 * 
			 * } }
			 * 
			 * logger.info("total time required to construct expense by category::"
			 * + expenseAnalysisDBObjectsStatistics.totalTimeRequired()); //
			 * calculating over budgeted and creating view for income analysis
			 * Statistics incomeAnalysisDBObjectsStatistics = new Statistics();
			 * Map<String, Map<String, Map<String, IncomeCategoryOverview>>>
			 * incomeCategoryMapMonYear = new LinkedHashMap<>(); // Map<String,
			 * Map<String, IncomeCategoryOverview>> // incomeCategoryMap = new
			 * LinkedHashMap<>(); for (DBObject incomeAnalysisDBObject :
			 * incomeAnalysisDBObjects) { Object monObject =
			 * incomeAnalysisDBObject.get("MONTH"); Object yearObject =
			 * incomeAnalysisDBObject.get("TRAN_YEAR"); if (monObject != null) {
			 * Map<String, Map<String, IncomeCategoryOverview>> yearMap =
			 * incomeCategoryMapMonYear .get("" + yearObject); // remove
			 * duplicate code for if else block if (yearMap == null) {
			 * incomeCategoryMapMonYear.put("" + yearObject, new
			 * LinkedHashMap<>()); } Map<String, IncomeCategoryOverview>
			 * innerMap = incomeCategoryMapMonYear.get("" + yearObject)
			 * .get((String) monObject); if (innerMap == null) { Map<String,
			 * IncomeCategoryOverview> map = new LinkedHashMap<>();
			 * incomeCategoryMapMonYear.get("" + yearObject).put((String)
			 * monObject, map); }
			 * 
			 * Object incomeObj = incomeAnalysisDBObject.get("income"); Object
			 * categoryFromIncomeExpense =
			 * incomeAnalysisDBObject.get("CATEGORY"); if
			 * (categoryFromIncomeExpense != null) { Double spends = incomeObj
			 * != null ? Double.valueOf(incomeObj.toString()) : 0.0;
			 * IncomeCategoryOverview incomeCategoryOverview = new
			 * IncomeCategoryOverview(spends); Map<String,
			 * IncomeCategoryOverview> map = new LinkedHashMap<>();
			 * map.put((String) categoryFromIncomeExpense,
			 * incomeCategoryOverview); incomeCategoryMapMonYear.get("" +
			 * yearObject).put((String) monObject, map); } } }
			 * 
			 * logger.info("total time required to construct income by category::"
			 * + incomeAnalysisDBObjectsStatistics.totalTimeRequired());
			 * 
			 * dataMap.put("Spending Overview", overviewYearMap); //
			 * dataMap.put("budgetList", budgetList); Map<String, Object>
			 * mapOfSpending_Analysis = new LinkedHashMap<>();
			 * mapOfSpending_Analysis.put("spending",
			 * spendingCategoryMapMonYear); mapOfSpending_Analysis.put("income",
			 * incomeCategoryMapMonYear); dataMap.put("Spending_Analysis",
			 * mapOfSpending_Analysis);
			 */
			// dataMap.put("AnalysisExpense", expenseAnalysisOutputList);
		} catch (Exception exception) {
			// exception.printStackTrace();
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		logger.info("total time taken::"
				+ overviewStatistics.totalTimeRequired());
		return dataMap;

	}

	@RequestMapping("/getCreditCardOverview")
	public Map<String, Object> getSpendAnalysisCreditCard(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL) {

		Statistics ccoverviewStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		// logger.info("serverIP::" + serverIp);
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		String keyValueYearMonth = "";
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("TRAN_DATE_TIME", -1));
		
		try {
			// overview
	//		List<DBObject> totalBudget = amazeService.getTotalBudget(USERID);
			Map<String, Map<String, Object>> ccoverviewFinalObj = new LinkedHashMap<>();
			dataMap.put("STATUS", "200");
	//		Object totalCalculatedBugetAmount=totalBudget != null && totalBudget.size() > 0 ? totalBudget
	//				.get(0).get("budget") : 0;
	//		Double totalCalculatedBugetAmountInDouble = Double.valueOf(totalCalculatedBugetAmount.toString());
	//		double totalCalculatedBugetAmountRounding = BigDecimal.valueOf(totalCalculatedBugetAmountInDouble)
	//				.setScale(3, RoundingMode.HALF_UP).doubleValue();
	//		dataMap.put("TotalBudgetAmount",totalCalculatedBugetAmountRounding);
			String SOURCE_SYSTEM_CODE = "20";
			List<String> accountList = amazeService.getAccountsCC(USERID, SOURCE_SYSTEM_CODE);
			logger.info("Count is "+accountList.size());
			List<DBObject> overviewList = amazeService
					.getPaymentSpendsOverviewByMonth(accountList);
	//		List<DBObject> budgetList = amazeService.getBugetMaster(USERID);
			List<DBObject> expenseAnalysisOutputList = amazeService
					.getExpenseAnalysisOutput_CC(accountList);
			List<DBObject> incomeAnalysisDBObjects = amazeService
					.getIncomeAnalysisOutput_CC(accountList);

			logger.info("Overview List is " + overviewList.size());
	//		logger.info("Budget List is " + budgetList);
			logger.info("Expense Analysis Output List is "
					+ expenseAnalysisOutputList.size());
			logger.info("Income Analysis List is "
					+ incomeAnalysisDBObjects.size());
			Map<String, Map<String, Map<String, Object>>> ccoverview = new LinkedHashMap<>();

			for (DBObject overviewObject : overviewList) {

				String accountValue = (String) overviewObject
						.get("SOURCE_ACCOUNT_NUMBER");
				logger.info("Account No. of Object income  "+accountValue);
				Object yearObject = overviewObject.get("TRAN_YEAR");
				Object monthObject = overviewObject.get("MONTH");
				keyValueYearMonth = ""
						+ amazeService.getMonthByInt(Integer
								.parseInt(monthObject.toString())) + " "
						+ yearObject.toString().substring(2, 4);
				// keyValueYearMonth =
				// ""+monthObject+" "+yearObject.toString().substring(2, 4);
				Map<String, Object> paymentAmountObj = new LinkedHashMap<>();
				Map<String, Object> spendsAmountObj = new LinkedHashMap<>();
	//			Map<String, Object> incomePercentObj = new LinkedHashMap<>();
	//			Map<String, Object> spendsPercentObj = new LinkedHashMap<>();

				// Double incomeDouble = (Double) overviewObject.get("income");

				// Double incomeDouble =
				// Double.valueOf(overviewObject.get("income"));
				Double incomeDouble = Double.valueOf(overviewObject.get(
						"income").toString());

				incomeDouble = BigDecimal.valueOf(incomeDouble)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();
		//		paymentAmountObj.put("Income amount", incomeDouble);
				// Double spendsDouble = (Double) overviewObject.get("spends");
				Double spendsDouble = Double.valueOf(overviewObject.get(
						"spends").toString());

				spendsDouble = BigDecimal.valueOf(spendsDouble)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				Double total = incomeDouble + spendsDouble;

				Double percentSpends = (spendsDouble / total) * 100;
				Double percentIncome = (incomeDouble / total) * 100;

				percentSpends = BigDecimal.valueOf(percentSpends)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				percentIncome = BigDecimal.valueOf(percentIncome)
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				paymentAmountObj.put("Payment amount", incomeDouble);
				spendsAmountObj.put("Spends amount", spendsDouble);
	//			incomePercentObj.put("Income percentage", percentIncome);
	//			spendsPercentObj.put("Spends percentage", percentSpends);

				Map<String, Object> innerObjIncome = new LinkedHashMap<>();
	//			Map<String, Object> outerObjIncome = new LinkedHashMap<>();
				
				for (DBObject incomeListObj : incomeAnalysisDBObjects) {
					Object yearObjectinc = incomeListObj.get("TRAN_YEAR");
					Object monthObjectinc = incomeListObj.get("MONTH");
					
					String accountNoObjectinc = (String)incomeListObj.get("SOURCE_ACCOUNT_NUMBER").toString();
					if(accountValue.equalsIgnoreCase(accountNoObjectinc)){
							
						String keyValueYearMonthinc = "" +amazeService.getMonthByInt(Integer
								.parseInt(monthObjectinc.toString())) + " "
								+ yearObjectinc.toString().substring(2, 4);
						if (keyValueYearMonthinc
								.equalsIgnoreCase(keyValueYearMonth)) {
							Map<String, Object> intermediate = new LinkedHashMap<>();
							// intermediate.put("amount", (Double)
							// incomeListObj.get("income"));
							intermediate.put("amount", Double.valueOf(incomeListObj
									.get("income").toString()));
							innerObjIncome.put(
									(String) incomeListObj.get("CATEGORY"),
									intermediate);
						}
					}
			//		System.out.println("INNER OBJECT INCOME BE LIKE "+innerObjIncome);

				}

	//			outerObjIncome.put("Payment ", innerObjIncome);
				Map<String, Object> innerObjSpends = new LinkedHashMap<>();
	//			Map<String, Object> outerObjSpends = new LinkedHashMap<>();
				for (DBObject spendsListObj : expenseAnalysisOutputList) {
					Object yearObjectexp = spendsListObj.get("TRAN_YEAR");
					Object monthObjectexp = spendsListObj.get("MONTH");
					String accountNoObjectexp = (String)spendsListObj.get("SOURCE_ACCOUNT_NUMBER").toString();
					if(accountNoObjectexp.equalsIgnoreCase(accountValue)){
			//			Double BudgetAmountValue = 0.0;
						Double spendsValue = (Double) spendsListObj.get("spends");
						String categoryValue = (String) spendsListObj
								.get("CATEGORY");
						
						String keyValueYearMonthexp = "" + amazeService.getMonthByInt(Integer
								.parseInt(monthObjectexp.toString())) + " "
								+ yearObjectexp.toString().substring(2, 4);
						if (keyValueYearMonthexp
								.equalsIgnoreCase(keyValueYearMonth)) {
													Map<String, Object> amountData = new LinkedHashMap<>();
							amountData.put("amount", spendsValue);
			//				amountData.put("budget", BudgetAmountValue);
							innerObjSpends.put(categoryValue, amountData);
						}
					}
		//			System.out.println("INNER OBJECT SPENDS BE LIKE "+innerObjSpends);

				}
	//			outerObjSpends.put("Spends", innerObjSpends);

				Map<String, Object> overviewPreData = new LinkedHashMap<>();
				overviewPreData.put("Payment amount", incomeDouble);
	//			overviewPreData.put("Income percentage", percentIncome);
				overviewPreData.put("Spends amount", spendsDouble);
	//			overviewPreData.put("Spends percentage", percentSpends);
				overviewPreData.put("Payment", innerObjIncome);
				overviewPreData.put("Spends", innerObjSpends);
				ccoverviewFinalObj.put(keyValueYearMonth, overviewPreData);
				
				
				ccoverview.put(accountValue, ccoverviewFinalObj);

			}
			dataMap.put("SpendAnalysis", ccoverview);
			Object object = amazeService.getTillDate(accountList);
			 String tillDate = null;
			 if (object != null) {
					tillDate = simpleDateFormat.format(object);
			 }
			dataMap.put("tillDate", tillDate);
			
		} catch (Exception exception) {
			// exception.printStackTrace();
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		logger.info("total time taken::"
				+ ccoverviewStatistics.totalTimeRequired());
		// return dataMap;
		
		return dataMap;
	}
	
	
	
	@RequestMapping("/getTransactions")
	public Map<String, Object> getTransactions(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL,
			@RequestParam(required = false) Long OFFSET,
			@RequestParam(required = false) Long LIMIT) {
		Statistics transactionStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		try {
			
			
//			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
//			df.setTimeZone(TimeZone.getTimeZone("IST"));
//			String date = df.format(new Date());
//			System.out.println("hello snehal date"+date);
			
			
//			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
//			String nowAsISO = df.format(new Date());
//
//			System.out.println(nowAsISO);
//
//			SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
//			//nowAsISO = "2013-05-31T00:00:00Z";
//			Date finalResult = df1.parse(nowAsISO);
//
//			System.out.println("tried iso date"+finalResult);
			
			List<TransactionTimeline> transactions = amazeService
					.getTransactions(USERID, OFFSET, LIMIT);

			dataMap.put("TRANSACTIONS", transactions);
			dataMap.put("STATUS", "200");
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		logger.info("total time required for getTransaction::"
				+ transactionStatistics.totalTimeRequired());
		return dataMap;
		// }
	}

	@RequestMapping("/getNLatestTransactionsUsingJava")
	public Map getTransactions(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL,
			@RequestParam(required = false, defaultValue = "100") Long limit) {

		Statistics nLatestTransactionsStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		Map<String, Map<String, Map<String, List<TransactionTimeline>>>> yearMonthDateTree = new LinkedHashMap<>();
		List<TransactionTimeline> transactions = amazeService.getTransactions(
				USERID, limit, 0L);
		for (TransactionTimeline transactionTimeline : transactions) {
			String tranYear = transactionTimeline.getTRAN_YEAR();
			String tranMon = transactionTimeline.getTRAN_MON();
			Integer tranDay = transactionTimeline.getTRAN_DAY();

			Map<String, Map<String, List<TransactionTimeline>>> monthDateMap = yearMonthDateTree
					.get(tranYear);
			if (monthDateMap == null) {
				monthDateMap = new LinkedHashMap<String, Map<String, List<TransactionTimeline>>>();
				yearMonthDateTree.put(tranYear, monthDateMap);
			}

			Map<String, List<TransactionTimeline>> dateMap = yearMonthDateTree
					.get(tranYear).get(tranMon);
			if (dateMap == null) {
				dateMap = new LinkedHashMap<String, List<TransactionTimeline>>();
				yearMonthDateTree.get(tranYear).put(tranMon, dateMap);
			}

			List<TransactionTimeline> transactionList = yearMonthDateTree
					.get(tranYear).get(tranMon).get(tranDay);
			if (transactionList == null) {
				transactionList = new LinkedList<TransactionTimeline>();
				yearMonthDateTree.get(tranYear).get(tranMon)
						.put("" + tranDay, transactionList);
			}

			yearMonthDateTree.get(tranYear).get(tranMon).get("" + tranDay)
					.add(transactionTimeline);
		}

		logger.info("total time required for getNLatestTransactions::"
				+ nLatestTransactionsStatistics.totalTimeRequired());
		return yearMonthDateTree;
	}

	@RequestMapping("/getNLatestTransactionsByMongo")
	public List<DBObject> getTransactionsByMongo(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL,
			@RequestParam(required = false, defaultValue = "100") Long limit) {

		Statistics nLatestTransactionsStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		// Map<String,Map<String,Map<String,Object>>> yearMonthDateTree=new
		// LinkedHashMap<>();
		List<DBObject> transactions = amazeService
				.getTransactionsByMongo(USERID);
		// for (DBObject transactionTimeline : transactions) {
		// String[] split = ((String)transactionTimeline.get("_id")).split("-");
		// String tranYear = split[0];
		// String tranMon = split[1];
		// String tranDay = split[2];
		//
		// Map<String, Map<String, Object>> monthDateMap =
		// yearMonthDateTree.get(tranYear);
		// if(monthDateMap==null)
		// {
		// monthDateMap=new LinkedHashMap<String, Map<String, Object>>();
		// yearMonthDateTree.put(tranYear, monthDateMap);
		// }
		//
		// Map<String,Object> dateMap =
		// yearMonthDateTree.get(tranYear).get(tranMon);
		// if(dateMap==null)
		// {
		// dateMap=new LinkedHashMap<String, Object>();
		// yearMonthDateTree.get(tranYear).put(tranMon, dateMap);
		// }
		//
		// yearMonthDateTree.get(tranYear).get(tranMon).put(""+tranDay,transactionTimeline.get("TRANSACTIONS"));
		// }

		logger.info("total time required for getNLatestTransactions::"
				+ nLatestTransactionsStatistics.totalTimeRequired());
		return transactions;
	}

	@RequestMapping("/getSRStatus")
	public Map<String, Object> getSRStatus(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL) {

		Statistics srStatusStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		try {

			Date todaysDate = new Date();
			Map<String, List<DBObject>> srByCategory = new LinkedHashMap<>();
			List<DBObject> srStatus = amazeService.getSRStatus(USERID);

			logger.info("srStatus count" + srStatus.size());
			for (DBObject dbObject : srStatus) {
				Object object = dbObject.get("FLAG");
				dbObject.put("SYS_DATE", simpleDateFormat.format(todaysDate));
				if (dbObject.get("TARGET_CLOSE_DATE") != null) {
					String formattedTARGET_CLOSE_DATE = simpleDateFormat
							.format((Date) dbObject.get("TARGET_CLOSE_DATE"));
					dbObject.put("TARGET_CLOSE_DATE",
							formattedTARGET_CLOSE_DATE);
				}
				else
				{
					dbObject.put("TARGET_CLOSE_DATE",
							null);
				}
				if (dbObject.get("ACTUAL_CLOSE_DATE") != null) {
					String formattedACTUAL_CLOSE_DATE = simpleDateFormat
							.format((Date) dbObject.get("ACTUAL_CLOSE_DATE"));
					dbObject.put("ACTUAL_CLOSE_DATE",
							formattedACTUAL_CLOSE_DATE);
				}
				else
				{
					dbObject.put("ACTUAL_CLOSE_DATE",
							null);
				}
				if (dbObject.get("REQUEST_DATE") != null) {
					String formattedREQUEST_DATE = simpleDateFormat
							.format((Date) dbObject.get("REQUEST_DATE"));
					dbObject.put("REQUEST_DATE", formattedREQUEST_DATE);
				}
				else
				{
					dbObject.put("REQUEST_DATE",
							null);
				}
				if (object != null) {
					String key = (String) object;
					List<DBObject> srByCategoryList = srByCategory.get(key);
					if (srByCategoryList == null) {
						srByCategory.put(key, new LinkedList<>());
					}
					dbObject.removeField("FLAG");
					srByCategory.get(key).add(dbObject);
				}
			}
			dataMap.put("SR_DELIVERABLE", srByCategory);
			dataMap.put("STATUS", "200");
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		logger.info("total time required for srStatusStatistics::"
				+ srStatusStatistics.totalTimeRequired());
		return dataMap;

	}

	@RequestMapping("/getUpcomingTransactions")
	public Map<String, Object> getUpcomingTransactions(
			@RequestParam String USERID, @RequestParam Double RRN,
			@RequestParam String MOBILE, @RequestParam String CHANNEL) {

		Statistics upcomingTransactionsStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		try {

			List<DBObject> upcomingTransactions = amazeService
					.getUpcomingTransaction(USERID);

			dataMap.put("getUpcomingTransactions",
					getUpcomingByCategoryView(upcomingTransactions));
			dataMap.put("STATUS", "200");
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		logger.info("total time required for upcomingTransactionsStatistics::"
				+ upcomingTransactionsStatistics.totalTimeRequired());

		return dataMap;

	}

	private Map<String, List<DBObject>> getUpcomingByCategoryView(
			List<DBObject> srStatus) {

		Statistics srByCategoryViewStatistics = new Statistics();
		
		Map<String, List<DBObject>> srByCategory = new LinkedHashMap<>();
		for (DBObject dbObject : srStatus) {
			Object object = dbObject.get("CATEGORY");

			if (dbObject.get("DUE_DATE") != null) {
				String formattedDUE_DATE = simpleDateFormat
						.format((Date) dbObject.get("DUE_DATE"));
				dbObject.put("DUE_DATE", formattedDUE_DATE);
			}
			if (object != null) {
				List<DBObject> srByCategoryList = srByCategory
						.get(((String) object));
				if (srByCategoryList == null) {
					srByCategory.put(((String) object), new ArrayList<>());
				}
				dbObject.removeField("CATEGORY");
				srByCategory.get((String) object).add(dbObject);
			}
		}

		logger.info("total time required for upcomingTransactionsStatistics::"
				+ srByCategoryViewStatistics.totalTimeRequired());

		return srByCategory;
	}

	@RequestMapping("/getCategoryList")
	public Map<String, Object> getCategaory(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL) {

		Statistics categoryListStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		try {

			Map<String, List<DBObject>> categoryMap = new LinkedHashMap<>();
			categoryMap.put("CR", amazeService.getCategoryList("CR"));
			categoryMap.put("DR", amazeService.getCategoryList("DR"));

			dataMap.put("categoryList", categoryMap);
			dataMap.put("STATUS", "200");
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		logger.info("total time required for categoryListStatisticsStatistics::"
				+ categoryListStatistics.totalTimeRequired());

		return dataMap;

	}

	@RequestMapping("/getRecommendation")
	public Map<String, Object> getRecommendation(@RequestParam String USERID,
			@RequestParam Double RRN, @RequestParam String MOBILE,
			@RequestParam String CHANNEL) {
		Statistics getRecommendationStatistics = new Statistics();
		USERID=USERID.toUpperCase();
		
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		try {
			List<DBObject> recommendationList = amazeService
					.getRecommendationList(USERID);
			dataMap.put("recommendations", recommendationList);
			dataMap.put("STATUS", "200");
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}

		logger.info("total time required for getRecommendationStatistics::"
				+ getRecommendationStatistics.totalTimeRequired());
		return dataMap;

	}

	@RequestMapping(value = "/updateCategory", method = RequestMethod.POST)
	public Map<String, Object> updateCategory(
			@RequestBody UpdateCategory updateCategory,
			@RequestParam String USERID, @RequestParam Double RRN,
			@RequestParam String MOBILE, @RequestParam String CHANNEL) {

		logger.info("Update category:" + updateCategory);
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		USERID=USERID.toUpperCase();
		dataMap.put("USERID", USERID);
		dataMap.put("RRN", RRN);
		dataMap.put("CHANNEL", CHANNEL);
		dataMap.put("MOBILE", MOBILE);
		try {
			int updateResult = amazeService.updateCategory(USERID, updateCategory);
			if (updateResult != -1) {
				dataMap = getTransactions(USERID, RRN, MOBILE, CHANNEL, null,
						null);
			} else {
				dataMap.put("STATUS", "500");
				dataMap.put("ERROR", "No records updated");
			}
		} catch (Exception exception) {
			logger.error("Error for operation", exception);
			dataMap.put("STATUS", "500");
		}
		return dataMap;
	}

}
