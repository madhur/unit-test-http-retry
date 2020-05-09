package in.co.madhur;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.SocketException;

@Configuration
@EnableAutoConfiguration
public class ApplicationConfig {
    private final int CONN_TIMEOUT_MS = 1000;
    private final int CONN_REQUEST_TIMEOUT_MS = 60000;
    private final int CONN_SOCKET_TIMEOUT_MS = 60000;
    private final int MAX_RETRIES = 3;
    private final int CONN_POOL_DEFAULT_MAX = 40;
    private final int CONN_POOL_DEFAULT_MAX_PER_ROUTE = 20;

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    @Bean
    public HttpClient retryHttpClient() {

        connectionManager
                .setDefaultMaxPerRoute(CONN_POOL_DEFAULT_MAX_PER_ROUTE);

        connectionManager.setMaxTotal(CONN_POOL_DEFAULT_MAX);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONN_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONN_REQUEST_TIMEOUT_MS)
                .setSocketTimeout(CONN_SOCKET_TIMEOUT_MS).build();
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler((exception, executionCount, context) -> executionCount <= MAX_RETRIES
                        && exception instanceof SocketException).setServiceUnavailableRetryStrategy(
                        new ServiceUnavailableRetryStrategy() {

                            @Override
                            public long getRetryInterval() {
                                return 500;
                            }

                            @Override
                            public boolean retryRequest(HttpResponse response,
                                                        int executionCount, HttpContext context) {
                                return executionCount <= MAX_RETRIES && response
                                        .getStatusLine()
                                        .getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
                            }
                        })
                .build();
    }

}