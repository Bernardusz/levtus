---
title: Routing & Parameters
description: Documentation on how to create a new route and take parameters in Levtus
---

Levtus was inspired by Express' fluent syntax, Go's performance, and Spring Boot robustness. So in our routing, it will look very familiar for those of you familiar with Express' routing.

## 📚 Explanation

Levtus utlized a Trie based router. Meaning the complexity is O(L). Do not be scared, basically the performance is based on how long the path is. 🐧

As Levtus is simple, you can use a single file to declare your app.

```java
import io.github.bernardusz.levtus.Levtus;

void main(){
    Levtus app = Levtus.create();

    app.get("/hello", ctx -> {
      ctx.text("Hello from Levtus Engine!");
    });

    app.listen(8080);
}
```

As you can see on the example above, an instance of `Levtus` (app) contains the router. Levtus has provided a shortcut for you, by calling `app.method("path", ctx -> {})` you can register a path.

## 📑 Registering a path

As explained above, you call the method of your path on the app instance. Currently Levtus supports GET, POST, PUT, and DELETE:

```java
void main(){
    Levtus app = Levtus.create();

    app.get("/hello", ctx -> {
      ctx.text("An incoming " + ctx.req().method() + "request!" );
    });

    app.post("/", ctx -> {
      ctx.text(ctx.req().body().toString());
    });

    app.put("/", ctx -> {
      ctx.text(ctx.req().body().toString());
    });
}
```
> Note; Query and Patch method will be coming soon in v0.3! 🐧🐧🐧

## 🏷️ Path Parameters (Wildcards)

Levtus supports dynamic routing using curly braces `{}`. These parameters are extracted and made available through the `ctx.param()` method.

```java
app.get("/user/{id}", ctx -> {
    String userId = ctx.param("id");
    ctx.json("{\"id\": \"" + userId + "\"}");
});

app.get("/posts/{category}/{slug}", ctx -> {
    String category = ctx.param("category");
    String slug = ctx.param("slug");
    ctx.text("Category: " + category + ", Post: " + slug);
});
```

The router uses a high-performance Prefix Tree (Trie), ensuring that parameter extraction is fast and efficient regardless of the number of routes.
