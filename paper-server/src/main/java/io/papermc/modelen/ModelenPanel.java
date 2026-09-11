package io.papermc.modelen;

import ca.spottedleaf.common.time.TickData;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class ModelenPanel extends AbstractAppender {
    private static final int MAX_LOG_LINES = 300;
    private static final long START = System.nanoTime();

    private final java.util.Deque<String> logBuffer = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private HttpServer server;
    private ModelenConfig config;
    private volatile boolean alerting;
    private volatile long lastAlertMs;

    private ModelenPanel() {
        super("ModelenPanel", null, null, true, org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY);
    }

    public static void start(final ModelenConfig config) {
        if (!config.panelEnabled) {
            return;
        }
        final ModelenPanel panel = new ModelenPanel();
        panel.config = config;
        final Logger root = (Logger) LogManager.getRootLogger();
        root.addAppender(panel);
        try {
            panel.server = HttpServer.create(new InetSocketAddress("127.0.0.1", config.panelPort), 0);
            panel.server.createContext("/", panel::handleRoot);
            panel.server.createContext("/api/status", panel::handleStatus);
            panel.server.createContext("/api/log", panel::handleLog);
            panel.server.setExecutor(null);
            panel.server.start();
            System.out.println("[Modelen] Web panel active: http://127.0.0.1:" + config.panelPort
                + (config.panelPassword.isEmpty() ? " (auth kapali - sadece localhost)" : " (sifre korumali)"));
            panel.startAlarmWatcher(config);
        } catch (final Exception e) {
            System.out.println("[Modelen] Web panel could not start: " + e.getMessage());
        }
    }

    private void startAlarmWatcher(final ModelenConfig cfg) {
        final java.util.concurrent.ScheduledExecutorService exec = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "Modelen-Alarm");
            t.setDaemon(true);
            return t;
        });
        exec.scheduleAtFixedRate(() -> {
            try {
                final double tps = ModelenBootstrap.tps5s();
                if (tps < cfg.tpsAlarmThreshold && (System.nanoTime() - START) > 300_000_000_000L) {
                    final long now = System.currentTimeMillis();
                    if (now - this.lastAlertMs > cfg.tpsAlarmCooldownMin * 60_000L) {
                        this.lastAlertMs = now;
                        this.alerting = true;
                        System.out.println("[Modelen ALARM] TPS 5s ortalama " + String.format("%.1f", tps)
                            + " seviyesine dustu (esik: " + cfg.tpsAlarmThreshold + "). Sebip analizi icin /modelen status ve panel yazisina bakin.");
                    }
                } else if (tps >= cfg.tpsAlarmThreshold + 1.0) {
                    if (this.alerting) {
                        this.alerting = false;
                        System.out.println("[Modelen] TPS normale dondu (" + String.format("%.1f", tps) + ").");
                    }
                }
            } catch (final Throwable ignored) {
            }
        }, 30, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    private boolean authorized(final HttpExchange exchange) {
        if (this.config == null || this.config.panelPassword.isEmpty()) {
            return true;
        }
        final String header = exchange.getRequestHeaders().getFirst("X-Modelen-Token");
        if (this.config.panelPassword.equals(header)) {
            return true;
        }
        final String q = exchange.getRequestURI().getQuery();
        return q != null && q.contains("token=" + this.config.panelPassword);
    }

    @Override
    public void append(final org.apache.logging.log4j.core.LogEvent event) {
        final String line = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            + " " + event.getLevel().name() + " " + event.getMessage().getFormattedMessage();
        if (this.logBuffer.size() > MAX_LOG_LINES) {
            this.logBuffer.pollFirst();
        }
        this.logBuffer.addLast(line);
    }

    private void handleRoot(final HttpExchange exchange) {
        try {
            if (this.config != null && !this.config.panelPassword.isEmpty() && !this.authorized(exchange)) {
                final String login = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Modelen Panel - Giris</title></head><body style='font-family:sans-serif;background:#111;color:#ddd'>"
                    + "<div style='max-width:320px;margin:80px auto'><h1>Modelen Panel</h1>"
                    + "<input id='p' type='password' placeholder='Panel sifresi' style='width:100%;padding:10px'>"
                    + "<button onclick='go()' style='width:100%;padding:10px;margin-top:8px'>Giris</button></div>"
                    + "<script>function go(){sessionStorage.setItem('mtok',document.getElementById('p').value);"
                    + "location.href='/';}</script></body></html>";
                this.send(exchange, "text/html; charset=utf-8", login.getBytes(StandardCharsets.UTF_8));
                return;
            }
            final String html = pageTemplate();
            this.send(exchange, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStatus(final HttpExchange exchange) {
        try {
            if (!this.authorized(exchange)) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                try (OutputStream os = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(401, -1);
                }
                return;
            }
            final double mspt = mspt5s();
            final double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 0.001));
            int plugins = 0;
            for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin.isEnabled()) {
                    plugins++;
                }
            }
            final String json = "{\"tps\":" + limit(tps)
                + ",\"mspt\":" + limit(mspt)
                + ",\"players\":" + Bukkit.getOnlinePlayers().size()
                + ",\"plugins\":" + plugins
                + ",\"alert\":" + this.alerting
                + ",\"uptimeMs\":" + ((System.nanoTime() - START) / 1_000_000)
                + "}";
            this.send(exchange, "application/json", json.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLog(final HttpExchange exchange) {
        try {
            if (!this.authorized(exchange)) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                try (OutputStream os = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(401, -1);
                }
                return;
            }
            final StringBuilder sb = new StringBuilder();
            final List<String> lines = new ArrayList<>(this.logBuffer);
            for (final String line : lines) {
                sb.append(escape(line)).append('\n');
            }
            this.send(exchange, "text/plain; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            try {
                this.send(exchange, "text/plain; charset=utf-8", "log unavailable".getBytes(StandardCharsets.UTF_8));
            } catch (final Exception ignored) {}
        }
    }

    private static String pageTemplate() {
        return "<!DOCTYPE html>"
            + "<html><head><meta charset='utf-8'><title>Modelen Panel</title><style>"
            + "body{font-family:sans-serif;background:#111;color:#ddd;margin:24px}"
            + "h1{color:#8fce00}.num{font-size:32px;margin-right:24px}"
            + "canvas{background:#1a1a1a;border-radius:8px}"
            + "pre{background:#1a1a1a;padding:12px;border-radius:8px;height:260px;overflow:auto;font-size:12px}"
            + "</style></head><body>"
            + "<h1>Modelen Panel</h1>"
            + "<div><span class='num' id='tps'>-</span><span class='num' id='mspt'>-</span>"
            + "<span class='num' id='players'>-</span><span class='num' id='plugins'>-</span>"
            + "<span class='num' id='uptime'>-</span></div>"
            + "<div id='alert' style='display:none;background:#7a1010;color:#ffdede;padding:12px;border-radius:8px;margin:12px 0'>ALERT: Dusuk TPS algilandi!</div>"
            + "<canvas id='chart' width='800' height='160'></canvas>"
            + "<h3>Log (son 300 satir)</h3><pre id='log'></pre>"
            + "<script>"
            + "const hist=[];"
            + "function fmt(ms){if(ms<60000)return Math.floor(ms/1000)+' saniye';if(ms<3600000)return Math.floor(ms/60000)+' dakika';return Math.floor(ms/3600000)+' saat';}"
            + "function draw(){const c=document.getElementById('chart');const ctx=c.getContext('2d');"
            + "ctx.clearRect(0,0,c.width,c.height);ctx.strokeStyle='#333';ctx.beginPath();"
            + "ctx.moveTo(0,c.height-30);ctx.lineTo(c.width,c.height-30);ctx.stroke();"
            + "ctx.beginPath();ctx.strokeStyle='#7db5f0';"
            + "const n=hist.length;const step=n>1?c.width/(n-1):0;"
            + "hist.forEach((v,i)=>{const x=i*step;const y=c.height-30-(v/20)*(c.height-40);"
            + "i==0?ctx.moveTo(x,y):ctx.lineTo(x,y);});"
            + "ctx.stroke();ctx.fillStyle='#888';ctx.fillText('20 TPS',4,14);ctx.fillText('0',4,c.height-16);}"
            + "const TOK=(()=>{const q=new URLSearchParams(location.search).get('token');"
            + "if(q){sessionStorage.setItem('mtok',q);history.replaceState({},'','/');return q;}"
            + "return sessionStorage.getItem('mtok')||'';})();"
            + "const Q=TOK?('?token='+encodeURIComponent(TOK)):'';"
            + "async function tick(){"
            + "const r=await fetch('/api/status'+Q);if(r.status==401){location.reload();return;}"
            + "const j=await r.json();"
            + "document.getElementById('tps').textContent='TPS '+j.tps.toFixed(1);"
            + "document.getElementById('mspt').textContent='MSPT '+j.mspt.toFixed(1);"
            + "document.getElementById('players').textContent='Oyuncu '+j.players;"
            + "document.getElementById('plugins').textContent='Plugin '+j.plugins;"
            + "document.getElementById('uptime').textContent='Calisma '+fmt(j.uptimeMs);"
            + "const a=document.getElementById('alert');a.style.display=j.alert?'block':'none';"
            + "hist.push(j.tps);if(hist.length>100)hist.shift();draw();"
            + "const l=await fetch('/api/log'+Q);document.getElementById('log').textContent=await l.text();"
            + "}setInterval(tick,2000);tick();"
            + "</script></body></html>";
    }

    private static String escape(final String s) {
        final StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 32) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static double mspt5s() {
        final TickData.TickReportData report = MinecraftServer.getServer().tickTimes5s.generateTickReport(null, System.nanoTime(), MinecraftServer.getServer().tickRateManager().nanosecondsPerTick());
        return report == null ? 0.0 : report.timePerTickData().segmentAll().average() * 1.0E-6;
    }

    private static double limit(final double tps) {
        return Math.round(tps * 100.0) / 100.0;
    }

    private void send(final HttpExchange exchange, final String contentType, final byte[] bytes) throws java.io.IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        try (OutputStream os = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(200, bytes.length);
            os.write(bytes);
        }
    }

}

