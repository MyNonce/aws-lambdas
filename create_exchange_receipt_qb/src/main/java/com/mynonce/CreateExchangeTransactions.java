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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CreateExchangeTransactions implements RequestHandler<Object, Object> {


	private Table gdax_exchange;
	private Table qbAuth_table;
	private Context context;
	private DataService service;
	private OAuth2PlatformClient client;
	private OAuth2Config oauth2Config;

	private void initDynamoDbClient() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
				.withRegion(Regions.US_EAST_1).build();
		DynamoDB dynamoDB = new DynamoDB(client);

		gdax_exchange = dynamoDB.getTable("gdax_exchange");
		qbAuth_table = dynamoDB.getTable("qbAuth");
	}

	@Override
	public Object handleRequest(Object input, Context context) {

		this.context = context;
		initDynamoDbClient();
		// Check for new payments
		List<ExchangeItem> exchangeItems = getExchangeItems();

		if (exchangeItems.isEmpty())
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

			// Create Transfer from wallet (uses the product "ETH", "BTC" to determin wallet)
			List<Transfer> transfers = AddTransfers(exchangeItems);
			for (Transfer transfer : transfers) {
				service.add(transfer);
			}

			// Update Payment to record sales receipt was update successful
			updateExchangeItems(exchangeItems);
		} catch (FMSException e) {
			handleError(e);
		}


		return null;
	}

	private void updateExchangeItems(List<ExchangeItem> exchangeItems) {
		for (ExchangeItem exchangeItem : exchangeItems) {
			exchangeItem.getItem().withBoolean("receipt", true);
			gdax_exchange.putItem(exchangeItem.getItem());
		}
	}

	private List<Transfer> AddTransfers(List<ExchangeItem> exchangeItems) {
		List<Transfer> transfers = new ArrayList<>();
		for (ExchangeItem exchangeItem : exchangeItems) {
			Transfer transfer = new Transfer();
			transfer.setTxnDate(exchangeItem.getTime());

			transfer.setFromAccountRef(getFromAccountReference(exchangeItem));
			transfer.setToAccountRef(getToAccountReference(exchangeItem));

			transfer.setAmount(exchangeItem.getMindedValue());

			transfer.setDomain("QBO");

			transfers.add(transfer);
		}
		return transfers;
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

	private ReferenceType getFromAccountReference(ExchangeItem exchangeItem) {
		// TODO lookup the Accounts

		if (exchangeItem.getProduct().equalsIgnoreCase("BTC")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin Wallets:Nicehash Wallet");
			itemRef.setValue("43");
			return itemRef;
		}
		if (exchangeItem.getProduct().equalsIgnoreCase("ETH")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin Wallets:Coinbase Wallet");
			itemRef.setValue("73");
			return itemRef;
		}
		return null;
	}

	private ReferenceType getToAccountReference(ExchangeItem exchangeItem) {
		// always tranfering to GDAX Coin Exchange
		ReferenceType itemRef = new ReferenceType();
		itemRef.setName("GDAX Coin Exchange");
		itemRef.setValue("74");
		return itemRef;
	}

	private List<ExchangeItem> getExchangeItems() {
		ItemCollection<ScanOutcome> items = gdax_exchange.scan(new ScanFilter("receipt").notExist());

		List<ExchangeItem> exchangeItems = new ArrayList<>();
		Iterator<Item> iterator = items.iterator();
		while (iterator.hasNext()) {
			Item item = iterator.next();
			exchangeItems.add(new ExchangeItem(item));
			context.getLogger().log(item.toJSONPretty());
		}
		return exchangeItems;
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
