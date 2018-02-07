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
import com.mynonce.qbo.DataServiceFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class CreateSalesReceipts implements RequestHandler<Object, Object> {

	private class Payment {
		private final String pool;
		private final Date time;
		private final BigDecimal usd;
		private final BigDecimal fee;
		private final BigDecimal amount;
		private final Item item;

		public Payment(Item item) {
			this.item = item;
			this.pool = item.getString("pool");
			this.time = new Date(item.getLong("time") * 1000);
			this.usd = item.getNumber("usd");
			this.amount = item.getNumber("amount");
			this.fee = item.getNumber("fee");
		}
	}

	private Table table;
	private Context context;
	private DataService service;

	private void initDynamoDbClient() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
				.withRegion(Regions.US_EAST_1).build();
		DynamoDB dynamoDB = new DynamoDB(client);

		table = dynamoDB.getTable("payments");
	}

	@Override
	public Object handleRequest(Object input, Context context) {

		this.context = context;
		initDynamoDbClient();
		// Check for new payments
		List<Payment> payments = getNewPayments();

		if (payments.isEmpty())
			return null;

		try {
			service = DataServiceFactory.getDataService();

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

	private void updateNewPayments(List<Payment> payments) {
		for (Payment payment : payments) {
			payment.item.withBoolean("receipt", true);
			table.putItem(payment.item);
		}
	}

	private List<SalesReceipt> AddSalesReceipt(List<Payment> payments) {
		List<SalesReceipt> receipts = new ArrayList<>();
		for (Payment payment : payments) {
			SalesReceipt salesReceipt = new SalesReceipt();
			salesReceipt.setAutoDocNumber(true);
			salesReceipt.setTxnDate(payment.time);

			salesReceipt.setLine(createLineItem(payment));
			salesReceipt.setDepositToAccountRef(getAccountReference(payment));
			salesReceipt.setCustomerRef(getCustomerReference(payment));
			receipts.add(salesReceipt);
		}
		return receipts;
	}

	private ReferenceType getCustomerReference(Payment payment) {
		// TODO lookup the Customers

		if (payment.pool.equalsIgnoreCase("nicehash")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Nicehash");
			itemRef.setValue("17");
			return itemRef;
		}
		if (payment.pool.equalsIgnoreCase("siamining")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Siamining");
			itemRef.setValue("25");
			return itemRef;
		}
		return null;
	}

	private ReferenceType getAccountReference(Payment payment) {
		// TODO lookup the Accounts

		if (payment.pool.equalsIgnoreCase("nicehash")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin Wallets:Nicehash Wallet");
			itemRef.setValue("43");
			return itemRef;
		}
		if (payment.pool.equalsIgnoreCase("siamining")){
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
		if (payment.pool.equalsIgnoreCase("nicehash")){
			lineItem.setDescription("Hashpower sold to the Nicehash Pool");
		} else if (payment.pool.equalsIgnoreCase("siacoin")) {
			lineItem.setDescription("Siacoin Mining");
		}
		lineItem.setAmount(payment.amount.multiply(payment.usd));
		lineItem.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);

		lineItem.setSalesItemLineDetail(createSalesItemLineDetail(payment));

		List<Line> lineItems = new ArrayList<Line>();
		lineItems.add(lineItem);

		return lineItems;
	}

	private SalesItemLineDetail createSalesItemLineDetail(Payment payment) {
		SalesItemLineDetail salesItemLineDetail = new SalesItemLineDetail();

		salesItemLineDetail.setItemRef(getItemReference(payment));

		salesItemLineDetail.setServiceDate(payment.time);
		salesItemLineDetail.setUnitPrice(payment.usd);
		salesItemLineDetail.setQty(payment.amount);
		return salesItemLineDetail;
	}

	private ReferenceType getItemReference(Payment payment) {
		// TODO lookup the items

		if (payment.pool.equalsIgnoreCase("nicehash")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Hashing Power:Nicehash");
			itemRef.setValue("4");
			return itemRef;
		}
		if (payment.pool.equalsIgnoreCase("siamining")){
			ReferenceType itemRef = new ReferenceType();
			itemRef.setName("Coin:Siacoin");
			itemRef.setValue("10");
			return itemRef;
		}
		return null;
	}

	private List<Payment> getNewPayments() {
		ItemCollection<ScanOutcome> items = table.scan(new ScanFilter("receipt").notExist());

		List<Payment> payments = new ArrayList<>();
		Iterator<Item> iterator = items.iterator();
		while (iterator.hasNext()) {
			Item item = iterator.next();
			payments.add(new Payment(item));
			context.getLogger().log(item.toJSONPretty());
		}
		return payments;
	}

	private void handleError(FMSException e) {
		context.getLogger().log("ERROR:" + e.getMessage());
		for (StackTraceElement traceElement : e.getStackTrace())
			context.getLogger().log("\tat " + traceElement + "\n");
	}
}
