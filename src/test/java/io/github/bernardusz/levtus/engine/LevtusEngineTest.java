package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.routing.Router;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class LevtusEngineTest {
  @Mock
  private HttpParser mockHttpParser;
  @Mock
  private Router mockRouter;
  private LevtusEngine engine;

  @BeforeEach
  void setUp() {
    engine = new LevtusEngine(mockRouter);
  }

  @Test
  void testStart() {
    engine.start(8080);
    verify(mockRouter).start();
  }
}
