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

	/**
	 * Initializes Context for a given app/company profile
	 * 
	 * @return
	 * @throws FMSException
	 *
	 */
	public static Context getContext(String bearerToken, String companyID) throws FMSException {
		IAuthorizer oauth = new OAuth2Authorizer(bearerToken);
		Context context = new Context(oauth, ServiceType.QBO, companyID);

		return context;
	}
}
