---
title: Middleware
description: Documentation on how to add Middleware to Levtus
---

Middleware components are interceptors that can process HTTP requests before they reach the final route handler. They are ideal for cross-cutting concerns like logging, authentication, and header manipulation.

## 📚 The Middleware Interface
Levtus uses a functional interface `Middleware` which provides the current `LevtusContext` and a `Runnable` to trigger the next step in the chain.

```java
@FunctionalInterface
public interface Middleware {
    void handle(LevtusContext ctx, Runnable next);
}
```

## ⚙️ How it Works
1. **Execution Chain:** Middleware are executed in the order they are registered using `app.use()`.
2. **Passing Control:** You **must** call `next.run()` to pass control to the next middleware or the final route handler.
3. **Short-circuiting:** If you don't call `next.run()`, the request stops there. This is useful for unauthorized access or early exits.

## 📝 Examples

### Simple Logger
```java
app.use((ctx, next) -> {
    System.out.println("Incoming: " + ctx.req().method() + " " + ctx.req().path());
    next.run();
});
```

### Authentication Guard
```java
app.use((ctx, next) -> {
    String auth = ctx.header("Authorization");
    if ("secret-token".equals(auth)) {
        next.run(); // Authorized
    } else {
        ctx.status(401).send("Unauthorized"); // Short-circuit
    }
});
```

### Response Timer
```java
app.use((ctx, next) -> {
    long start = System.currentTimeMillis();
    next.run();
    long end = System.currentTimeMillis();
    System.out.println("Handled in " + (end - start) + "ms");
});
```
