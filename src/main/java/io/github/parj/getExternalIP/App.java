package io.github.parj.getExternalIP;

import java.util.Optional;

public class App {

    public static void main(String[] args) {
        String port = Optional.ofNullable(System.getenv("port")).orElse("9090");
        MainServer.getInstance()
                .startServer()
                .startReverseProxy(Integer.valueOf(port));
    }

}
