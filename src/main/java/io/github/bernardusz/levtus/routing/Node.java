package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a single node within the internal Prefix Tree (Trie) used for request routing.
 *
 * <p>Each node corresponds to a specific segment of a URI path or an HTTP method. Nodes can have
 * static children (exact matches) or a single wildcard child (parameterized matches).
 */
public class Node {
  /**
   * A map of static path segments to their corresponding child nodes. The keys are stored in
   * uppercase to ensure case-insensitive matching.
   */
  final Map<String, Node> children = new HashMap<>();

  /**
   * The child node responsible for handling wildcard/parameterized path segments (e.g., "{id}").
   * Only one wildcard child is allowed per node level.
   */
  Node wildcardChild = null;

  /**
   * The name of the wildcard parameter (e.g., "id" for the segment "{id}"), used to extract the
   * value during routing.
   */
  String wildcardName = null;

  /**
   * The functional handler associated with this node. If non-null, this node represents a terminal
   * point of a registered route.
   *
   * <p>TLDR: The handler/lambda that takes LevtusContext and is the function that is called when a route is matched.</p>
   */
  Consumer<LevtusContext> handler;

  /**
   * Determines if this node represents a wildcard path segment.
   *
   * @return {@code true} if the node has an associated wildcard name, {@code false} otherwise
   */
  boolean isWildcard() {
    return wildcardName != null;
  }
}
