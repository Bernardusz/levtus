---
title: SSL/TSL Setup
description: Documentation on how to set up SSL/TSL to make Levtus an HTTPS server
---

# 🔐 SSL/TLS Setup

Levtus supports secure connections over HTTPS using Java KeyStores (JKS or PKCS12).

## 🚀 Enabling HTTPS
To enable SSL, call the `.ssl()` method on your application instance before calling `.listen()`.

```java
Levtus app = Levtus.create();

app.ssl("path/to/keystore.p12", "your-password"); // Independent of static files path

app.listen(8443);
```

## 🛠 Preparing your KeyStore
Levtus requires a valid certificate stored in a keystore file. You can generate a self-signed certificate for development using the JDK's `keytool`:

```bash
keytool -genkeypair \
  -alias levtus \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 3650
```

## 📡 Automatic Detection
Levtus will automatically switch to an SSL-capable `ServerSocket` if `.ssl()` has been configured. The console output will confirm the mode:

```text
🚀 Levtus Engine started on port 8443 (HTTPS)
```

## ⚠️ Important Notes
- **Virtual Threads:** Levtus still uses Loom's virtual threads for HTTPS connections, ensuring high performance even with the overhead of encryption.
- **Protocol:** Currently supports standard TLS protocols provided by the underlying JVM.
