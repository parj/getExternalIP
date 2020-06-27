package io.github.parj.getExternalIP;

import io.muserver.*;
import io.muserver.murp.Murp;
import io.muserver.murp.ReverseProxyBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * This class holds the starting of the web server and the reverse proxy server.
 * To use this, the code should be used as follows:
 * <pre>
 *     {@code
 *      MainServer.getInstance()
 *                 .startServer()
 *                 .startReverseProxy(9998);
 *     }
 * </pre>
 *
 * If you want it to start the reverse proxy on a random port, remove the port. Example
 * <pre>
 *     {@code
 *      MainServer.getInstance()
 *                 .startServer()
 *                 .startReverseProxy();
 *     }
 * </pre>
 */
public class MainServer {
    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);
    private URI reverseProxyURI;

    private static MainServer INSTANCE;
    private MuServer targetServer;
    private MuServer reverseProxy;

    private MainServer() { }

    /**
     * Singleton constructor
     * @return Instance of MainServer
     */
    public static MainServer getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new MainServer();
        }

        return INSTANCE;
    }

    /**
     * Start the initial webserver
     * @return This instance of class
     */
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

    /**
     * Starts the reverse proxy server
     * @return This instance of class
     */
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
                    logger.info("Returned " + clientResponse.status() + " in " + durationMillis + "ms");
                });
        return builder;
    }

    /**
     * Starts the reverse proxy using the specified port
     * @param port Port to start proxy ons
     */
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

    /**
     * Gets the url the reverse proxy has started on. This is used for junit testing.
     * @return The url of the reverse proxy port
     */
    public URI getReverseProxyURI() {
        return this.reverseProxyURI;
    }
}
