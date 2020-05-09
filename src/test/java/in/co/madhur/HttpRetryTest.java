package in.co.madhur;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=ApplicationConfig.class)
@SpringBootTest
public class HttpRetryTest {

    @Autowired
    private HttpClient retryHttpClient;

    private String httpScheme = "http";
    private String localHost = "localhost";
    private int port = 8089;
    private String testResource = "/my/resource";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port); // No-args constructor defaults to port 8080


    @Test
    public void testFourRetryFor503StatusCode() {
        stubFor(get(urlEqualTo(testResource)).willReturn(aResponse().withStatus(503)));

        try {
            URI uri = new URIBuilder()
                    .setScheme(httpScheme)
                    .setHost(localHost)
                    .setPort(port)
                    .setPath(testResource)
                    .build();
            retryHttpClient.execute(new HttpGet(uri));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            Assert.fail();
        }
        verify(1 + 3, getRequestedFor(urlEqualTo(testResource)));
    }

    @Test
    public void testZeroRetryFor502StatusCode() {
        stubFor(get(urlEqualTo(testResource)).willReturn(aResponse().withStatus(502)));

        try {
            URI uri = new URIBuilder()
                    .setScheme(httpScheme)
                    .setHost(localHost)
                    .setPort(port)
                    .setPath(testResource)
                    .build();
            retryHttpClient.execute(new HttpGet(uri));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            Assert.fail();
        }
        verify(1, getRequestedFor(urlEqualTo(testResource)));
    }

    @Test
    public void testFourRetryForSocketException() {
        stubFor(get(urlEqualTo(testResource)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        try {
            URI uri = new URIBuilder()
                    .setScheme(httpScheme)
                    .setHost(localHost)
                    .setPort(port)
                    .setPath(testResource)
                    .build();
            retryHttpClient.execute(new HttpGet(uri));
        } catch (IOException e) {
            // IOException is expected
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            Assert.fail();
        }
        verify(1 + 3, getRequestedFor(urlEqualTo(testResource)));
    }
}
