package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Node, represents a node in the routing tree. */
public class Node {
  /** The Children. */
  final Map<String, Node> children = new HashMap<>();

  /** The Wildcard child. */
  Node wildcardChild = null;

  /** The Wildcard name. */
  String wildcardName = null;

  /** The Handler. */
  Consumer<LevtusContext> handler;

  /**
   * Is wildcard boolean.
   *
   * @return the boolean
   */
  boolean isWildcard() {
    return wildcardName != null;
  }
}
