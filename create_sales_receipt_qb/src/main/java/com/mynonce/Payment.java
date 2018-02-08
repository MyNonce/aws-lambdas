package com.mynonce;

import com.amazonaws.services.dynamodbv2.document.Item;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by jameswarren on 2/7/18.
 */
public class Payment {
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

	public String getPool() {
		return pool;
	}

	public Date getTime() {
		return time;
	}

	public BigDecimal getUsd() {
		return usd;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Item getItem() {
		return item;
	}
}
