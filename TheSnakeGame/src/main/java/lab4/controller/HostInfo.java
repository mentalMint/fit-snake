package lab4.controller;

import java.net.InetAddress;
import java.util.Objects;

public class HostInfo {
    public InetAddress ip;
    public int port;

    public HostInfo(InetAddress ip, int port){
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostInfo hostInfo = (HostInfo) o;
        return port == hostInfo.port && Objects.equals(ip, hostInfo.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return  ip + ":" + port;
    }
}
