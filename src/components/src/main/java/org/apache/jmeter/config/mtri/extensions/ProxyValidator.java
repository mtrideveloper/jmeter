package org.apache.jmeter.config.mtri.extensions;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.jmeter.config.mtri.model.MyProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyValidator {
    private static final Logger log = LoggerFactory.getLogger(ProxyValidator.class);

    private static final String TEST_URL = "https://api.ipify.org";
    private static final int MAX_CONCURRENT = 20;
    private static final int TIMEOUT = 15000;

    public List<MyProxy> validateProxies(List<MyProxy> proxies) {
        if (proxies.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Starting validation of " + proxies.size() + " proxy...");
        List<MyProxy> aliveProxies = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (MyProxy proxy : proxies) {
                futures.add(executor.submit(() -> {
                    if (testProxy(proxy)) {
                        aliveProxies.add(proxy);
                        log.info("✓ Proxy alive: " + proxy);
                    } else {
                        log.info("✗ Proxy dead: " + proxy);
                    }
                }));
            }

            // Đợi tất cả task hoàn thành
            for (Future<?> future : futures) {
                try {
                    future.get(TIMEOUT + 1000, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.error("Exception occurred while validating proxy: " + e.getMessage());
                }
            }
        } finally {
            executor.shutdownNow();
        }

        log.info("Validation completed. Found " + aliveProxies.size() + " alive proxies");
        return aliveProxies;
    }

    private static boolean testProxy(MyProxy proxy) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(TEST_URL);
            
            java.net.Proxy javaProxy = new java.net.Proxy(
                java.net.Proxy.Type.HTTP,
                new InetSocketAddress(proxy.getIp(), proxy.getPort())
            );

            connection = (HttpURLConnection) url.openConnection(javaProxy);
            
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            
            // Thành công nếu response code là 2xx
            return responseCode >= 200 && responseCode < 300;

        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                    log.warn("Error occurred while disconnecting connection for proxy: " + ignored.getMessage());
                }
            }
        }
    }
}