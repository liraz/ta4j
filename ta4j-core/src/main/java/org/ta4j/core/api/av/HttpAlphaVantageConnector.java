package org.ta4j.core.api.av;

import okhttp3.*;
import org.patriques.ApiConnector;
import org.patriques.input.ApiParameter;
import org.patriques.input.ApiParameterBuilder;
import org.patriques.output.AlphaVantageException;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class HttpAlphaVantageConnector implements ApiConnector {
	private static final String BASE_URL = "https://www.alphavantage.co/query?";

	private final OkHttpClient client;
	private final String apiKey;
	private final int timeOut;

	/**
	 * Creates an AlphaVantageConnector.
	 *
	 * @param apiKey the secret key to access the api.
	 * @param timeOut the timeout for when reading the connection should give up.
	 */
	public HttpAlphaVantageConnector(String apiKey, int timeOut) {
		client = getUnsafeOkHttpClient(timeOut);

		this.apiKey = apiKey;
		this.timeOut = timeOut;
	}

	@Override
	public String getRequest(ApiParameter... apiParameters) {
		String params = getParameters(apiParameters);
		try {

			Request request = new Request.Builder()
					.url(BASE_URL + params)
					.build();
			Response response = client.newCall(request).execute();
			ResponseBody body = response.body();
			if(body == null)
				throw new IllegalStateException();

			InputStreamReader inputStream = new InputStreamReader(body.byteStream(), "UTF-8");
			BufferedReader bufferedReader = new BufferedReader(inputStream);
			StringBuilder responseBuilder = new StringBuilder();

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				responseBuilder.append(line);
			}
			bufferedReader.close();
			return responseBuilder.toString();
		} catch (IOException e) {
			throw new AlphaVantageException("failure sending request", e);
		}
	}

	/**
	 * Builds up the url query from the api parameters used to append to the base url.
	 *
	 * @param apiParameters the api parameters used in the query
	 * @return the query string to use in the url
	 */
	private String getParameters(ApiParameter... apiParameters) {
		ApiParameterBuilder urlBuilder = new ApiParameterBuilder();
		for (ApiParameter parameter : apiParameters) {
			urlBuilder.append(parameter);
		}
		urlBuilder.append("apikey", apiKey);
		return urlBuilder.getUrl();
	}

	private OkHttpClient getUnsafeOkHttpClient(int timeOut) {
		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
						}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
						}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
			builder.hostnameVerifier((hostname, session) -> true);

			return builder
					.connectTimeout(timeOut, TimeUnit.MILLISECONDS)
					.writeTimeout(timeOut, TimeUnit.MILLISECONDS)
					.readTimeout(timeOut, TimeUnit.MILLISECONDS)
					.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
