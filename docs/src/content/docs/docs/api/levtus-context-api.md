---
title: LevtusContext API
description: Documentation on how to handle the incoming HTTP Request and outgoing HTTP Response from a single wrapper; LevtusContext.
---

Yep, both Request and Request are wrapped inside LevtusContext. Every example you saw in [Request](/docs/api/request-api) and [Response](/docs/api/response-api) are calling the wrapped Request and Response inside LevtusContext respectively.

Every single method inside Request and Response have been wrapped inside LevtusContext so you can immediately call them. For example:

```java
app.get("/header", ctx -> {
    String userAgent = ctx.header("User-Agent"); // Get the first value
    List<String> cookies = ctx.headers("Cookie"); // Get the whole list of value
    Map<String, List<String>> = ctx.headers(); // Get the whole headers list.
});
```

So every method is available inside LevtusContext; Do not worry 🐧🐧🐧

## Path Param - Wildcard
> However, there is one method unique to LevtusContext, and that is `param()` and `params()`, this is to get the value of the Path Param or Wildcard route in the full path. This will return empty string or empty Map respectively.

```java
app.get("/users/{id}", ctx -> {
    String userId = ctx.param("id"); // Get the single param of id
    HashMap<String, String> params = ctx.params(); // Get the full map of path params
});
```