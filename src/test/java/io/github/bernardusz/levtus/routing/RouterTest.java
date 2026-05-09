package io.github.bernardusz.levtus.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

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
    void testRouteNotFound() {
        router.get("/hello", ctx -> {});

        Mockito.when(mockRequest.method()).thenReturn("GET");
        Mockito.when(mockRequest.path()).thenReturn("/not-found");

        router.handle(mockContext);

        // Verify that 404 was sent (either via ctx.send or ctx.res().status().send())
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
}
