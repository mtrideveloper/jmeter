package org.apache.jmeter.config.mtri.model;

public class MyProxy {
    // Thêm từ khóa final vào đây
    private final String ip;
    private final int port;
    private final String type;

    public MyProxy(String ip, int port, String type) {
        this.ip = ip;
        this.port = port;
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return ip + ":" + port + " (" + type + ")";
    }
}