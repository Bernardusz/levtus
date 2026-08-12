---
title: Levtus Configuration
description: Documentation on how to configure Levtus, security-wise and serving file and rendering HTML
---

# 🔒 Static Files & Security

Levtus is designed with a "Security First" mindset, providing built-in protection for serving static assets.

## 📂 Serving Static Files
By default, Levtus looks for a `./public` directory. you can customize this using `app.staticFiles()`.

```java
Levtus app = Levtus.create();
app.staticFiles("./www"); // Serve from ./www
```

To serve a file within a route:
```java
app.get("/", ctx -> {
    ctx.render("index.html");
});
```

## 🛡️ Security Features

### Path Normalization
Levtus automatically normalizes paths provided to `render()`. It resolves `.` and `..` segments and converts backslashes to forward slashes.

### Directory Traversal Protection
The `render()` method checks if the resolved absolute path still resides within the configured static root. If a client attempts to use `../../etc/passwd` to escape the root, Levtus will catch it and return a **403 Forbidden** response.

### MIME Type Probing
Levtus uses Java's `Files.probeContentType()` to automatically determine the correct `Content-Type` for the files it serves, falling back to `application/octet-stream` if unknown.

## 🛠 Security Hardening
You can further harden your server by limiting the surface area for attacks:

```java
app.maxConcurrentConnections(50) // Restricts the maximum number of concurrent client connections.
        .maxBodySize(1024 * 1024) // Imposes a hard limit on the HTTP request body size to prevent memory exhaustion (OOM) attacks.
        .maxHeaderCount(50) // Restricts the maximum number of HTTP headers a client can send in a single request.
        .maxHeaderSize(4096) // Defines the maximum cumulative byte size allowed for all HTTP headers combined.
        .maxLineSize(4096) // Sets the maximum character length for the initial HTTP request line (Method + URI + Protocol).
        .maxEmptyLines(3) // Defines the maximum number of empty lines tolerated between requests on a Keep-Alive connection.
        .staticFiles("public") // Designates a local directory to automatically serve static web assets (HTML, CSS, JS, Images).
        .initialSocketTimeout(5000) // Set the initial socket timeout for all incoming HTTP requests
        .processingSocketTimeout(20000); // Set the processing socket timeout for all incoming HTTP requests
```
