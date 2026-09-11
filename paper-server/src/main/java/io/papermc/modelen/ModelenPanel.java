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

    private ModelenPanel() {
        super("ModelenPanel", null, null, true, org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY);
    }

    public static void start(final ModelenConfig config) {
        if (!config.panelEnabled) {
            return;
        }
        final ModelenPanel panel = new ModelenPanel();
        final Logger root = (Logger) LogManager.getRootLogger();
        root.addAppender(panel);
        try {
            panel.server = HttpServer.create(new InetSocketAddress("127.0.0.1", config.panelPort), 0);
            panel.server.createContext("/", panel::handleRoot);
            panel.server.createContext("/api/status", panel::handleStatus);
            panel.server.createContext("/api/log", panel::handleLog);
            panel.server.setExecutor(null);
            panel.server.start();
            System.out.println("[Modelen] Web panel active: http://127.0.0.1:" + config.panelPort);
        } catch (final Exception e) {
            System.out.println("[Modelen] Web panel could not start: " + e.getMessage());
        }
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
            final String html = pageTemplate();
            this.send(exchange, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStatus(final HttpExchange exchange) {
        try {
            final MinecraftServer server = MinecraftServer.getServer();
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
                + ",\"uptimeMs\":" + ((System.nanoTime() - START) / 1_000_000)
                + "}";
            this.send(exchange, "application/json", json.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLog(final HttpExchange exchange) {
        try {
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
            + "async function tick(){"
            + "const r=await fetch('/api/status');const j=await r.json();"
            + "document.getElementById('tps').textContent='TPS '+j.tps.toFixed(1);"
            + "document.getElementById('mspt').textContent='MSPT '+j.mspt.toFixed(1);"
            + "document.getElementById('players').textContent='Oyuncu '+j.players;"
            + "document.getElementById('plugins').textContent='Plugin '+j.plugins;"
            + "document.getElementById('uptime').textContent='Calisma '+fmt(j.uptimeMs);"
            + "hist.push(j.tps);if(hist.length>100)hist.shift();draw();"
            + "const l=await fetch('/api/log');document.getElementById('log').textContent=await l.text();"
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

