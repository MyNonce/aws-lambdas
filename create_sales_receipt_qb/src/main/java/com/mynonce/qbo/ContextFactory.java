package com.mynonce.qbo;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.IAuthorizer;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.security.OAuthAuthorizer;

import java.util.Map;

/**
 * 
 * @author dderose
 *
 */

public class ContextFactory {

	private static final String companyID = "company_id";
	private static final String consumerKey = "consumer_key";
	private static final String consumerSecret = "consumer_secret";
	private static final String accessToken = "oauth_accessToken";
	private static final String accessTokenSecret = "oauth_accessTokenSecret";
	private static final String bearerToken = "oauth2_accessToken";
	

	/**
	 * Initializes Context for a given app/company profile
	 * 
	 * @return
	 * @throws FMSException
	 *
	 */
	public static Context getContext() throws FMSException {
		//create oauth object
		IAuthorizer oauth;
		if(System.getenv("oauth_type").equals("1")) {
			oauth = new OAuthAuthorizer(System.getenv(consumerKey), System.getenv(consumerSecret), System.getenv(accessToken), System.getenv(accessTokenSecret));
		} else {
			oauth = new OAuth2Authorizer(System.getenv(bearerToken));
		}
		//create context
		Context context = new Context(oauth, ServiceType.QBO, System.getenv(companyID));

		return context;
	}
}
