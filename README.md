# Levtus (Levis Conatus) 🐧🚀
![Maven Central](https://img.shields.io/maven-central/v/io.github.bernardusz/levtus-core)
![License](https://img.shields.io/github/license/Bernardusz/levtus)
![Maven Release](https://img.shields.io/github/actions/workflow/status/Bernardusz/levtus/release.yml?label=maven%20release)

**Levtus** (Latin: *Levis Conatus* - "Light Effort") is a high-performance, zero-dependency HTTP/1.1 engine built from the ground up for the modern JVM. It is designed to be lightweight, secure, and incredibly fast by leveraging the power of **Java 21+ Virtual Threads (Project Loom)**.

> "Infrastructure should be simple, transparent, and built to last."

---

## 🚧 **IMPORTANT - Construction Zone: Pre-v1.0.0** 
> Levtus is currently in rapid iteration mode. We prioritize **Developer Experience (DX)** and **Performance** above all else, which means the API will evolve quickly and **breaking changes** are common. Stable Long-Term Support (LTS) will only be provided starting from version **1.0.0**.

---

## ✨ Why Levtus?
- 📦 Zero External Dependencies: No runtime overhead, no third-party version conflicts, and tiny deployment binaries.
- 🌳 Trie-Based Router: Route matching speed is proportional only to the depth of your URL segments (O(L)), making it consistently fast even as you add hundreds of endpoints.
- 🔒 Built-In Security Guards: Configurable hardware safeguards prevent denial-of-service (DoS) attempts by enforcing strict size boundaries on headers during stream consumption.
- 🎯 Native Java Performance: Optimized for Java 25+, taking full advantage of modern platform innovations.

---

## ⚡ Technical Highlights

- **Loom-Native Concurrency:** Uses a `newVirtualThreadPerTaskExecutor` to handle thousands of concurrent connections with minimal memory footprint.
- **Trie-Based Routing:** Features a high-performance Prefix Tree (Trie) router for $O(K)$ route matching (where $K$ is the path length).
- **Zero Dependencies:** Pure Java. No external libraries, no "DLL hell," and ultra-small JAR size.
- **Hardened Security:** Built-in protection against:
  - **Path Traversal:** Secure `render()` logic with path normalization.
  - **Memory Exhaustion:** Configurable limits for headers, body size, and line lengths.
  - **Connection Overload:** Semaphore-based throttling to protect system resources.
- **SSL/TLS Ready:** Native support for HTTPS via PKCS12 keystores.
- **Fluent API:** Express-inspired context handling for JSON, HTML, and binary data.

---

## 🚀 Quick Start

```java
import io.github.bernardusz.levtus.Levtus;

public class Main {
    public static void main(String[] args) {
        Levtus app = Levtus.create();

        // Middleware support
        app.use((ctx, next) -> {
            System.out.println("Request received: " + ctx.req().path());
            next.run();
        });

        // Simple GET route
        app.get("/hello", ctx -> {
            ctx.text("Hello from the Levtus Engine!");
        });

        // Dynamic routing with path params
        app.get("/user/{id}", ctx -> {
            String userId = ctx.param("id");
            ctx.json("{\"id\": \"" + userId + "\"}");
        });

        // Secure static file rendering
        app.get("/", ctx -> {
            ctx.render("index.html");
        });

        app.listen(8080);
    }
}
```

---

## 🛠 Architecture & Internals

### The Request Lifecycle
1. **Connection Throttling:** A global `Semaphore` limits active connections to prevent the JVM from being overwhelmed.
2. **Virtual Thread Hand-off:** Each socket is handed to a Virtual Thread, keeping the main loop free for new accepts.
3. **Protocol Parsing:** Raw `InputStream` parsing for HTTP/1.1 compliance, including support for persistent connections and keep-alive.
4. **Trie Matching:** The router traverses the Trie to find the correct handler while extracting path parameters on the fly.
5. **Middleware Execution:** A recursive execution chain allows for powerful pre- and post-processing.

### Security Configurations
Levtus gives you fine-grained control over your server's surface area:
```java
app.setMaxBodySize(10 * 1024 * 1024); // 10MB limit
app.setMaxHeaderCount(100);
app.setMaxLineSize(8192); // Prevent Slowloris attacks
```

---

## 📈 Performance
By utilizing **Project Loom**, Levtus avoids the overhead of traditional thread-pooling and the complexity of reactive programming. It provides a simple, synchronous programming model that scales horizontally with hardware.

---

## 📜 License
MIT License. Feel free to use, modify, and distribute.

---
*Built with 🐧 and raw sockets 🐧💀.*
