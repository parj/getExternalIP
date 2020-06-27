package io.github.parj.getExternalIP;

import io.muserver.*;
import io.muserver.murp.Murp;
import io.muserver.murp.ReverseProxyBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class MainServer {
    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);
    private URI reverseProxyURI;

    private static MainServer INSTANCE;
    private MuServer targetServer;
    private MuServer reverseProxy;

    private MainServer() { }

    public static MainServer getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new MainServer();
        }

        return INSTANCE;
    }

    public MainServer startServer() {
        targetServer = MuServerBuilder.httpServer()
                .withHttp2Config(Http2ConfigBuilder.http2EnabledIfAvailable())
                .addHandler(Method.GET, "/", (request, response, pathParams) -> {
                    grabAndFormatIP(request.query().get("format"),
                            request.headers().get(HeaderNames.X_FORWARDED_FOR),
                            response);
                })
                .start();

        logger.info("Target started server at " + targetServer.uri());
        return this;
    }

    public MainServer startReverseProxy() {
        reverseProxy = MuServerBuilder.httpsServer()
                .withHttp2Config(Http2ConfigBuilder.http2EnabledIfAvailable())
                .addHandler(
                        buildReverseProxy(targetServer)
                )
                .start();
        reverseProxyURI = reverseProxy.uri();
        logger.info("Reverse proxy started at " + reverseProxy.uri());
        return this;
    }

    private void grabAndFormatIP(String format, String xForwardFor, MuResponse response) {
        // Wikipedia page: https://en.wikipedia.org/wiki/X-Forwarded-For
        String userIP = xForwardFor.split(",")[0];

        if (format != null && format.toLowerCase().equals("json")) {
            response.contentType("application/json");
            String jsonString = new JSONObject()
                    .put("ip", userIP)
                    .toString();
            response.write(jsonString);
        } else {
            response.contentType("text/plain");
            response.write(userIP);
        }
    }

    private ReverseProxyBuilder buildReverseProxy(MuServer server) {
        ReverseProxyBuilder builder = ReverseProxyBuilder.reverseProxy()
                .withUriMapper(request -> {
                    String pathAndQuery = Murp.pathAndQuery(request.uri());
                    return server.uri().resolve(pathAndQuery);
                })
                .sendLegacyForwardedHeaders(true) // Adds X-Forwarded-*
                .withViaName("reverseproxy")
                .proxyHostHeader(false)
                .addProxyCompleteListener((clientRequest, clientResponse, targetUri, durationMillis) -> {
                    logger.info("Proxied " + clientRequest + " to " + targetUri +
                            " and returned " + clientResponse.status() + " in " + durationMillis + "ms");
                });
        return builder;
    }

    public void startReverseProxy(int port) {
        reverseProxy = MuServerBuilder.httpsServer()
                .withHttp2Config(Http2ConfigBuilder.http2EnabledIfAvailable())
                .addHandler(
                        buildReverseProxy(targetServer)
                )
                .withHttpsPort(port)
                .start();
        logger.info("Reverse proxy started at " + reverseProxy.uri());
    }

    public URI getReverseProxyURI() {
        return this.reverseProxyURI;
    }
}
