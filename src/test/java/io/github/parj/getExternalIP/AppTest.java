package io.github.parj.getExternalIP;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {
    private static String testServerURL;

    @BeforeAll
    static void startServer() {
        testServerURL = MainServer.getInstance()
                .startServer()
                .startReverseProxy()
                .getReverseProxyURI().toString();
        Unirest.config().verifySsl(false);
    }

    @Test
    void testGetIPJSON() throws JSONException {
        HttpResponse<String> result = Unirest.get(testServerURL)
                .queryString("format", "json")
                .asString();
        String actual = "{\"ip\":\"127.0.0.1\"}";
        JSONAssert.assertEquals(result.getBody(), actual, JSONCompareMode.STRICT);
    }

    @Test
    void testGetIPString() throws JSONException {
        HttpResponse<String> result = Unirest.get(testServerURL)
                .asString();
        String actual = "127.0.0.1";
        assertEquals(result.getBody(), actual);
    }
}