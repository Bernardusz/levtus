---
title: Request API
description: Documentation on how to handle the incoming HTTP Request.
---

The `Request` object represents an incoming HTTP/1.1 request. It is accessible via `ctx.req()`.

## Quick Explanation on Request
> This is a quick explanation about incoming request & HTTP/1.1 concept. Skip if you already understand: [Next Section](#-understanding-the-request)

So in HTTP/1.1 (and HTTP/1.0) by default is based on Request-Response based arcitecture. So the client (Browser/Mobile app/Curl or Wget) will send a Request to the server (In this case Levtus) and our server will respond with the answer -> Response.

So in Levtus, the incoming HTTP Request is neatly translated into the object with the properties explained below 🐧🐧🐧

## 📖 Understanding the Request
Levtus maps every incoming socket connection to a `Request` object. Most fields are immutable (final) to ensure consistency during the request lifecycle.

### 🎯 Core Properties
- **Method:** `GET`, `POST`, `PUT`, or `DELETE`.
- **Path:** The normalized URI path.
- **Headers:** Case-insensitive access to HTTP headers.
- **Queries:** Parsed URL query parameters.
- **Max Body Size:** The maximum body size in a Request.
- **Max Chunk Size:** The maximum size of a chunk in a Request.
- **Max Chunk Count:** The maximum chunk size in a Request.
- **Protocol:** The protocol version (HTTP/1.1 or HTTP/1.0) 


## ✉️ Reading Headers
You can access headers directly from the request:

```java
app.get("/header", ctx -> {
    String userAgent = ctx.req().header("User-Agent"); // Get the first value
    List<String> cookies = ctx.req().headers("Cookie"); // Get the whole list of value
    Map<String, List<String>> = ctx.req().headers(); // Get the whole headers list.
});
```

Returns an empty String/List if not found/is empty.

## 🔍 Query Parameters
Levtus automatically parses query strings into a map of lists.

```java
// URL: /search?q=levtus&tag=java&tag=loom
app.get("/search", ctx -> {
    String query = ctx.req().query("q"); // "levtus"
    List<String> tags = ctx.req().queries("tag"); // ["java", "loom"]
    HashMap<String, List<String>> id = ctx.req().queries(); // return the whole map of queries.
});
```

## 📦 Request Body
For `POST` and `PUT` requests, you can read the body bytes or string.

```java
app.post("/submit", ctx -> {
    String body = ctx.req().body().toString();
    String secondBody = ctx.req().bodyAsString();
    System.out.println("Received: " + body);
});

app.put("/edit", ctx -> {
    LevtusInputStream bodyStream = ctx.bodyStream(); // LevtusInputStream is a wrapper used internally by bodyStream() you can use it, but it is reccomended to use normal InputStream:
    InputStream bodyStream = ctx.bodyStream();

    // As a note, you can only call bodyStream once, afterwards you can't call `body()`, `bodyAsString()`, `bodyStream()`, nor `bodyStream()` wrapper. If you do, you'll get bodyAlreadyConsumed Exception.
});

app.post("/upload", ctx -> {
    ctx.bodyStream(stream -> {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            // process chunk
            processChunk(buffer, read);
        }
    });

    ctx.text("Upload received");
});
```

## ✂️ Chunked Request Support
> In modern HTTP/1.1 (and even HTTP/1.0), Chunked Request is widely used as you don't need to know the final size of the file. Or you can chunk a large file into the server for uploads. Levtus handles this behind the scene with `LevtusInputStream`, so choose what methods suits you!

## 📚 Request API
> This section lists off all the undoccumented APIs inside Request. Feel free to skip.

Just keep in mind, that all APIs below is documented by calling the Request object inside [LevtusContext](/levtus/docs/api/levtus-context-api). For cleaner API you can omit the `.req()`.

### Checking method
Retrieves the HTTP method used for this request.
```java
String method = ctx.req().method();
```

### Path
Retrieves the requested URI path without the query string.
```java
String path = ctx.req().method();
``` 

### Content Type
Determines the MIME type of the request payload based on the 'Content-Type' header. Defaults to "text/plain" if the header is missing
```java
String contentType = ctx.req().contentType();
```

### Content Length
Parses the 'Content-Length' header to determine the expected size of the request body. Defaults to 0 if the header is missing or malformed.

```java
int contentLength = ctx.req().contentLength();
```

### Is Cached
Checks whether the request body has already been fully read and stored in memory.

So internally `bodyAsString()` calls `body()` and `body()` caches the reading, so the call wouldn't need to be read from socket again.

```java
boolean isCached = ctx.req().isCached();
```

### Bytes Read
Retrieves the total number of body bytes successfully read from the client stream so far.

```java
long bytesRead = ctx.req().bytesRead();
```

### Is Chunked
Checks whether the request body/incoming request is chunked.

```java
boolean isChunked = ctx.req().isChunked();
```

### Is Keep Alive
Check the connection header to determine if the connection should be kept alive.

In HTTP/1.1 by default it is keep alive, but when client want to close connection we respect it.

In HTTP/1.0 is close immediately by default. But they can prefer keep alive if the header is present.

```java
boolean isKeepAlive = ctx.req().isKeepAlive();
```
