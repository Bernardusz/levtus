# Levtus - Levis Conatus

**Levtus** is a high-performance, zero-dependency Java micro-framework designed for the modern JVM. 

Most Java frameworks are built on "Black Boxes"—layers of dependencies that hide how the internet actually works. **Levtus** is different. It is a "Clean Room" implementation of a web server, built from the ground up using only the standard Java 21+ libraries.

## 🚀 The Philosophy: "Zero-Dep, Total Control"

* **Zero Dependencies:** No Netty, no Jetty, no Jackson. The `pom.xml` is empty. Every byte is auditable.
* **Virtual Threading:** Powered by **Project Loom**. Every incoming connection spawns a Virtual Thread, allowing for massive concurrency without the memory overhead of traditional thread pools.
* **Architectural Sovereignty:** Built on raw `SSLServerSocket` and `java.io` to demonstrate a deep understanding of the TCP/IP and HTTP/1.1 protocols.
* **Kotlin Interop:** Designed with Functional Interfaces (SAM) to be 100% compatible with Kotlin's idiomatic DSL syntax.

## 🏗️ Technical Architecture

Levtus follows a "Modular Kernel" design. The core is lightweight, while advanced features (ORM, Templating) are plugged in via interfaces.

| Component | Logic                                                                   |
| :--- |:------------------------------------------------------------------------|
| **Engine** | `java.net.ServerSocket` + `Executors.newVirtualThreadPerTaskExecutor()` |
| **Routing** | O(L) Key-Value Lookup via `java.util.HashMap`                           |
| **Security** | Native TLS 1.3 implementation via `SSLContext`                          |
| **Concurrency** | Non-blocking feeling through synchronous Virtual Threads                |



## 🛠️ Usage (Future API)

```java
package io.github.bernardusz.levtus;

public class Main {
    public static void main(String[] args) {
        Levtus app = Levtus.create();

        // A simple GET route
        app.get("/hello", ctx -> {
            ctx.res().send("Hello from the Levtus Engine!");
        });

        app.get("/", ctx -> {
            ctx.render("index.html");
        });

        app.ssl("./keystore.p12", "1234567");
        app.staticFiles("./public");

        // A POST route for data processing
        app.post("/data", ctx -> {
            byte[] body = ctx.req().body();
            System.out.println(new String(body));
            // Action: Save to DB or Cloud
            ctx.res().status(201).send("Data Received");
        });

        app.listen(8080);
    }
}
```

## 📈 Roadmap

- [x] **Phase 1:** Core Networking Engine & Virtual Thread Integration.
- [x] **Phase 2:** Byte-level HTTP/1.1 Request Parser (Method, Headers, Body).
- [x] **Phase 3:** O(L) Router and Middleware Pipeline.
- [x] **Phase 4:** TLS 1.3 / SSL Support.
- [ ] **Phase 5:** Maven Central Publication (`io.github.bernardusz`).

## 🎓 Scholarship & Academic Context

This project was developed as a technical thesis in minimalist software engineering. It aims to prove that modern Java can achieve high performance and security without relying on third-party ecosystems, prioritizing **Auditability** and **Fundamental Engineering**.
Alongside to prove my capability in coding and creating useful tools without any dependencies.

---

**Author:** [Bernardusz](https://github.com/Bernardusz)  
**License:** MIT
