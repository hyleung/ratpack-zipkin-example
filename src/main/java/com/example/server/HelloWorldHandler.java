/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.server;

import brave.Span;
import brave.Tracer;
import brave.http.HttpTracing;
import com.github.kristofa.brave.KeyValueAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.server.ServerConfig;
import ratpack.zipkin.Zipkin;

import javax.inject.Inject;
import java.net.URI;

public class HelloWorldHandler implements Handler {
  private final Logger logger = LoggerFactory.getLogger(HelloWorldHandler.class);
  @Inject
  private HttpTracing httpTracing;
  @Inject
  @Zipkin
  private HttpClient client;

  @Override
  public void handle(final Context ctx) throws Exception {
    ctx.getExecution().add(KeyValueAnnotation.create("some-key", "some-value"));
    Tracer tracer = httpTracing.tracing().tracer();
    Span currentSpan = tracer.currentSpan();
    final Span child = tracer.newChild(currentSpan.context()).name("HelloWorld handler").start();
    final Tracer.SpanInScope scope = tracer.withSpanInScope(child);
    client.get(new URI("http://localhost:" + (ctx.get(ServerConfig.class).getPort() + 1) + "/"))
          .map(ReceivedResponse::getStatusCode)
          .then(a -> {
            ctx.getResponse().send("Yo dawg.");
            child.finish();
            scope.close();
          });

  }
}
