package com.mynonce.qbo;

import com.amazonaws.services.lambda.runtime.Context;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;

/**
 * Created by jameswarren on 2/7/18.
 */
public class OAuth2PlatformClientFactory {

	OAuth2PlatformClient client;
	OAuth2Config oauth2Config;

	public void init() {
		if (client == null) {
			// intitialize a single thread executor, this will ensure only one thread processes the queue
			oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(System.getenv("OAuth2AppClientId"), System.getenv("OAuth2AppClientSecret")) //set client id, secret
					.callDiscoveryAPI(Environment.PRODUCTION) // call discovery API to populate urls
					.buildConfig();
			client = new OAuth2PlatformClient(oauth2Config);
		}
	}


	public OAuth2PlatformClient getOAuth2PlatformClient()  {
		init();
		return client;
	}

	public OAuth2Config getOAuth2Config()  {
		return oauth2Config;
	}

	public String getPropertyValue(String proppertyName) {
		return System.getenv(proppertyName);
	}

	public void showConfig(Context context) {
		context.getLogger().log(" endpoint : " + oauth2Config.getIntuitBearerTokenEndpoint() + "\n");
		context.getLogger().log(" clientid : " + oauth2Config.getClientId() + "\n");
		context.getLogger().log(" secret : " + oauth2Config.getClientSecret() + "\n");
	}
}