package io.github.bernardusz.levtus.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RouterTest {
    private Router router;
    private LevtusContext mockContext;
    private Request mockRequest;
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        router = new Router();
        mockContext = Mockito.mock(LevtusContext.class);
        mockRequest = Mockito.mock(Request.class);
        mockResponse = Mockito.mock(Response.class);

        Mockito.when(mockContext.req()).thenReturn(mockRequest);
        Mockito.when(mockContext.res()).thenReturn(mockResponse);
        Mockito.when(mockResponse.status(Mockito.anyInt())).thenReturn(mockResponse);
    }

    @Test
    void testBasicGetRouteMatching() {
        AtomicBoolean handled = new AtomicBoolean(false);
        router.get("/hello", ctx -> handled.set(true));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/hello");

        router.handle(mockContext);

        assertEquals(true, handled.get(), "Handler should have been executed");
    }

    @Test
    void testCaseInsensitiveMatching() {
        AtomicBoolean handled = new AtomicBoolean(false);
        router.get("/HELLO", ctx -> handled.set(true));

        Mockito.when(mockRequest.method()).thenReturn("get");
        Mockito.when(mockRequest.path()).thenReturn("/hello");

        router.handle(mockContext);

        assertEquals(true, handled.get(), "Should match regardless of case");
    }

    @Test
    void testRootRoute() {
        AtomicBoolean handled = new AtomicBoolean(false);
        router.get("/", ctx -> handled.set(true));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/");

        router.handle(mockContext);

        assertEquals(true, handled.get(), "Should match root route");
    }

    @Test
    void testTrailingAndDoubleSlashes() {
        AtomicBoolean handled = new AtomicBoolean(false);
        router.get("/hello", ctx -> handled.set(true));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        
        Mockito.when(mockRequest.path()).thenReturn("/hello/");
        router.handle(mockContext);
        assertEquals(true, handled.get(), "Should match with trailing slash");

        handled.set(false);
        Mockito.when(mockRequest.path()).thenReturn("//hello");
        router.handle(mockContext);
        assertEquals(true, handled.get(), "Should match with double slash");
    }

    @Test
    void testOverlappingRoutes() {
        AtomicBoolean staticHandled = new AtomicBoolean(false);
        AtomicBoolean wildcardHandled = new AtomicBoolean(false);

        router.get("/user/profile", ctx -> staticHandled.set(true));
        router.get("/user/{id}", ctx -> wildcardHandled.set(true));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/user/profile");

        router.handle(mockContext);

        assertEquals(true, staticHandled.get(), "Static route should take precedence");
        assertEquals(false, wildcardHandled.get(), "Wildcard route should not be called");
    }

    @Test
    void testMultipleWildcards() {
        AtomicBoolean handled = new AtomicBoolean(false);
        router.get("/user/{userId}/post/{postId}", ctx -> handled.set(true));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/user/42/post/100");

        router.handle(mockContext);

        assertEquals(true, handled.get());
        Mockito.verify(mockContext).setPathParams(Mockito.argThat(params -> 
            "42".equals(params.get("userId")) && "100".equals(params.get("postId"))
        ));
    }

    @Test
    void testOtherHttpMethods() {
        AtomicBoolean postHandled = new AtomicBoolean(false);
        AtomicBoolean putHandled = new AtomicBoolean(false);
        AtomicBoolean deleteHandled = new AtomicBoolean(false);

        router.post("/test", ctx -> postHandled.set(true));
        router.put("/test", ctx -> putHandled.set(true));
        router.delete("/test", ctx -> deleteHandled.set(true));

        Mockito.when(mockRequest.path()).thenReturn("/test");

        Mockito.when(mockRequest.method()).thenReturn("POST");
        router.handle(mockContext);
        assertEquals(true, postHandled.get());

        Mockito.when(mockRequest.method()).thenReturn("PUT");
        router.handle(mockContext);
        assertEquals(true, putHandled.get());

        Mockito.when(mockRequest.method()).thenReturn("DELETE");
        router.handle(mockContext);
        assertEquals(true, deleteHandled.get());
    }

    @Test
    void testMiddlewareTermination() {
        AtomicBoolean handlerExecuted = new AtomicBoolean(false);
        
        // Middleware that doesn't call next.run()
        router.use((ctx, next) -> {
            ctx.send(401, "Unauthorized");
        });

        router.get("/test", ctx -> handlerExecuted.set(true));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/test");

        router.handle(mockContext);

        assertEquals(false, handlerExecuted.get(), "Handler should NOT have been executed");
        Mockito.verify(mockContext).send(401, "Unauthorized");
    }

    @Test
    void testRouteNotFound() {
        router.get("/hello", ctx -> {});

        // Case 1: Method not found (e.g. OPTIONS if not registered)
        Mockito.when(mockRequest.method()).thenReturn("OPTIONS");
        Mockito.when(mockRequest.path()).thenReturn("/hello");
        router.handle(mockContext);
        Mockito.verify(mockResponse).status(404);
        Mockito.verify(mockResponse).send("404 - Not Found");

        // Case 2: Path not found
        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/not-found");
        router.handle(mockContext);
        Mockito.verify(mockContext).send(Mockito.eq(404), Mockito.anyString());
    }

    @Test
    void testWildcardRouting() {
        AtomicBoolean handled = new AtomicBoolean(false);
        router.get("/user/{id}", ctx -> {
            handled.set(true);
        });

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/user/123");

        router.handle(mockContext);

        assertEquals(true, handled.get(), "Wildcard handler should have been executed");
        Mockito.verify(mockContext).setPathParams(Mockito.argThat(params -> 
            "123".equals(params.get("id"))
        ));
    }

    @Test
    void testConflictingWildcards() {
        router.get("/test/{first}", ctx -> {});
        
        assertThrows(IllegalStateException.class, () -> {
            router.get("/test/{second}", ctx -> {});
        }, "Should throw exception when adding two different wildcards in the same position");
    }

    @Test
    void testMiddlewareExecution() {
        StringBuilder executionOrder = new StringBuilder();
        
        router.use((ctx, next) -> {
            executionOrder.append("M1-Start ");
            next.run();
            executionOrder.append("M1-End");
        });

        router.get("/test", ctx -> executionOrder.append("Handler "));

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/test");

        router.handle(mockContext);

        assertEquals("M1-Start Handler M1-End", executionOrder.toString());
    }

    @Test
    void testUtf8Decoder() {
        assertEquals("test", router.utf8Decoder("test"));
        assertEquals("test space", router.utf8Decoder("test%20space"));
        assertEquals("😀", router.utf8Decoder("%F0%9F%98%80"));
        assertEquals("你好", router.utf8Decoder("%E4%BD%A0%E5%A5%BD"));
        assertEquals("special!@#$%^&*()", router.utf8Decoder("special!@#$%25^&*()"));
    }
}
