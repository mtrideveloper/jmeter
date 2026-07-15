package org.apache.jmeter.config.mtri.extensions;

import org.apache.jmeter.config.mtri.model.MyProxy;
import org.apache.jmeter.config.mtri.util.MyConstant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class ProxyScraper {
    private static final Logger log = LoggerFactory.getLogger(ProxyScraper.class);

    public List<MyProxy> scrapeProxies(int maxProxies) {
        List<MyProxy> allProxies = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(3);

        if (allProxies.size() > maxProxies) {
            log.info("Should scrape only " + maxProxies + " proxies.");
            executor.shutdown();
            return allProxies;
        }

        List<Future<List<MyProxy>>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            return scrapeFromSource(MyConstant.PROXY_SOURCE, maxProxies);
        }));

        for (Future<List<MyProxy>> future : futures) {
            try {
                allProxies.addAll(future.get(15, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.error("Exception occurred while scraping " + MyConstant.PROXY_SOURCE + ": " + e.getMessage());
            }
        }

        executor.shutdown();
        return allProxies;
    }

    private static List<MyProxy> scrapeFromSource(String url, int maxProxies) {
        List<MyProxy> proxies = new ArrayList<>();
        try {
            log.info("Scraping: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent(MyConstant.USER_AGENT)
                    .timeout(MyConstant.TIMEOUT)
                    .get();

            // 1. Khởi tạo index mặc định theo cấu trúc ảnh
            int ipIndex = 0;
            int portIndex = 1;
            int httpsIndex = 6;

            // 2. Tự động quét tìm index từ thẻ <thead> -> <tr> -> <th>
            Element table = doc.select("table.table").first(); // Nhắm vào table có class "table"
            if (table != null) {
                Element headerRow = table.select("thead tr").first();
                if (headerRow != null) {
                    // Quét tất cả các thẻ <th> để xác định index của IP, Port và HTTPS
                    Elements headers = headerRow.select("th");
                    for (int i = 0; i < headers.size(); i++) {
                        String headerText = headers.get(i).text().trim().toLowerCase(Locale.ROOT);

                        if (headerText.contains("ip") || headerText.contains("address")) {
                            ipIndex = i;
                        } else if (headerText.contains("port")) {
                            portIndex = i;
                        } else if (headerText.contains("https")) {
                            httpsIndex = i;
                        }
                    }
                }
            }

            // 3. Quét các hàng dữ liệu trong <tbody>
            Elements trList = table.select("tbody tr");
            for (Element tr : trList) {
                Elements tdList = tr.select("td");

                // kiểm tra IP, Port hay HTTPS đâu là cột cuối
                // tránh lỗi IndexOutOfBoundsException
                int maxIndex = Math.max(ipIndex, Math.max(portIndex, httpsIndex));

                if (tdList.size() > maxIndex) {
                    // lấy dữ liệu thẻ td theo index
                    String ip = tdList.get(ipIndex).text().trim();
                    String port = tdList.get(portIndex).text().trim();
                    String https = tdList.get(httpsIndex).text().trim();

                    if (proxies.size() >= maxProxies) {
                        break; // Dừng khi đủ proxy
                    }

                    if (https.equalsIgnoreCase("yes") && isValidIP(ip) && isValidPort(port)) {
                        proxies.add(new MyProxy(ip, Integer.parseInt(port), "HTTPS"));
                    }
                }
            }

            log.info("Scraped " + proxies.size() + " proxies from " + url);
        } catch (Exception e) {
            log.error("Error occurred while scraping " + url + ": " + e.getMessage());
        }
        return proxies;
    }

    private static boolean isValidIP(String ip) {
        return ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    private static boolean isValidPort(String port) {
        try {
            int p = Integer.parseInt(port);
            return p > 0 && p < 65536;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
