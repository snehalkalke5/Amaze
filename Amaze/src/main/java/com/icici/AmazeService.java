package com.icici;

import java.sql.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.icici.model.TransactionTimeline;
import com.icici.model.UpdateCategory;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

@Service
public class AmazeService {
	// @Value("${limit_transactions}")
	// private int limit;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"dd MMM yy");

	private static final Logger logger = Logger.getLogger(AmazeService.class);

	public Date getLatestRecordDate(String collectionName) {
		Date latestDate = null;
		Statistics getLatestRecordDateStatistics = new Statistics();

		Criteria c1 = new Criteria("STATUS").is("ENDED");
		Criteria c2 = new Criteria("COLLECTION_NAME").is(collectionName);

		Criteria andOperator = c1.andOperator(c2);

		MatchOperation matchStage = Aggregation.match(andOperator);
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("CURRENT_TS", -1));

		Aggregation aggregation = null;

		LimitOperation limitOperation = Aggregation.limit(1);
		aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(sortCondition), limitOperation);

		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_JOB_AUDIT", DBObject.class);
		logger.info("total time required for getLatestRecordDate::"
				+ getLatestRecordDateStatistics.totalTimeRequired());
		DBObject dbObject = output.getMappedResults().size() > 0 ? output
				.getMappedResults().get(0) : null;

				logger.info("dbObject::" + dbObject);
		if (dbObject.get("JOB_DATE") instanceof Date) {
			latestDate = dbObject != null ? (Date) dbObject.get("JOB_DATE")
					: null;
		} else if (dbObject.get("JOB_DATE") instanceof String) {
			Object object = dbObject.get("JOB_DATE");
			if (object != null) {
				try {
					latestDate = new SimpleDateFormat("dd-MM-yyyy")
							.parse((String) object);
				} catch (ParseException e) {
					logger.warn(
							"Please check AMAZE_JOB_AUDIT table, not able to parse date."
							+ "Format should be dd-MM-yyyy or it should be date object.",
							e);
				}
			}
		}
		if (latestDate == null) {
			logger.warn("Please check AMAZE_JOB_AUDIT. "
					+ "AMAZE_JOB_AUDIT should have ENDED date for job. "
					+ "Considering current date minus 2 days for collection "+ collectionName);
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_MONTH, -2);
			calendar.set(Calendar.HOUR, 00);
			calendar.set(Calendar.MINUTE, 00);
			calendar.set(Calendar.SECOND, 00);
			calendar.set(Calendar.MILLISECOND, 00);
			latestDate = calendar.getTime();
		}
		return latestDate;
	}

	public List<String> getAccounts(String userId) {
		Statistics getUserIdStatistics = new Statistics();
		List<String> accountList=new ArrayList<String>();
		
		Date latestRecordDate = getLatestRecordDate("AMAZE_USERID_ACCOUNT");
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);
		logger.info("before adding day::" + calendar.getTime());
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		logger.info("after adding day::" + calendar.getTime());

		Criteria c1 = new Criteria("USER_ID").is(userId);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria andOperator = c1.andOperator(c2, c3);
		MatchOperation matchStage = Aggregation.match(andOperator);
		

		Aggregation aggregation = Aggregation.newAggregation(matchStage);

		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_USERID_ACCOUNT", DBObject.class);
		logger.info("total time required for getLatestRecord::"
				+ getUserIdStatistics.totalTimeRequired());

		for (DBObject dbObject : output) {
			accountList.add((String)dbObject.get("SOURCE_ACCOUNT_NBR"));
		}
		logger.info("accountList"+accountList);
		return accountList;
	}
	public List<String> getAccountsCC(String userId, String SOURCE_SYSTEM_CODE) {
		Statistics getUserIdStatistics = new Statistics();
		List<String> accountList=new ArrayList<String>();
		Date latestRecordDate = getLatestRecordDate("AMAZE_USERID_ACCOUNT");
		logger.info("latest record date is" +latestRecordDate);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);
		logger.info("before adding day::" + calendar.getTime());
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		logger.info("after adding day::" + calendar.getTime());

		Criteria c1 = new Criteria("USER_ID").is(userId);
		Criteria c2 = new Criteria("SOURCE_SYSTEM_CODE").is(SOURCE_SYSTEM_CODE);
		Criteria c3 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c4 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria andOperator = c1.andOperator(c2, c3,c4);
		MatchOperation matchStage = Aggregation.match(andOperator);
		

		Aggregation aggregation = Aggregation.newAggregation(matchStage);

		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_USERID_ACCOUNT", DBObject.class);
		logger.info("total time required for getLatestRecord::"
				+ getUserIdStatistics.totalTimeRequired());

		for (DBObject dbObject : output) {
			accountList.add((String)dbObject.get("SOURCE_ACCOUNT_NBR"));
		}
		logger.info("accountList"+accountList);
		return accountList;
	}
	
	public Object getTillDate(List<String> accountList) {
		
		Statistics getOldestDateStatistics = new Statistics();
		Date latestRecordDate = getLatestRecordDate("AMAZE_TRANS_PARAM");
		logger.info("latest record date is" +latestRecordDate);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);
		logger.info("before adding day::" + calendar.getTime());
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		logger.info("after adding day::" + calendar.getTime());

		Criteria c1 = new Criteria("SOURCE_ACCOUNT_NBR").in(accountList);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		MatchOperation matchStage = Aggregation.match(c1.andOperator(c2,c3));
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("MAX_TRAN_DATE",-1));

		Aggregation aggregation = null;

		LimitOperation limitOperation = Aggregation.limit(1);
		aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(sortCondition), limitOperation);
		AggregationResults<DBObject> Output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANS_PARAM",
				DBObject.class);
		
		logger.info("total time required for getOldestDate::"
				+ getOldestDateStatistics.totalTimeRequired());
		
		DBObject dbObject = Output.getMappedResults().size() > 0 ? Output
				.getMappedResults().get(0) : null;

				logger.info("dbObject::" + dbObject);
				
		return dbObject.get("MAX_TRAN_DATE");

	}
	

	public Double getSpendingSumForCurrentMonth(String userId) {

		Statistics getSpendingSumForCurrentMonthStatistics = new Statistics();

		Double spendingSumForCurrentMonth = 0.0;
		SimpleDateFormat monFormat = new SimpleDateFormat("MMM");
		SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
		Calendar calendar = Calendar.getInstance();
		Date time = calendar.getTime();
		String MMM = monFormat.format(time);
		String yyyy = yearFormat.format(time);
		logger.info("MMM::" + MMM);
		List<String> accountList = getAccounts(userId);
		Criteria c1 = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
		Criteria c2 = new Criteria("TRAN_MON").is(MMM.toUpperCase());
		Criteria c3 = new Criteria("TRAN_YEAR").is(yyyy); //-Tuere
		Criteria c4 = new Criteria("INCOME_EXPENSE").is("EXPENSE"); //-Tuere
		Criteria c1Andc2Criteria = c1.andOperator(c2,c3,c4);
		// Criteria c1Andc2Criteria =
		// c1.andOperator(c2).andOperator(c3).andOperator(c4); -Tuere
		MatchOperation matchStage = Aggregation.match(c1Andc2Criteria);

		DBObject myGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("_id", 1)
						.append("AMOUNT", 1)
						.append("TRAN_MON", 1)
						.append("EXPENSE",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 }))
		// .append("income",
		// new BasicDBObject("$cond",
		// new Object[] {
		// new BasicDBObject("$eq", new Object[] { "$INCOME_EXPENSE",
		// "INCOME"
		// }),
		// "$AMOUNT", 0 }))
		);
		DBObject myGroup1 = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", new BasicDBObject("MONTH", "$TRAN_MON"

				)).append("spends", new BasicDBObject("$sum", "$EXPENSE")));

		logger.info("aggregation1:" + myGroup);
		logger.info("aggregation2:" + myGroup1);
		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(myGroup), new CustomGroupOperation(
						myGroup1));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		List<DBObject> dbObjects = output.getMappedResults();
		if (dbObjects.size() >= 1) {
			Object object = dbObjects.get(0).get("spends");
			if (object != null) {
				spendingSumForCurrentMonth = Double.valueOf(object.toString());
			}
		}

		logger.info("total time required for getSpendingSumForCurrentMonth::"
				+ getSpendingSumForCurrentMonthStatistics.totalTimeRequired());
		return spendingSumForCurrentMonth;
	}

	// public List<DBObject> getUpcomingTransaction(String userID) {
	// Criteria c1 = new Criteria("USER_ID").is(userID);
	// MatchOperation matchStage = Aggregation.match(c1);
	//
	// Aggregation aggregation = Aggregation.newAggregation(matchStage);
	// AggregationResults<DBObject> output =
	// mongoTemplate.aggregate(aggregation, "AMAZE_UPCOMING_TRANSACTION",
	// DBObject.class);
	// List<DBObject> categoryList = output.getMappedResults();
	//
	// return categoryList;
	// }

	public List<DBObject> getServiceRequestOverview(String userID) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_SR_DELIVERABLE_STATUS");
		logger.info("latestRecordDate::" + latestRecordDate);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);
		logger.info("before adding day::" + calendar.getTime());
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		logger.info("after adding day::" + calendar.getTime());
		Statistics getServiceRequestOverviewStatistics = new Statistics();

		Criteria c1 = new Criteria("USER_ID").is(userID);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria andOperator = c1.andOperator(c2, c3);
		MatchOperation matchStage = Aggregation.match(andOperator);

		DBObject myGroup = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", "$STATUS").append("count",
						new BasicDBObject("$sum", 1)));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(myGroup));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_SR_DELIVERABLE_STATUS", DBObject.class);
		List<DBObject> serviceRequestOverview = output.getMappedResults();

		logger.info("total time required for getServiceRequestOverview::"
				+ getServiceRequestOverviewStatistics.totalTimeRequired());
		return serviceRequestOverview;
	}

	public long getRecommendationsCount(String userID) {
		Statistics getRecommendationsCountStatistics = new Statistics();

		Date latestRecordDate = getLatestRecordDate("AMAZE_RECOMMENDATION_ENGINE");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		Criteria c1 = new Criteria("USER_ID").is(userID);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria msgTypeCriteria = new Criteria("MSG_TYPE").ne("DISCOVER NEW");
		Criteria andOperator = c1.andOperator(msgTypeCriteria,c2, c3);
		// MatchOperation matchStage = Aggregation.match(c1);
		Query query = new Query(andOperator);
		// Aggregation aggregation = Aggregation.newAggregation(matchStage);
		// AggregationResults<DBObject> output =
		// mongoTemplate.aggregate(aggregation, "AMAZE_RECOMMENDATION_ENGINE",
		// DBObject.class);
//		AggregationResults<DBObject> output =
		// mongoTemplate.aggregate(aggregation, "AMAZE_RECOMMENDATION_ENGINE",
		// DBObject.class);
		long count = mongoTemplate.count(query, "AMAZE_RECOMMENDATION_ENGINE");

		logger.info("total time required for getRecommendationsCount::"
				+ getRecommendationsCountStatistics.totalTimeRequired());
		return count;
	}

	public List<DBObject> getRecommendationsOverview(String userID) {

		Statistics getRecommendationsOverviewStatistics = new Statistics();
		Criteria c1 = new Criteria("USER_ID").is(userID);
		MatchOperation matchStage = Aggregation.match(c1);

		Aggregation aggregation = Aggregation.newAggregation(matchStage);
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_RECOMMENDATION_ENGINE", DBObject.class);
		List<DBObject> recommendationsOverview = output.getMappedResults();

		logger.info("total time required for getRecommendationsOverview::"
				+ getRecommendationsOverviewStatistics.totalTimeRequired());
		return recommendationsOverview;
	}

	public List<DBObject> getBugetMaster(String USERID) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_BUDGET_MASTER");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		Statistics getBugetMasterStatistics = new Statistics();
		Criteria c1 = new Criteria("USER_ID").is(USERID);
		Criteria c2 = new Criteria("INCOME_EXPENSE").is("EXPENSE");

		Criteria c3 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c4 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria matcher2 = c1.andOperator(c2, c3, c4);

		//Criteria matcher2 = c1.andOperator(c2);
		MatchOperation matchStage2 = Aggregation.match(matcher2);

		// TUERE PLEASE CHANGE BUDGET_AMOUNT TO NEW_AMOUNT
		DBObject myGroup2 = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("_id", 0).append("BUDGET_AMOUNT", 1).append(
						"BUCKET", 1));

		Aggregation aggregation1 = Aggregation.newAggregation(matchStage2,
				new CustomGroupOperation(myGroup2));
		AggregationResults<DBObject> output1 = mongoTemplate.aggregate(
				aggregation1, "AMAZE_BUDGET_MASTER", DBObject.class);
		List<DBObject> budgetList = output1.getMappedResults();
		logger.info("total time required for getBugetMaster::"
				+ getBugetMasterStatistics.totalTimeRequired());
		return budgetList;
	}

	public List<DBObject> getCategoryList(String type) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_CATEGORY_MASTER");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		Statistics getCategoryListStatistics = new Statistics();
		Criteria c1 = new Criteria("DR_CR_FLAG").is(type);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());

		Criteria andOperator = c1.andOperator(c2, c3);
		MatchOperation matchStage = Aggregation.match(andOperator);

		DBObject myGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("_id", 0).append("BUCKET", 1).append(
						"CATEGORY", 1));
//		DBObject myGroup2 = (DBObject) new BasicDBObject("$group",
//				new BasicDBObject("_id",
//						new BasicDBObject("BUCKET", "$BUCKET").append(
//								"CATEGORY", "$CATEGORY")));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(myGroup));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_CATEGORY_MASTER", DBObject.class);
		logger.info("total time required for getCategoryList::"
				+ getCategoryListStatistics.totalTimeRequired());
		return output.getMappedResults();

	}

	public List<TransactionTimeline> getTransactions(String USERID, Long LIMIT,
			Long OFFSET) {
		Statistics getTransactionsStatistics = new Statistics();
		List<String> accountList = getAccounts(USERID);
		Criteria c1 = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
		MatchOperation matchStage = Aggregation.match(c1);

		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("TRAN_DATE_TIME", -1));
		
		DBObject projectGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("_id", 0).append("TRAN_DATE_TIME", 1)
				.append("TRAN_DAY", 1)
				.append("TRAN_MON", 1)
				.append("TRAN_YEAR", 1)
				.append("SOURCE_ACCOUNT_NUMBER", 1)
				.append("SOURCE_SYSTEM_CODE", 1)
				.append("MERCHANT", 1)
				.append("CATEGORY", 1)
				.append("MERCHANT", 1)
				.append("CATEGORY", 1)
				.append("DEBIT_CREDIT", 1)
				.append("AMOUNT", 1)
				.append("BUCKET", 1)
				.append("EVENTID", 1)
				.append("INCOME_EXPENSE", 1)
				);
		
		Aggregation aggregation = null;

		// Should only Limit be allowed? -Tuere
		// e.g., -Tuere
		// start of example
		// if (LIMIT != null) {
		// LimitOperation limitOperation = Aggregation.limit(LIMIT);
		// aggregation = Aggregation.newAggregation(matchStage, limitOperation,
		// new CustomGroupOperation(sortCondition));
		// end of example
		// } else if (LIMIT != null && OFFSET != null) { .. blah blah blah
		// -Tuere

		if (LIMIT != null && OFFSET != null) {
			LimitOperation limitOperation = Aggregation.limit(LIMIT);
			SkipOperation skipOperation = Aggregation.skip(OFFSET);
			aggregation = Aggregation.newAggregation(matchStage,
					limitOperation, skipOperation, new CustomGroupOperation(
							sortCondition));
		} else {
			// if (limit > 0) {
			// LimitOperation limitOperation = Aggregation.limit(limit);
			// aggregation = Aggregation
			// .newAggregation(matchStage, limitOperation,
			// new CustomGroupOperation(sortCondition));
			// } else {
			aggregation = Aggregation.newAggregation(matchStage,new CustomGroupOperation(projectGroup),
					new CustomGroupOperation(sortCondition));
			// }
		}

		AggregationResults<TransactionTimeline> output = mongoTemplate
				.aggregate(aggregation, "AMAZE_TRANSACTION_TIMELINE",
						TransactionTimeline.class);
		logger.info("total time required for getTransactions::"
				+ getTransactionsStatistics.totalTimeRequired());
		return output.getMappedResults();
	}

	public List<DBObject> getRecommendationList(String userId) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_RECOMMENDATION_ENGINE");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		Statistics getRecommendationListStatistics = new Statistics();
		Criteria c1 = new Criteria("USER_ID").is(userId);
//				.andOperator(new Criteria("MSG_TYPE").ne("DISCOVER_NEW"));
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
//		Criteria c4 = new Criteria("MSG_TYPE").ne("DISCOVER NEW");
		Criteria andOperator = c1.andOperator(c2, c3);
		
		MatchOperation matchStage = Aggregation.match(andOperator);

		DBObject myGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("MSG_Type", "$MSG_TYPE").append("_id", 0)
						.append("Com_Text", "$COM_TEXT")
						.append("Source_account_nbr", "$SOURCE_ACCOUNT_NBR")
						.append("category", "$CATEGORY")
						.append("CTA_HASHTAG", 1).append("PARAM_1", 1)
						.append("cta_flag", 1));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(myGroup));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_RECOMMENDATION_ENGINE", DBObject.class);
		logger.info(output);
		logger.info("total time required for getRecommendationList::"
				+ getRecommendationListStatistics.totalTimeRequired());
		return output.getMappedResults();

	}

	public List<DBObject> getUpcomingTransaction(String userID) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_UPCOMING_TRANSACTION");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("DUE_DATE", 1));
		Statistics getUpcomingTransactionStatistics = new Statistics();
		Criteria c1 = new Criteria("USER_ID").is(userID);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria andOperator = c1.andOperator(c2, c3);
		MatchOperation matchStage = Aggregation.match(andOperator);

		DBObject myGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("CATEGORY", 1).append("_id", 0)
						.append("src_account_no", "$SRC_ACCOUNT_NO")
						.append("MERCHANT", 1).append("DUE_AMT", 1)
						.append("DUE_DATE", 1).append("RECOMMENDATION", 1)
						.append("cta_flag", "$CTA_FLAG"));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(myGroup),new CustomGroupOperation(sortCondition));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_UPCOMING_TRANSACTION", DBObject.class);
		logger.info("total time required for getUpcomingTransaction::"
				+ getUpcomingTransactionStatistics.totalTimeRequired());
		return output.getMappedResults();
	}

	public List<DBObject> getSRStatus(String userId) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_SR_DELIVERABLE_STATUS");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		Statistics getSRStatusStatistics = new Statistics();
		Criteria c1 = new Criteria("USER_ID").is(userId);
		Criteria c2 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c3 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria andOperator = c1.andOperator(c2, c3);
		MatchOperation matchStage = Aggregation.match(andOperator);

		DBObject myGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("_id", 0).append("FLAG", 1)
						.append("TRACKING_NUMBER", 1)
						.append("SOURCE_ACCOUNT_NBR", 1)
						.append("REQUEST_DESC", 1)
						.append("TARGET_CLOSE_DATE", 1)
						.append("ACTUAL_CLOSE_DATE", 1)
						.append("REQUEST_DATE", 1).append("STATUS", 1)
						.append("STAGE", 1));
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("REQUEST_DATE", -1));

		logger.info("srStatus count::" + myGroup);
		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(myGroup), new CustomGroupOperation(
						sortCondition));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_SR_DELIVERABLE_STATUS", DBObject.class);
		logger.info("total time required for getSRStatus::"
				+ getSRStatusStatistics.totalTimeRequired());
		return output.getMappedResults();
	}

	// CHANGE BUDGET_AMOUNT TO NEWBUDGET
	public List<DBObject> getTotalBudget(String userId) {

		Date latestRecordDate = getLatestRecordDate("AMAZE_BUDGET_MASTER");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestRecordDate);

		calendar.add(Calendar.DAY_OF_MONTH, 1);

		Statistics getTotalBudgetStatistics = new Statistics();
		Criteria c1 = new Criteria("USER_ID").is(userId);

		Criteria c2 = new Criteria("INCOME_EXPENSE").is("EXPENSE");

		Criteria c3 = new Criteria("DATE_CREATED").gte(latestRecordDate);
		Criteria c4 = new Criteria("DATE_CREATED").lt(calendar.getTime());
		Criteria matcher2 = c1.andOperator(c2, c3, c4);
		//Criteria matcher2 = c1.andOperator(c2);
		
		MatchOperation matchStage1 = Aggregation.match(matcher2);
		DBObject myGroup2 = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("_id", 0).append("USER_ID", 1).append(
						"BUDGET_AMOUNT", 1));
		DBObject myGroup3 = (DBObject) new BasicDBObject(
				"$group",
				new BasicDBObject("_id", new BasicDBObject("userid", "$USER_ID"

				)).append("budget", new BasicDBObject("$sum", "$BUDGET_AMOUNT")));
		Aggregation aggregation1 = Aggregation.newAggregation(matchStage1,
				new CustomGroupOperation(myGroup2), new CustomGroupOperation(
						myGroup3));
		AggregationResults<DBObject> output1 = mongoTemplate.aggregate(
				aggregation1, "AMAZE_BUDGET_MASTER", DBObject.class);
		logger.info("total time required for getTotalBudget::"
				+ getTotalBudgetStatistics.totalTimeRequired());
		return output1.getMappedResults();
	}

	public List<DBObject> getIncomeAnalysisOutput(List<String> accountList) {
		Statistics getIncomeAnalysisOutputStatistics = new Statistics();
		
		//List<String> accountList = getAccounts(userId);
		
		Criteria userIdCriteria = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
//		Criteria userIdCriteria = new Criteria("USER_ID").is(userId);
		
		Criteria incomeCriteria = new Criteria("INCOME_EXPENSE").is("INCOME");
		Criteria incomeAndOperator = incomeCriteria.andOperator(userIdCriteria);
		MatchOperation incomeAndUserMatch = Aggregation
				.match(incomeAndOperator);

		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("_id.TRAN_YEAR", -1).append("_id.MONTH", -1));
		
		DBObject incomeAnalysisGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("SOURCE_ACCOUNT_NUMBER",1)
						.append("BUCKET",1)
						.append("INCOME_EXPENSE", 1)
						.append("TRAN_YEAR", 1)
						.append("CATEGORY", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("TRAN_MON", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))
						.append("income",
								new BasicDBObject("$cond", new Object[] {
										new BasicDBObject("$eq", new Object[] {
												"$INCOME_EXPENSE", "INCOME" }),
										"$AMOUNT", 0 })));

		DBObject incomeAnalysisGroup2 = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", new BasicDBObject("CATEGORY",
						"$BUCKET"

				).append("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR")).append("income",
						new BasicDBObject("$sum", "$income")));

		Aggregation incomeAnalysisAggregation = Aggregation.newAggregation(
				incomeAndUserMatch, new CustomGroupOperation(
						incomeAnalysisGroup), new CustomGroupOperation(
						incomeAnalysisGroup2),new CustomGroupOperation(sortCondition));
		AggregationResults<DBObject> analysisOutput = mongoTemplate.aggregate(
				incomeAnalysisAggregation, "AMAZE_TRANSACTION_TIMELINE",
				DBObject.class);
		logger.info("total time required for getIncomeAnalysisOutput::"
				+ getIncomeAnalysisOutputStatistics.totalTimeRequired());
		return analysisOutput.getMappedResults();
	}

	public List<DBObject> getExpenseAnalysisOutput(List<String> accountList) {
		Statistics getExpenseAnalysisOutputStatistics = new Statistics();
		//List<String> accountList = getAccounts(userId);
		
//		Criteria userIdCriteria = new Criteria("USER_ID").is(userId);
		Criteria userIdCriteria = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
		
		Criteria expenseCriteria = new Criteria("INCOME_EXPENSE").is("EXPENSE");
		Criteria expenseAndOperator = expenseCriteria
				.andOperator(userIdCriteria);
		MatchOperation expenseAndUserMatch = Aggregation
				.match(expenseAndOperator);

		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("_id.TRAN_YEAR", -1).append("_id.MONTH", -1));
		
		DBObject expenseAnalysisGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("INCOME_EXPENSE", 1)
						.append("SOURCE_ACCOUNT_NUMBER",1)
						.append("BUCKET",1)
						.append("TRAN_YEAR", 1)
						.append("CATEGORY", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))						
						.append("TRAN_MON", 1)
						.append("expense",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 })));

		DBObject expenseAnalysisGroup2 = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", new BasicDBObject("CATEGORY",
						"$BUCKET"

				).append("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR")).append("spends",
						new BasicDBObject("$sum", "$expense")));

		Aggregation expenseAnalysisAggregation = Aggregation.newAggregation(
				expenseAndUserMatch, new CustomGroupOperation(
						expenseAnalysisGroup), new CustomGroupOperation(
						expenseAnalysisGroup2),new CustomGroupOperation(
								sortCondition));
		AggregationResults<DBObject> expenseAnalysisOutput = mongoTemplate
				.aggregate(expenseAnalysisAggregation,
						"AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		logger.info("total time required for getExpenseAnalysisOutput::"
				+ getExpenseAnalysisOutputStatistics.totalTimeRequired());
		return expenseAnalysisOutput.getMappedResults();
	}


	public List<DBObject> getIncomeAnalysisOutput_CC(List<String> accountList) {
		Statistics getIncomeAnalysisOutputStatistics = new Statistics();
		
	//	List<String> accountList = getAccounts(userId);
		
		Criteria userIdCriteria = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
//		Criteria userIdCriteria = new Criteria("USER_ID").is(userId);
		
		Criteria incomeCriteria = new Criteria("INCOME_EXPENSE").is("INCOME");
		Criteria incomeAndOperator = incomeCriteria.andOperator(userIdCriteria);
		MatchOperation incomeAndUserMatch = Aggregation
				.match(incomeAndOperator);

		DBObject incomeAnalysisGroup = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("SOURCE_ACCOUNT_NUMBER",1)
						.append("INCOME_EXPENSE", 1)
						.append("BUCKET", 1)
						.append("TRAN_YEAR", 1)
						.append("CATEGORY", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("TRAN_MON", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))
						.append("income",
								new BasicDBObject("$cond", new Object[] {
										new BasicDBObject("$eq", new Object[] {
												"$INCOME_EXPENSE", "INCOME" }),
										"$AMOUNT", 0 })));

		DBObject incomeAnalysisGroup2 = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", new BasicDBObject("CATEGORY",
						"$BUCKET"

				).append("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR")
						.append("SOURCE_ACCOUNT_NUMBER","$SOURCE_ACCOUNT_NUMBER")).append("income",
						new BasicDBObject("$sum", "$income"))
						);

		Aggregation incomeAnalysisAggregation = Aggregation.newAggregation(
				incomeAndUserMatch, new CustomGroupOperation(
						incomeAnalysisGroup), new CustomGroupOperation(
						incomeAnalysisGroup2));
		AggregationResults<DBObject> analysisOutput = mongoTemplate.aggregate(
				incomeAnalysisAggregation, "AMAZE_TRANSACTION_TIMELINE",
				DBObject.class);
		logger.info("total time required for getIncomeAnalysisOutputCC::"
				+ getIncomeAnalysisOutputStatistics.totalTimeRequired());
		return analysisOutput.getMappedResults();
	}

	public List<DBObject> getExpenseAnalysisOutput_CC(List<String> accountList) {
		Statistics getExpenseAnalysisOutputStatistics = new Statistics();
	//	List<String> accountList = getAccounts(userId);
		
//		Criteria userIdCriteria = new Criteria("USER_ID").is(userId);
		Criteria userIdCriteria = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
		
		Criteria expenseCriteria = new Criteria("INCOME_EXPENSE").is("EXPENSE");
		Criteria expenseAndOperator = expenseCriteria
				.andOperator(userIdCriteria);
		MatchOperation expenseAndUserMatch = Aggregation
				.match(expenseAndOperator);

		DBObject expenseAnalysisGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("INCOME_EXPENSE", 1)
						.append("SOURCE_ACCOUNT_NUMBER",1)
						.append("TRAN_YEAR", 1)
						.append("CATEGORY", 1)
						.append("BUCKET", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))						
						.append("TRAN_MON", 1)
						.append("expense",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 })));

		DBObject expenseAnalysisGroup2 = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", new BasicDBObject("CATEGORY",
						"$BUCKET"

				).append("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR")
						.append("SOURCE_ACCOUNT_NUMBER","$SOURCE_ACCOUNT_NUMBER")).append("spends",
						new BasicDBObject("$sum", "$expense")));

		Aggregation expenseAnalysisAggregation = Aggregation.newAggregation(
				expenseAndUserMatch, new CustomGroupOperation(
						expenseAnalysisGroup), new CustomGroupOperation(
						expenseAnalysisGroup2));
		AggregationResults<DBObject> expenseAnalysisOutput = mongoTemplate
				.aggregate(expenseAnalysisAggregation,
						"AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		logger.info("total time required for getExpenseAnalysisOutputCC::"
				+ getExpenseAnalysisOutputStatistics.totalTimeRequired());
		return expenseAnalysisOutput.getMappedResults();
	}


	public List<DBObject> getPaymentSpendsOverviewByMonthCC(String SOURCE_ACCOUNT_NUMBER) {
		Statistics getPaymentSpendsOverviewByMonth = new Statistics();
		//DBObject output;
		List<DBObject> outputResults = new ArrayList<DBObject>();
		
	
//		for (int i=0;i< accountList.size(); ++i )
//		{
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("_id.TRAN_YEAR", -1).append("_id.MONTH", -1));
//		Criteria c1 = new Criteria("USER_ID").is(userId);
		//String accountNumber = accountList.get(i);
		Criteria c1 = new Criteria(SOURCE_ACCOUNT_NUMBER);
//		Criteria c2 = new Criteria("SOURCE_SYSTEM_CODE").is("20");
		
		MatchOperation matchStage = Aggregation.match(c1);
		DBObject overviewGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("INCOME_EXPENSE", 1)
						.append("TRAN_YEAR", 1)
						.append("TRAN_MON", 1)
						.append("TRAN_DAY", 1)
						.append("SOURCE_ACCOUNT_NUMBER", 1)
						.append("SOURCE_SYSTEM_CODE", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))
						.append("expense",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 }))
						.append("income",
								new BasicDBObject("$cond", new Object[] {
										new BasicDBObject("$eq", new Object[] {
												"$INCOME_EXPENSE", "INCOME" }),
										"$AMOUNT", 0 })));
		DBObject myGroup1 = (DBObject) new BasicDBObject(
				"$group",
				new BasicDBObject("_id", new BasicDBObject("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR").append("SOURCE_ACCOUNT_NUMBER", "$SOURCE_ACCOUNT_NUMBER"))
						// .append("TRAN_MON_AGG", "$AGG_MON")
						// .append("TRANYEAR", "$TRAN_YEAR"))
						.append("spends", new BasicDBObject("$sum", "$expense"))
						.append("income", new BasicDBObject("$sum", "$income")));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(overviewGroup),
				new CustomGroupOperation(myGroup1), new CustomGroupOperation(
						sortCondition));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		
		logger.info("total time required for getIncomeExpenseOverviewByMonth::"
				+ getPaymentSpendsOverviewByMonth.totalTimeRequired());
		logger.info(output);
		logger.info(output.getMappedResults());
		outputResults.addAll(output.getMappedResults());
		logger.info(outputResults);
		//return output.getMappedResults();
//	}
		return outputResults;
	}

	
	public List<DBObject> getSpendingOverviewByMonthYear(String userId) {
		Statistics getSpendingOverviewByMonthYear = new Statistics();
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("AGG_YEAR", -1).append("AGG_MON", -1));
		Criteria c1 = new Criteria("USER_ID").is(userId);
		MatchOperation matchStage = Aggregation.match(c1);

		DBObject overviewGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("INCOME_EXPENSE", 1)
						.append("TRAN_YEAR", 1)
						.append("TRAN_MON", 1)
						.append("TRAN_DAY", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "TRAN_DATE_TIME"))
						.append("expense",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 }))
						.append("income",
								new BasicDBObject("$cond", new Object[] {
										new BasicDBObject("$eq", new Object[] {
												"$INCOME_EXPENSE", "INCOME" }),
										"$AMOUNT", 0 })));
		DBObject myGroup1 = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", new BasicDBObject("MONTH", "$TRAN_MON"

				).append("TRAN_YEAR", "$TRAN_YEAR")).append("spends",
						new BasicDBObject("$sum", "$expense")).append("income",
						new BasicDBObject("$sum", "$income")));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(sortCondition),
				new CustomGroupOperation(overviewGroup),
				new CustomGroupOperation(myGroup1));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		logger.info("total time required for getIncomeExpenseOverviewByMonth::"
				+ getSpendingOverviewByMonthYear.totalTimeRequired());

		return output.getMappedResults();
	}

	public List<DBObject> getIncomeExpenseOverviewByMonth(List<String> accountList) {
		Statistics getIncomeExpenseOverviewByMonth = new Statistics();
		
		//List<String> accountList = getAccounts(userId);
		
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("_id.TRAN_YEAR", -1).append("_id.MONTH", -1));
//		Criteria c1 = new Criteria("USER_ID").is(userId);
		Criteria c1 = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
		MatchOperation matchStage = Aggregation.match(c1);

		DBObject overviewGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("INCOME_EXPENSE", 1)
						.append("TRAN_YEAR", 1)
						.append("TRAN_MON", 1)
						.append("TRAN_DAY", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))
						.append("expense",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 }))
						.append("income",
								new BasicDBObject("$cond", new Object[] {
										new BasicDBObject("$eq", new Object[] {
												"$INCOME_EXPENSE", "INCOME" }),
										"$AMOUNT", 0 })));
		DBObject myGroup1 = (DBObject) new BasicDBObject(
				"$group",
				new BasicDBObject("_id", new BasicDBObject("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR"))
						// .append("TRAN_MON_AGG", "$AGG_MON")
						// .append("TRANYEAR", "$TRAN_YEAR"))
						.append("spends", new BasicDBObject("$sum", "$expense"))
						.append("income", new BasicDBObject("$sum", "$income")));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(overviewGroup),
				new CustomGroupOperation(myGroup1), new CustomGroupOperation(
						sortCondition));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		logger.info("total time required for getIncomeExpenseOverviewByMonth::"
				+ getIncomeExpenseOverviewByMonth.totalTimeRequired());
		logger.info(output);
		return output.getMappedResults();
	}
	
	public List<DBObject> getPaymentSpendsOverviewByMonth(List<String> accountList) {
		Statistics getPaymentSpendsOverviewByMonth = new Statistics();
		//DBObject output;
		List<DBObject> outputResults = new ArrayList<DBObject>();
		
	
//		for (int i=0;i< accountList.size(); ++i )
//		{
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("_id.TRAN_YEAR", -1).append("_id.MONTH", -1));
//		Criteria c1 = new Criteria("USER_ID").is(userId);
		//String accountNumber = accountList.get(i);
		Criteria c1 = new Criteria("SOURCE_ACCOUNT_NUMBER").in(accountList);
//		Criteria c2 = new Criteria("SOURCE_SYSTEM_CODE").is("20");
		
		MatchOperation matchStage = Aggregation.match(c1);
		DBObject overviewGroup = (DBObject) new BasicDBObject(
				"$project",
				new BasicDBObject("AMOUNT", 1)
						.append("USER_ID", 1)
						.append("INCOME_EXPENSE", 1)
						.append("TRAN_YEAR", 1)
						.append("TRAN_MON", 1)
						.append("TRAN_DAY", 1)
						.append("SOURCE_ACCOUNT_NUMBER", 1)
						.append("SOURCE_SYSTEM_CODE", 1)
						.append("TRAN_DATE_TIME", 1)
						.append("AGG_YEAR",
								new BasicDBObject("$year", "$TRAN_DATE_TIME"))
						.append("AGG_MON",
								new BasicDBObject("$month", "$TRAN_DATE_TIME"))
						.append("expense",
								new BasicDBObject(
										"$cond",
										new Object[] {
												new BasicDBObject(
														"$eq",
														new Object[] {
																"$INCOME_EXPENSE",
																"EXPENSE" }),
												"$AMOUNT", 0 }))
						.append("income",
								new BasicDBObject("$cond", new Object[] {
										new BasicDBObject("$eq", new Object[] {
												"$INCOME_EXPENSE", "INCOME" }),
										"$AMOUNT", 0 })));
		DBObject myGroup1 = (DBObject) new BasicDBObject(
				"$group",
				new BasicDBObject("_id", new BasicDBObject("MONTH", "$AGG_MON")
						.append("TRAN_YEAR", "$AGG_YEAR").append("SOURCE_ACCOUNT_NUMBER", "$SOURCE_ACCOUNT_NUMBER"))
						// .append("TRAN_MON_AGG", "$AGG_MON")
						// .append("TRANYEAR", "$TRAN_YEAR"))
						.append("spends", new BasicDBObject("$sum", "$expense"))
						.append("income", new BasicDBObject("$sum", "$income")));

		Aggregation aggregation = Aggregation.newAggregation(matchStage,
				new CustomGroupOperation(overviewGroup),
				new CustomGroupOperation(myGroup1), new CustomGroupOperation(
						sortCondition));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		
		logger.info("total time required for getIncomeExpenseOverviewByMonth::"
				+ getPaymentSpendsOverviewByMonth.totalTimeRequired());
		logger.info(output);
		logger.info(output.getMappedResults());
		outputResults.addAll(output.getMappedResults());
		logger.info(outputResults);
		//return output.getMappedResults();
//	}
		return outputResults;
	}

	public String getMonthByInt(int monthNumber) {
		String monthArray[] = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
				"JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
		return monthArray[monthNumber - 1];
	}

	public List<DBObject> getTransactionsByMongo(String userId) {
		Statistics getTransactionsByMongoStatistics = new Statistics();
		DBObject sortCondition = (DBObject) new BasicDBObject("$sort",
				new BasicDBObject("_id", -1));
		Criteria userIdCriteria = new Criteria("USER_ID").is(userId);
		MatchOperation userIdMatch = Aggregation.match(userIdCriteria);

		DBObject project = (DBObject) new BasicDBObject("$project",
				new BasicDBObject("DayVal", new BasicDBObject("$dateToString",
						new BasicDBObject("format", "%Y-%m-%d").append("date",
								"$TRAN_DATE_TIME")))
						.append("USER_ID", "$USER_ID")
						.append("TRAN_DATE_TIME", "$TRAN_DATE_TIME")
						.append("ACCOUNT_NO", "$ACCOUNT_NO")
						.append("MERCHANT", "$MERCHANT")
						.append("CATEGORY", "$CATEGORY")
						.append("DEBIT_CREDIT", "$DEBIT_CREDIT")
						.append("AMOUNT", "$AMOUNT")
						.append("BUCKET", "$BUCKET")
						.append("INCOME_EXPENSE", "$INCOME_EXPENSE"));

		DBObject group = (DBObject) new BasicDBObject("$group",
				new BasicDBObject("_id", "$DayVal").append(
						"TRANSACTIONS",
						new BasicDBObject("$push", new BasicDBObject("USER_ID",
								"$USER_ID")
								.append("TRAN_DATE_TIME", "$TRAN_DATE_TIME")
								.append("ACCOUNT_NO", "$ACCOUNT_NO")
								.append("MERCHANT", "$MERCHANT")
								.append("CATEGORY", "$CATEGORY")
								.append("DEBIT_CREDIT", "$DEBIT_CREDIT")
								.append("AMOUNT", "$AMOUNT")
								.append("BUCKET", "$BUCKET")
								.append("INCOME_EXPENSE", "$INCOME_EXPENSE"))));
		Aggregation aggregation = Aggregation.newAggregation(userIdMatch,
				new CustomGroupOperation(project), new CustomGroupOperation(
						group), new CustomGroupOperation(sortCondition));
		AggregationResults<DBObject> output = mongoTemplate.aggregate(
				aggregation, "AMAZE_TRANSACTION_TIMELINE", DBObject.class);
		logger.info("total time required for getTransactionsByMongo::"
				+ getTransactionsByMongoStatistics.totalTimeRequired());
		return output.getMappedResults();

	}

	public int updateCategory(String userId, UpdateCategory updateCategory) {
		DB db = mongoTemplate.getDb();
		DBCollection collection = db
				.getCollection("AMAZE_TRANSACTION_TIMELINE");

		BasicDBObject updateQuery = new BasicDBObject();
		updateQuery.append(
				"$set",
				new BasicDBObject().append("CATEGORY",
						updateCategory.getNewCategory()));

		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("USER_ID", userId);
		searchQuery.append("EVENTID",updateCategory.getEventId());
		searchQuery.append("CATEGORY", updateCategory.getOldCategory());
	//	searchQuery.append("TRAN_DATE_TIME",new Date(updateCategory.getTranDate()));
	//	searchQuery.append("AMOUNT", updateCategory.getTranAmount());
	//	searchQuery.append("SOURCE_ACCOUNT_NUMBER",	updateCategory.getAccountNo());
	//	searchQuery.append("MERCHANT", updateCategory.getMerchant());

		logger.info(searchQuery);
		logger.info(updateQuery);

		WriteResult wrupdate = collection.update(searchQuery, updateQuery,
				false, true);

		if (wrupdate.isUpdateOfExisting()) {
			DBCollection collection1 = db
					.getCollection("AMAZE_CUST_CATEGORIZATION");

			Boolean value = false;
			logger.info("inserting document..");
			BasicDBObject document = new BasicDBObject();
			document.put("USER_ID", userId);
			document.put("PROPOSED_CATEGORY", updateCategory.getNewCategory());
			document.put("EXISTING_CATEGORY", updateCategory.getOldCategory());
			document.put("EVENTID", updateCategory.getEventId());
			Date dateCreated = new Date();
			document.put("DATE_CREATED",dateCreated);
			
			
////			TimeZone.setDefault(TimeZone.getTimeZone("IST"));
//			Calendar instance = Calendar.getInstance();
//			instance.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
//			Date curTime = instance.getTime();
			
//			public static final long HOUR=3600*1000;
//			Date withIST = new Date(System.currentTimeMillis()+330*60*1000);
//			Date withIST = new Date(now.getTime() + 5.5 * 3600 * 1000);
		//	BasicDBObject dateObj = new BasicDBObject("date", now);
			
			
	//		document.put("TRAN_AMOUNT", updateCategory.getTranAmount());
	//		document.put("SOURCE_ACCOUNT_NBR", updateCategory.getAccountNo());
	//		document.put("TRAN_DATE_TIME",
	//				new Date(updateCategory.getTranDate()));
	//		document.put("EXISTING_MERCHANT", updateCategory.getMerchant());
			document.put("PROCESSED", value);

			collection1.insert(document);
			return wrupdate.getN();

		} else {
			return -1;
		}

	}

}
