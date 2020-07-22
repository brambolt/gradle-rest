/*
 * Copyright 2017-2020 Brambolt ehf.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brambolt.gradle.rest.tasks

import groovy.json.JsonSlurper
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.ContentProvider
import org.eclipse.jetty.client.api.ContentResponse
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.MultiPartContentProvider
import org.eclipse.jetty.client.util.PathContentProvider
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.util.Fields
import org.eclipse.jetty.util.Fields.Field
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

import static com.brambolt.gradle.text.Strings.bind
import static com.brambolt.gradle.text.Json.minify

class RestApiTask extends DefaultTask {

  @TaskAction
  void apply() {
    HttpClient client
    try {
      client = new HttpClient()
      client.start()
      apply(client)
    } finally {
      if (null != client)
        client.stop()
    }
  }

  void apply(HttpClient client) {
    // Default implementation does not nothing with the client
  }

  ContentResponse form(HttpClient client, String url, Map pairs) {
    client.FORM(url, createFields(pairs))
  }

  Fields createFields(Map pairs) {
    pairs.inject(new Fields()) { Fields fields, Map.Entry pair ->
      addFormField(fields, (String) pair.getKey().toString(), pair.getValue().toString())
    }
  }

  Fields addFormField(Fields fields, String name, String value) {
    fields.put(createFormField(name, value))
    fields
  }

  Field createFormField(String name, String value) {
    new Field(name, value)
  }

  ContentResponse get(HttpClient client, String url) {
    get(client, url, [:], [:])
  }

  ContentResponse get(HttpClient client, String url, Map headers) {
    get(client, url, headers, [:])
  }

  ContentResponse get(HttpClient client, String url, Map headers, Map cookies) {
    send(client, HttpMethod.GET, url, headers, cookies, null)
  }

  ContentResponse get(HttpClient client, String url, Map headers, List cookies) {
    send(client, HttpMethod.GET, url, headers, cookies, (ContentProvider) null)
  }

  ContentResponse post(HttpClient client, String url, String content) {
    post(client, url, [:], [:], content)
  }

  ContentResponse post(HttpClient client, String url, Map headers, String content) {
    post(client, url, headers, [:], content)
  }

  ContentResponse post(HttpClient client, String url, Map headers, Map cookies, String content) {
    send(client, HttpMethod.POST, url, headers, cookies, content)
  }

  ContentResponse post(HttpClient client, String url, Map headers, List cookies, String content) {
    send(client, HttpMethod.POST, url, headers, cookies, content)
  }

  ContentResponse put(HttpClient client, String url, String content) {
    put(client, url, [:], [:], content)
  }

  ContentResponse put(HttpClient client, String url, Map headers, String content) {
    put(client, url, headers, [:], content)
  }

  ContentResponse put(HttpClient client, String url, Map headers, Map cookies, String content) {
    send(client, HttpMethod.PUT, url, headers, cookies, content)
  }

  ContentResponse put(HttpClient client, String url, Map headers, List cookies, String content) {
    send(client, HttpMethod.PUT, url, headers, cookies, content)
  }

  ContentResponse delete(HttpClient client, String url) {
    delete(client, url, [:], [:], null)
  }

  ContentResponse delete(HttpClient client, String url, String content) {
    delete(client, url, [:], [:], content)
  }

  ContentResponse delete(HttpClient client, String url, Map headers) {
    delete(client, url, headers, [:], null)
  }

  ContentResponse delete(HttpClient client, String url, Map headers, String content) {
    delete(client, url, headers, [:], content)
  }

  ContentResponse delete(HttpClient client, String url, Map headers, List cookies) {
    send(client, HttpMethod.DELETE, url, headers, cookies, null)
  }

  ContentResponse delete(HttpClient client, String url, Map headers, List cookies, String content) {
    send(client, HttpMethod.DELETE, url, headers, cookies, content)
  }

  ContentResponse delete(HttpClient client, String url, Map headers, Map cookies) {
    send(client, HttpMethod.DELETE, url, headers, cookies, null)
  }

  ContentResponse delete(HttpClient client, String url, Map headers, Map cookies, String content) {
    send(client, HttpMethod.DELETE, url, headers, cookies, content)
  }

  ContentResponse send(HttpClient client, HttpMethod method, String url, Map headers, Map cookies, String content) {
    send(client, method, url, headers, createCookies(cookies), content)
  }

  List<HttpCookie> createCookies(Map pairs) {
    pairs.inject([]) { List<HttpCookie> cookies, Map.Entry pair ->
      addCookie(cookies, (String) pair.getKey(), (String) pair.getValue())
    }
  }

  List<HttpCookie> addCookie(List<HttpCookie> cookies, String name, String value) {
    cookies.add(createCookie(name, value))
    cookies
  }

  HttpCookie createCookie(String name, String value) {
    new HttpCookie(name, value)
  }

  ContentResponse send(HttpClient client, HttpMethod method, String url, Map headers, List<HttpCookie> cookies, String content) {
    send(client, method, url, headers, cookies, null != content ? new StringContentProvider(content) : null)
  }

  ContentResponse send(HttpClient client, HttpMethod method, String url, Map headers, List<HttpCookie> cookies, ContentProvider content) {
    Request request = client.newRequest(url).method(method)
    headers.inject(request) { Request r, Map.Entry header ->
      r.header(header.getKey().toString(), header.getValue().toString())
    }
    cookies.inject(request) { Request r, HttpCookie cookie ->
      r.cookie(cookie)
    }
    if (null != content)
      request.content(content)
    request.send()
  }

  ContentResponse getJson(HttpClient client, String url) {
    getJson(client, url, [:], [])
  }

  ContentResponse getJson(HttpClient client, String url, Map headers) {
    getJson(client, url, headers, [])
  }

  ContentResponse getJson(HttpClient client, String url, Map headers, List cookies) {
    send(client, HttpMethod.GET, url, createJsonHeaders(headers), cookies, (ContentProvider) null)
  }

  List asList(ContentResponse response, Closure message) {
    (List) asObject(response, message)
  }

  Map asMap(ContentResponse response, Closure message) {
    (Map) asObject(response, message)
  }

  Object asObject(ContentResponse response, Closure message) {
    new JsonSlurper().parseText(asString(response, message))
  }

  String asString(ContentResponse response, Closure message) {
    throwIfNot200(response, message)
    response.contentAsString
  }

  ContentResponse postJson(HttpClient client, String url, String json) {
    postJson(client, url, [:], [], json)
  }

  ContentResponse postJson(HttpClient client, String url, Map headers, String content) {
    postJson(client, url, headers, [], content)
  }

  ContentResponse postJson(HttpClient client, String url, Map headers, List cookies, String json) {
    sendJson(client, HttpMethod.POST, url, headers, cookies, json, null)
  }

  ContentResponse postJson(HttpClient client, String url, Map headers, List cookies, String json, Map bindings) {
    sendJson(client, HttpMethod.POST, url, headers, cookies, json, bindings)
  }

  ContentResponse putJson(HttpClient client, String url, String json) {
    putJson(client, url, [:], [], json)
  }

  ContentResponse putJson(HttpClient client, String url, Map headers, String content) {
    putJson(client, url, headers, [], content)
  }

  ContentResponse putJson(HttpClient client, String url, Map headers, List cookies, String json) {
    sendJson(client, HttpMethod.PUT, url, headers, cookies, json, null)
  }

  ContentResponse putJson(HttpClient client, String url, Map headers, List cookies, String json, Map bindings) {
    sendJson(client, HttpMethod.PUT, url, headers, cookies, json, bindings)
  }

  ContentResponse deleteWithJson(HttpClient client, String url, Map headers, String json) {
    deleteWithJson(client, url, headers, [], json, null)
  }

  ContentResponse deleteWithJson(HttpClient client, String url, Map headers, List cookies, String json) {
    deleteWithJson(client, url, headers, cookies, json, null)
  }

  ContentResponse deleteWithJson(HttpClient client, String url, Map headers, List cookies, String json, Map bindings) {
    sendJson(client, HttpMethod.DELETE, url, headers, cookies, json, bindings)
  }

  ContentResponse sendJson(HttpClient client, HttpMethod method, String url, Map headers, List cookies, String json) {
    sendJson(client, HttpMethod.PUT, url, headers, cookies, json, null)
  }

  ContentResponse sendJson(HttpClient client, HttpMethod method, String url, Map headers, List cookies, String json, Map bindings) {
    String minified = minify(bind(json, bindings))
    project.logger.debug("Sending JSON content: ${minified}")
    send(client, method, url, createJsonHeaders(headers, json), cookies, minified)
  }

  ContentResponse multiPart(HttpClient client, String url, Map headers, Map parts) {
    send(client, HttpMethod.POST, url, headers, [], createMultiPart(parts))
  }

  MultiPartContentProvider createMultiPart(Map parts) {
    MultiPartContentProvider multiPart = new MultiPartContentProvider()
    parts.each { part -> addPart(multiPart, part) }
    multiPart.close()
    multiPart
  }

  void addPart(MultiPartContentProvider multiPart, Map.Entry part) {
    switch (part.type) {
      case 'file':
        multiPart.addFilePart(
          (String) part.field,
          (String) part.basename,
          new PathContentProvider(Paths.get((String) part.path)),
          null)
        break
      default:
        throw new GradleException("Unsupported part type: ${part}")
    }
  }

  Map createJsonHeaders(Map overrides) {
    createJsonHeaders(overrides, null)
  }

  Map createJsonHeaders(Map overrides, String json) {
    Map headers = [
      (HttpHeader.ACCEPT)      : 'application/json',
      (HttpHeader.CONTENT_TYPE): 'application/json'
    ]
    headers.putAll(overrides)
    headers
  }

  void throwIfNot200(ContentResponse response, String message) {
    throwIfNot(HttpStatus.OK_200, response, message)
  }

  void throwIfNot200(ContentResponse response, Closure message) {
    throwIfNot(HttpStatus.OK_200, response, message)
  }

  void throwIfNot201(ContentResponse response, String message) {
    throwIfNot(HttpStatus.CREATED_201, response, message)
  }

  void throwIfNot201(ContentResponse response, Closure message) {
    throwIfNot(HttpStatus.CREATED_201, response, message)
  }

  void throwIfNot(int expectedStatus, ContentResponse response, String message) {
    if (response.getStatus() != expectedStatus)
      throw new GradleException(message)
  }

  void throwIfNot(int expectedStatus, ContentResponse response, Closure message) {
    if (response.getStatus() != expectedStatus)
      throw new GradleException(message.call(response).toString())
  }
}
