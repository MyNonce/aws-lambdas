package com.mynonce;

import com.amazonaws.services.dynamodbv2.document.Item;

import java.math.BigDecimal;

/**
 * Created by jameswarren on 2/7/18.
 */
public class OAuthDynamoDB {

	private final String refresh_token;
	private final String access_token;
	private final BigDecimal x_refresh_token_expires_in;
	private final BigDecimal expires_in;

	public OAuthDynamoDB(Item item) {
		this.refresh_token = item.getString("refresh_token");
		this.access_token = item.getString("access_token");
		this.x_refresh_token_expires_in = item.getNumber("x_refresh_token_expires_in");
		this.expires_in = item.getNumber("expires_in");
	}

	public String getRefresh_token() {
		return refresh_token;
	}

	public String getAccess_token() {
		return access_token;
	}

	public BigDecimal getX_refresh_token_expires_in() {
		return x_refresh_token_expires_in;
	}

	public BigDecimal getExpires_in() {
		return expires_in;
	}
}
