package io.github.bernardusz.levtus.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bernardusz.levtus.routing.Router;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LevtusEngineTest {

  @Mock private Router mockRouter;
  @Mock private Socket mockSocket;

  private LevtusEngine engine;

  @BeforeEach
  void setUp() {
    engine = new LevtusEngine(mockRouter);
  }

  @Test
  void testDelegationMethods() {
    engine.setMaxBodySize(1024);
    assertEquals(1024, engine.getMaxBodySize());
    assertEquals(1024, engine.handler.getMaxBodySize());

    engine.setMaxHeaderCount(50);
    assertEquals(50, engine.getMaxHeaderCount());
    assertEquals(50, engine.handler.getMaxHeaderCount());

    engine.setMaxHeaderSize(200);
    assertEquals(200, engine.getMaxHeaderSize());
    assertEquals(200, engine.handler.getMaxHeaderSize());

    engine.setMaxLineSize(300);
    assertEquals(300, engine.getMaxLineSize());
    assertEquals(300, engine.handler.getMaxLineSize());

    engine.setMaxEmptyLines(5);
    assertEquals(5, engine.getMaxEmptyLines());
    assertEquals(5, engine.handler.getMaxEmptyLines());

    engine.setMaxConcurrentConnections(500);
    assertEquals(500, engine.getMaxConcurrentConnections());
  }

  @Test
  void testSendOverloadedResponse() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    engine.sendOverloadedResponse(mockSocket);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(response.contains("503 Service Unavailable"));
    assertTrue(response.contains("Server Overloaded"));

    verify(mockSocket).close();
  }
}
