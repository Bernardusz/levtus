---
title: Response API
description: Documentation on how to send Response to client
---

> Now that we have learned about how to receive the incoming Request, let's learn how to answer the request 🐧🐧🐧
>
> This page shows the `Response` API accessed through `ctx.res()` from the Levtus context.
>
> Every example below calls the underlying `Response` object.

The `Response` object provides a fluent API to construct and send data back to the client. It handles headers, status codes, and various payload formats.

## 📡 Status Codes
You can set the HTTP status code using the `.status(int)` method. This method returns the `Response` instance, allowing for method chaining.

```java
app.get("/not-found", ctx -> {
    ctx.res().status(404).send("Custom Not Found Page");
});
```

## 📑 Content Type
Set the `Content-Type` header easily:

```java
app.get("/custom", ctx -> {
    ctx.res().contentType("text/xml").send("<root>Hello</root>");
});
```

## ✉️ Sending Data

### Plain Text
Automatically sets `Content-Type` to `text/plain`.
```java
ctx.res().text("Hello World");
```

### HTML
Automatically sets `Content-Type` to `text/html`.
```java
ctx.res().html("<h1>Welcome</h1>");
```

### JSON
Automatically sets `Content-Type` to `application/json`.
```java
ctx.res().json("{\"message\": \"success\"}");
```

### Binary Data
Sends a raw byte array as `application/octet-stream`.
```java
byte[] data = ...;
ctx.res().sendBinary(data);
```

## 📂 Rendering Files
The `render(String path)` method serves static files from your configured static directory. It automatically probes the MIME type and handles path normalization to prevent directory traversal attacks.

```java
app.get("/", ctx -> {
    ctx.res().render("index.html");
});
```

## 🛠 Header Management
You can set individual headers or multiple headers at once.

```java
// Single header - replacing any existing value(s) for this header name
ctx.res().header("X-Custom-Header", "Value"); 
// Single Batch - replacing any existing list for this header name.
ctx.res().headers("X-Header", List.of("Levtus"));
// Batch headers - Merges a map of headers into the existing response headers.
Map<String, List<String>> myHeaders = Map.of("X-Service", List.of("Levtus"));
ctx.res().headers(myHeaders);
```

## 🧱 Empty and Raw Bytes Responses
Send an empty response or raw byte payload without converting it to a String.

```java
ctx.res().status(204).send();

byte[] data = getBytesFromSource();
ctx.res().send(data);
```

### Supported overloads
- `ctx.res().send()`
- `ctx.res().send(byte[] bodyBytes)`

## 📂 File Responses
Send files directly from disk. `sendFile` uses zero-copy transfer when possible.

```java
ctx.res().sendFile(Path.of("/var/www/static/image.png"));
ctx.res().sendFile("/static/image.png");
```

### Supported overloads
- `ctx.res().sendFile(Path path)`
- `ctx.res().sendFile(String path)`

## 🧱 Binary File Responses
Send a file as raw bytes with `application/octet-stream`.

```java
ctx.res().sendBinary(Path.of("/var/www/static/archive.zip"));
ctx.res().sendBinary("/static/archive.zip");
```

### Supported overloads
- `ctx.res().sendBinary(Path path)`
- `ctx.res().sendBinary(String path)`

## ⬇️ Download Responses
Force the browser to download a file by setting a `Content-Disposition` header.

```java
ctx.res().downloadFile(Path.of("/var/www/static/report.pdf"), "report-2026.pdf");
ctx.res().downloadFile("/static/report.pdf", "report-2026.pdf");
ctx.res().downloadFile(Path.of("/var/www/static/report.pdf"));
ctx.res().downloadFile("/static/report.pdf");
```

### Supported overloads
- `ctx.res().downloadFile(Path path, String filename)`
- `ctx.res().downloadFile(String path, String filename)`
- `ctx.res().downloadFile(Path path)`
- `ctx.res().downloadFile(String path)`
- `ctx.res().download(String filename)`

## 🚿 Chunked Streaming Responses
Use chunked mode when you need to send data progressively instead of buffering the whole body.

```java
ctx.res().stream()
   .sendChunk("first chunk")
   .sendChunk("second chunk");
ctx.res().finishChunkedResponse();
```

### Supported chunk methods
- `ctx.res().stream()`
- `ctx.res().sendChunk(byte[] data)`
- `ctx.res().sendChunk(byte[] data, int offset, int length)`
- `ctx.res().sendChunk(String data)`
- `ctx.res().sendChunk(String data, int offset, int length)`
- `ctx.res().finishChunkedResponse()`

## 📦 Chunked File Streaming
Stream a file in chunks rather than sending it in one shot.

```java
ctx.res().stream().streamFile(Path.of("/var/data/large.log"));
ctx.res().stream().streamFile(Path.of("/var/data/large.log"), 65536);
ctx.res().stream().streamFile("/static/large.log");
ctx.res().stream().streamFile("/static/large.log", 65536);
ctx.res().finishChunkedResponse();
```

### Supported overloads
- `ctx.res().streamFile(Path path)`
- `ctx.res().streamFile(Path path, int chunkSize)`
- `ctx.res().streamFile(String path)`
- `ctx.res().streamFile(String path, int chunkSize)`

## 📥 Chunked Download Streaming
Send a downloadable file in chunked mode so the client can start receiving data immediately.

```java
ctx.res().streamDownloadFile(Path.of("/var/www/video.mp4"), "video.mp4", 65536);
ctx.res().streamDownloadFile("/static/video.mp4", "video.mp4", 65536);
ctx.res().streamDownloadFile(Path.of("/var/www/video.mp4"), "video.mp4");
ctx.res().streamDownloadFile("/static/video.mp4", "video.mp4");
ctx.res().streamDownloadFile(Path.of("/var/www/video.mp4"));
ctx.res().streamDownloadFile("/static/video.mp4");
```

### Supported overloads
- `ctx.res().streamDownloadFile(Path path, String filename, int chunkSize)`
- `ctx.res().streamDownloadFile(String path, String filename, int chunkSize)`
- `ctx.res().streamDownloadFile(Path path, String filename)`
- `ctx.res().streamDownloadFile(String path, String filename)`
- `ctx.res().streamDownloadFile(Path path)`
- `ctx.res().streamDownloadFile(String path)`

## 🔁 Stream from an InputStream
Proxy or stream data from another `InputStream` into the response.

```java
ctx.res().stream().streamFrom(inputStream);
ctx.res().stream().streamFrom(inputStream, 65536);
ctx.res().finishChunkedResponse();
```

### Supported overloads
- `ctx.res().streamFrom(InputStream is)`
- `ctx.res().streamFrom(InputStream is, int chunkSize)`

## 🔧 Chunked Streaming Helpers
These helpers let you inspect and configure chunked responses.

- `ctx.res().isChunked()` — returns `true` when the response is already in chunked mode
- `ctx.res().withChunkSize(int chunkSize)` — change the default chunk size before streaming
- `ctx.res().finishChunkedResponse()` — write the final zero-length chunk and complete the response
```
