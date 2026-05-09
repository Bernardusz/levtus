package io.github.bernardusz.levtus.http;

import java.util.Map;

/**
 * The type Levtus context, a wrapper for Request and Response.
 */
public class LevtusContext {
  /**
   * The Request object of the context.
   */
  Request req;
  /**
   * The Response object of the context.
   */
  Response res;

  /** The Path params. */
  Map<String, String> pathParams;

  /**
   * Instantiates a new Levtus context.
   *
   * @param req the req
   * @param res the res
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
   * @return  the response
   */
public Response res() {
    return res;
  }

  /**
   * Sets path params.
   *
   * @param pathParams the path params
   */
public void setPathParams(Map<String, String> pathParams) {
    this.pathParams = pathParams;
  }

  /**
   * Param string.
   *
   * @param name the name
   * @return  the string
   */
public String param(String name) {
    return pathParams != null ? pathParams.getOrDefault(name, "") : "";
  }

  /**
   * Query string.
   *
   * @param name the name
   * @return  the string
   */
public String query(String name) {
    return req.query(name);
  }

  /**
   * Send.
   *
   * @param data the data
   */
public void send(String data) {
    res.send(data);
  }

  /**
   * Send.
   *
   * @param code the code
   * @param data the data
   */
public void send(int code, String data) {
    res.status(code).send(data);
  }

  /**
   * Send.
   *
   * @param code the code
   * @param contentType the content type
   * @param data the data
   */
public void send(int code, String contentType, String data) {
    res.status(code).contentType(contentType);
    res.send(data);
  }

  /**
   * Html.
   *
   * @param html the html
   */
public void html(String html) {
    res.html(html);
  }

  /**
   * Text.
   *
   * @param text the text
   */
public void text(String text) {
    res.text(text);
  }

  /**
   * Send binary.
   *
   * @param body the body
   */
public void sendBinary(byte[] body) {
    res.sendBinary(body);
  }

  /**
   * Send the body of the response (String JSON).
   *
   * @param json the String JSON
   */
public void json(String json) {
    res.json(json);
  }

  /**
   * Send the body of the response (HTML file).
   *
   * @param htmlPath the path of HTML file
   */
public void render(String htmlPath) {
    res.render(htmlPath);
  }
}
