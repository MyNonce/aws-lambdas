package com.mynonce;

import com.amazonaws.services.dynamodbv2.document.Item;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by jameswarren on 4/22/18.
 */
public class ExchangeItem {
	private final String product;
	private final Date time;
	private final BigDecimal exchangeValue;
	private final BigDecimal mindedValue;
	private final String payments;
	private final String orders;
	private final Item item;

	public ExchangeItem(Item item) {
		this.item = item;
		this.product = item.getString("product");
		this.time = new Date(item.getLong("time") * 1000);
		this.exchangeValue = item.getNumber("ExchangeValue");
		this.mindedValue = item.getNumber("MinedValue");
		this.payments = item.getJSON("Payments");
		this.orders = item.getJSON("Orders");
	}

	public String getProduct() {
		return product;
	}

	public Date getTime() {
		return time;
	}

	public BigDecimal getExchangeValue() {
		return exchangeValue;
	}

	public BigDecimal getMindedValue() {
		return mindedValue;
	}

	public String getPayments() {
		return payments;
	}

	public String getOrders() {
		return orders;
	}

	public Item getItem() {
		return this.item;
	}
}
