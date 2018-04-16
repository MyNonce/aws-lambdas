package com.mynonce;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.mynonce.qbo.DataServiceFactory;
import com.mynonce.qbo.OAuth2PlatformClientFactory;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CreateSalesReceipts implements RequestHandler<Object, Object> {


	private Table payments_table;
	private Table qbAuth_table;
	private Context context;
	private DataService service;
	private OAuth2PlatformClient client;
	private OAuth2Config oauth2Config;

	private void initDynamoDbClient() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
				.withRegion(Regions.US_EAST_1).build();
		DynamoDB dynamoDB = new DynamoDB(client);

		payments_table = dynamoDB.getTable("payments");
		qbAuth_table = dynamoDB.getTable("qbAuth");
	}

	@Override
	public Object handleRequest(Object input, Context context) {

		this.context = context;
		initDynamoDbClient();
		// Check for new payments
		List<Payment> payments = getNewPayments();

		if (payments.isEmpty())
			return null;

		String access_token = refreshAccessToken(context);
		if (access_token == null) {
			context.getLogger().log("Failed to refresh access_token"+"\n");
			return null;
		}
		try {
			String company_id = System.getenv("company_id");
			context.getLogger().log("COMPANY_ID = " + company_id +"\n");
			service = DataServiceFactory.getDataService(access_token, company_id);

			// Create Sales Receipt
			List<SalesReceipt> salesReceipts = AddSalesReceipt(payments);
			for (SalesReceipt salesReceipt : salesReceipts) {
				SalesReceipt savedSalesReceipt = service.add(salesReceipt);
			}

			// Update Payment to record sales receipt was update successful
			updateNewPayments(payments);
		} catch (FMSException e) {
			handleError(e);
		}


		return null;
	}

	private String refreshAccessToken(Context context) {

		try {
			OAuthDynamoDB oAuthDocument = getOAuthDocument();
			if (oAuthDocument == null) {
				// TODO check to see if we need to refresh access_token
				// TODO check to see if the refresh_token has expired
				return null;
			}

			OAuth2PlatformClientFactory factory = new OAuth2PlatformClientFactory();

			OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
			String refreshToken = oAuthDocument.getRefresh_token();
			context.getLogger().log("REFRESH TOKEN = " + refreshToken+"\n");
			BearerTokenResponse bearerTokenResponse = client.refreshToken(refreshToken);

			qbAuth_table.deleteItem("refresh_token", refreshToken);
			updateOAuthDocument(bearerTokenResponse);

			return bearerTokenResponse.getAccessToken();
		}
		catch (Exception ex) {
			handleError(ex);
		}
		return null;
	}

	private void updateOAuthDocument(BearerTokenResponse tokenResponse) {
		Item qbAuthItem = new Item();
		qbAuthItem.withString("refresh_token", tokenResponse.getRefreshToken());
		qbAuthItem.withString("access_token", tokenResponse.getAccessToken());
		qbAuthItem.withString("token_type", tokenResponse.getTokenType());
		qbAuthItem.withNumber("x_refresh_token_expires_in", tokenResponse.getXRefreshTokenExpiresIn());
		qbAuthItem.withNumber("expires_in", tokenResponse.getExpiresIn());
		qbAuth_table.putItem(qbAuthItem);
	}

	private void updateNewPayments(List<Payment> payments) {
		for (Payment payment : payments) {
			payment.getItem().withBoolean("receipt", true);
			payments_table.putItem(payment.getItem());
		}
	}

	private List<SalesReceipt> AddSalesReceipt(List<Payment> payments) {
		List<SalesReceipt> receipts = new ArrayList<>();
		for (Payment payment : payments) {
			SalesReceipt salesReceipt = new SalesReceipt();
			salesReceipt.setAutoDocNumber(true);
			salesReceipt.setTxnDate(payment.getTime());

			salesReceipt.setLine(createLineItem(payment));
			salesReceipt.setDepositToAccountRef(getAccountReference(payment));
			salesReceipt.setCustomerRef(getCustomerReference(payment));
			receipts.add(salesReceipt);
		}
		return receipts;
	}

	private ReferenceType getCustomerReference(Payment payment) {
		// TODO lookup the Customers

		if (payment.getPool().equalsIgnoreCase("nicehash")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Nicehash");
			itemRef.setValue("17");
			return itemRef;
		}
		if (payment.getPool().equalsIgnoreCase("nanopool")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Nano Pool");
			itemRef.setValue("18");
			return itemRef;
		}
		if (payment.getPool().equalsIgnoreCase("siamining")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Siamining");
			itemRef.setValue("25");
			return itemRef;
		}
		return null;
	}

	private ReferenceType getAccountReference(Payment payment) {
		// TODO lookup the Accounts

		if (payment.getPool().equalsIgnoreCase("nicehash")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin Wallets:Nicehash Wallet");
			itemRef.setValue("43");
			return itemRef;
		}
		if (payment.getPool().equalsIgnoreCase("nanopool")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin Wallets:Coinbase Wallet");
			itemRef.setValue("73");
			return itemRef;
		}
		if (payment.getPool().equalsIgnoreCase("siamining")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin Wallets:Siacoin Wallet");
			itemRef.setValue("70");
			return itemRef;
		}
		return null;
	}

	private List<Line> createLineItem(Payment payment) {
		Line lineItem = new Line();
		lineItem.setLineNum(new BigInteger("1"));
		if (payment.getPool().equalsIgnoreCase("nicehash")){
			lineItem.setDescription("Hashpower sold to the Nicehash Pool");
		} else if (payment.getPool().equalsIgnoreCase("siacoin")) {
			lineItem.setDescription("Siacoin Mining");
		} else if (payment.getPool().equalsIgnoreCase("nanopool")) {
			lineItem.setDescription("Ethereum Mining");
		}
		lineItem.setAmount(payment.getAmount().multiply(payment.getUsd()));
		lineItem.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);

		lineItem.setSalesItemLineDetail(createSalesItemLineDetail(payment));

		List<Line> lineItems = new ArrayList<Line>();
		lineItems.add(lineItem);

		return lineItems;
	}

	private SalesItemLineDetail createSalesItemLineDetail(Payment payment) {
		SalesItemLineDetail salesItemLineDetail = new SalesItemLineDetail();

		salesItemLineDetail.setItemRef(getItemReference(payment));

		salesItemLineDetail.setServiceDate(payment.getTime());
		salesItemLineDetail.setUnitPrice(payment.getUsd());
		salesItemLineDetail.setQty(payment.getAmount());
		return salesItemLineDetail;
	}

	private ReferenceType getItemReference(Payment payment) {
		// TODO lookup the items

		if (payment.getPool().equalsIgnoreCase("nicehash")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Hashing Power:Nicehash");
			itemRef.setValue("4");
			return itemRef;
		}
		if (payment.getPool().equalsIgnoreCase("nanopool")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Hashing Power:Nicehash");
			itemRef.setValue("11");
			return itemRef;
		}
		if (payment.getPool().equalsIgnoreCase("siamining")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin:Siacoin");
			itemRef.setValue("10");
			return itemRef;
		}
		return null;
	}

	private List<Payment> getNewPayments() {
		ItemCollection<ScanOutcome> items = payments_table.scan(new ScanFilter("receipt").notExist());

		List<Payment> payments = new ArrayList<>();
		Iterator<Item> iterator = items.iterator();
		while (iterator.hasNext()) {
			Item item = iterator.next();
			payments.add(new Payment(item));
			context.getLogger().log(item.toJSONPretty());
		}
		return payments;
	}

	private OAuthDynamoDB getOAuthDocument() {
		ItemCollection<ScanOutcome> items = qbAuth_table.scan(new ScanFilter("refresh_token").exists());
		if (!items.iterator().hasNext()) {
			return null;
		}

		return new OAuthDynamoDB(items.iterator().next());
	}

	private void handleError(FMSException e) {
		context.getLogger().log("ERROR:" + e.getMessage());
		for (StackTraceElement traceElement : e.getStackTrace())
			context.getLogger().log("\tat " + traceElement + "\n");
	}

	private void handleError(Exception e) {
		context.getLogger().log("ERROR:" + e.getMessage());
		for (StackTraceElement traceElement : e.getStackTrace())
			context.getLogger().log("\tat " + traceElement + "\n");
	}

}
