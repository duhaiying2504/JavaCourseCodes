package io.github.kimmking.gateway.outbound.httpclient5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * httpclient5 同步、请求工具类
 * </p>
 * 
 * @author duhaiying
 */
public class HttpClientUtil {

	private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

	// request configuration
	private static final RequestConfig defaultRequestConfig = RequestConfig.custom()
    		.setConnectTimeout(Timeout.ofSeconds(5))
    		.setResponseTimeout(Timeout.ofSeconds(5))
    		.setConnectionRequestTimeout(Timeout.ofSeconds(5))
	        .build();
	
	private static PoolingHttpClientConnectionManager connManager = null;
	
	static {
		
		 // SSL context for secure connections can be created either based on
       // system or application specific properties.
       final SSLContext sslcontext = SSLContexts.createSystemDefault();
		
	    // Create a registry of custom connection socket factories for supported
       // protocol schemes.
       final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
           .register("http", PlainConnectionSocketFactory.INSTANCE)
           .register("https", new SSLConnectionSocketFactory(sslcontext))
           .build();
		
       // Create a connection manager with custom configuration.
       connManager = new PoolingHttpClientConnectionManager(
               socketFactoryRegistry, 
               PoolConcurrencyPolicy.STRICT, 
               PoolReusePolicy.LIFO, 
               TimeValue.ofMinutes(5),
               null, null, null);

       // Configure total max or per route limits for persistent connections
       // that can be kept in the pool or leased by the connection manager.
       connManager.setMaxTotal(1000);
       connManager.setDefaultMaxPerRoute(100);
      // connManager.setMaxPerRoute(new HttpRoute(new HttpHost("somehost", 80)), 20);
		
	}

	public static String get(String uri) {

		HttpGet httpGet = new HttpGet(uri);

		return request(httpGet);
	}

	public static String post(String uri, Map<String, String> formParams) {

		HttpPost httpPost = new HttpPost(uri);

		if (formParams != null) {
			List<NameValuePair> nvps = new ArrayList<>();
			formParams.forEach((key, value) -> {
				nvps.add(new BasicNameValuePair(key, value));
			});
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		}

		return request(httpPost);
	}

	public static String request(ClassicHttpRequest request) {

		try (CloseableHttpClient httpclient = HttpClients.custom()
				.setConnectionManager(connManager)
				.setDefaultRequestConfig(defaultRequestConfig)
				.build()) {

			CloseableHttpResponse response = httpclient.execute(request);
			int status = response.getCode();
			if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
				HttpEntity entity = response.getEntity();
				return entity != null ? EntityUtils.toString(entity) : "";
			} else {
				log.warn("Unexpected response, uri: {}, status: {}", request.getUri(), status);
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		return null;
	}
	

}