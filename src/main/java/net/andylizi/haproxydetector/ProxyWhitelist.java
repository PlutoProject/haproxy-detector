package net.andylizi.haproxydetector;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ProxyWhitelist {
    @Nullable
    public static ProxyWhitelist whitelist = new ProxyWhitelist(new ArrayList<>(0));

    private static volatile InetAddress lastWarning;

    public static boolean check(SocketAddress addr) {
        if (whitelist == null) return true;
        return addr instanceof InetSocketAddress && whitelist.matches(((InetSocketAddress) addr).getAddress());
    }

    public static Optional<String> getWarningFor(SocketAddress socketAddress) {
        if (!(socketAddress instanceof InetSocketAddress)) return Optional.empty();
        InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
        if (!address.equals(lastWarning)) {
            lastWarning = address;
            return Optional.of("Proxied remote address " + address + " is not in the whitelist");
        }
        return Optional.empty();
    }

    public static Optional<ProxyWhitelist> loadOrDefault(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (!Files.exists(path) || Files.isDirectory(path)) {
            Files.write(path, Arrays.asList(
                    "# List of allowed proxy IPs",
                    "#",
                    "# An empty whitelist will disallow all proxies.",
                    "# Each entry must be an valid IP address, domain name or CIDR.",
                    "# Domain names will be resolved only once at startup.",
                    "# Each domain can have multiple A/AAAA records, all of them will be allowed.",
                    "# CIDR prefixes are not allowed in domain names.",
                    "",
                    "127.0.0.0/8",
                    "::1/128"
            ), StandardCharsets.UTF_8);
        }
        return load(path);
    }

    public static Optional<ProxyWhitelist> load(Path path) throws IOException {
        ArrayList<CIDRWrapper> list = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            boolean first = true;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                if (first && line.startsWith("YesIReallyWantToDisableWhitelistItsExtremelyDangerousButIKnowWhatIAmDoing")) {
                    return Optional.empty();
                }

                first = false;

                list.add(new CIDRWrapper(line));
            }
        }
        return Optional.of(new ProxyWhitelist(list));
    }

    private final List<CIDRWrapper> list;

    public ProxyWhitelist(List<CIDRWrapper> list) {
        this.list = new ArrayList<>(list);
    }

    public boolean matches(InetAddress addr) {
        for (CIDRWrapper cidrWrapper : list) {
            if (cidrWrapper.check(addr)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return this.list.size();
    }

    @Override
    public String toString() {
        return "ProxyWhitelist" + list;
    }


    public static class CIDRWrapper {
        private final String line;
        private List<CIDR> cached;
        private Instant cacheValidUntil;

        public CIDRWrapper(String line) throws UnknownHostException {
            this.line = line;
            this.cached = new ArrayList<>();
            this.cacheValidUntil = Instant.ofEpochMilli(0);
            checkCache();
        }

        public void checkCache() throws UnknownHostException {
            if (cacheValidUntil.isBefore(Instant.now())) {
                this.cached = CIDR.parse(line);
                this.cacheValidUntil = Instant.now().plus(5, ChronoUnit.MINUTES);
            }
        }

        public boolean check(InetAddress address) {
            try {
                checkCache();
            } catch (UnknownHostException e) {
                System.out.println("[haproxy-detector-ria] UnknownHostException: " + e.getMessage() + ": for host: " + line);
            }
            for (CIDR cidr : cached) {
                if (cidr.contains(address)) {
                    return true;
                }
            }
            return false;
        }
    }

}
