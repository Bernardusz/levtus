package io.github.bernardusz.levtus.http;

import java.util.Map;

/**
 * The LevtusContext that wraps HTTP Requests and Output Stream as {@link Request} and {@link Response}.
 *
 * <p>Responsible for:</p>
 * <ul>
 *   <li>Wrapping HTTP Requests and Output Stream</li>
 *   <li>Handling, Setting, and Saving Path Parameters</li>
 *   <li>Handling Query Parameters</li>
 *   <li>Handling Request Body</li>
 *   <li>Handling Response transmission</li>
 * </ul>
 * @author Bernardusz
 * @version 0.1.1
 */
public class LevtusContext {
  /** The Request object that represents an incoming HTTP/1.1 request. */
  Request req;

  /** The fully instantiated Response object that represents an outgoing HTTP/1.1 response. */
  Response res;

  /**
   * The path parameters extracted from the URI based on the route pattern (wildcards).
   * For example, in a route "/users/{id}", the value of "{id}" is stored here.
   */
  Map<String, String> pathParams;

  /**
   * Instantiates a new LevtusContext, setting the Request and Response objects.
   *
   * @param req the incoming request
   * @param res the outgoing response
   */
  public LevtusContext(Request req, Response res) {
    this.req = req;
    this.res = res;
  }

  /**
   * Return the Request object.
   *
   * @return the Request object
   */
  public Request req() {
    return req;
  }

  /**
   * Return the Response object.
   *
   * @return the response
   */
  public Response res() {
    return res;
  }

  /**
   * Sets the path parameters (wildcards) extracted from the URI based on the route pattern.
   *
   * @implNote This replaces the entire path parameters map, it does not append to it.
   *
   * @param pathParams the map of extracted path parameters
   */
  public void setPathParams(Map<String, String> pathParams) {
    this.pathParams = pathParams;
  }

  /**
   * Retrieves the value of a path parameter (wildcard) by its name.
   *
   * <p>{@code String userId = ctx.param("id");}
   *
   * @param name the name of the path parameter (defined in the route pattern)
   * @return the parameter value, or an empty string if not found
   */
  public String param(String name) {
    return pathParams != null ? pathParams.getOrDefault(name, "") : "";
  }

  /**
   * Retrieves the value of a specific query parameter by its name.
   *
   * @param name the name of the query parameter
   * @return the query parameter value, or an empty string if not found
   */
  public String query(String name) {
    return req.query(name);
  }

  /**
   * Send a plain String as data (text/plain) through the Response.
   *
   * @param data the string data to send
   */
  public void send(String data) {
    res.send(data);
  }

  /**
   * Send a plain String as data (text/plain) through the Response with a custom status code.
   *
   * @param code the HTTP status code
   * @param data the string data to send
   */
  public void send(int code, String data) {
    res.status(code).send(data);
  }

  /**
   * Send a plain String as data through the Response with a custom status code and content type.
   *
   * @param code the HTTP status code
   * @param contentType the MIME type of the content (e.g., "application/json")
   * @param data the string data to send
   */
  public void send(int code, String contentType, String data) {
    res.status(code).contentType(contentType);
    res.send(data);
  }

  /**
   * Send an HTML String as data (text/html) through the Response.
   *
   * @param html the html
   */
  public void html(String html) {
    res.html(html);
  }

  /**
   * Send a plain String as data (text/plain) through the Response.
   *
   * @param text the text
   */
  public void text(String text) {
    res.text(text);
  }

  /**
   * Send a binary array as data (application/octet-stream) through the Response.
   *
   * @param body the body
   */
  public void sendBinary(byte[] body) {
    res.sendBinary(body);
  }

  /**
   * Send a JSON String as data (application/json) through the Response.
   *
   * @param json the String JSON
   */
  public void json(String json) {
    res.json(json);
  }

  /**
   * Send an HTML file as data (text/html) through the Response.
   *
   * @param htmlPath the path of HTML file
   */
  public void render(String htmlPath) {
    res.render(htmlPath);
  }
}
