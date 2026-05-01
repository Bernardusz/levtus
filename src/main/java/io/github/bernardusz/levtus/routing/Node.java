package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Node {
     final Map<String, Node> children = new HashMap<>();
     Node wildcardChild = null;
     String wildcardName = null;

     Consumer<LevtusContext> handler;

     boolean isWildcard(){
        return wildcardName != null;
     }
}
