package com.example.server;

import brave.sampler.Sampler;
import ratpack.guice.Guice;
import ratpack.logging.MDCInterceptor;
import ratpack.server.RatpackServer;
import ratpack.zipkin.ServerTracingModule;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.kafka11.KafkaSender;
import zipkin2.reporter.okhttp3.OkHttpSender;

/*
 * RatPack Server.
 */
public class App {
  public static void main(String[] args) throws Exception {
    Integer serverPort = Integer.parseInt(System.getProperty("port", "8081"));
    Float samplingPct = Float.parseFloat(System.getProperty("samplingPct","1"));

    RatpackServer.start(server -> server
        .serverConfig(config -> config.port(serverPort))
        .registry(Guice.registry(binding -> binding
            .module(ServerTracingModule.class, config -> {
              config
                  .serviceName("ratpack-demo")
                  .sampler(Sampler.create(samplingPct))
                  .spanReporter(spanReporter());
            })
            .bind(HelloWorldHandler.class)
            .add(MDCInterceptor.instance())
        ))
        .handlers(chain -> chain
            .get("hello", HelloWorldHandler.class)
            .all(ctx -> ctx.render("root")))
    );

    RatpackServer.start(server -> server
        .serverConfig(config -> config.port(serverPort + 1))
        .registry(Guice.registry(binding -> binding
            .module(ServerTracingModule.class, config -> config
                .serviceName("other-server")
                .sampler(Sampler.create(samplingPct))
                .spanReporter(spanReporter()))
            .bind(HelloWorldHandler.class)
            .add(MDCInterceptor.instance())
        ))
        .handlers(chain -> chain
            .all(ctx -> ctx.render("root")))
    );
  }

  private static Reporter<Span> spanReporter() {
    String kafkaHost = System.getProperty("kafkaHost");
    String okHttpHost = System.getProperty("okhttpHost");
    if (kafkaHost != null) {
      KafkaSender kafkaSender = KafkaSender
          .create(kafkaHost)
          .toBuilder()
          .topic("zipkin")
          .build();
      return AsyncReporter.create(kafkaSender);
    } else if (okHttpHost != null) {
      OkHttpSender okHttpSender = OkHttpSender
          .create(String.format("http://%s:9411/api/v2/spans", okHttpHost));
      return AsyncReporter.create(okHttpSender);
    } else {
      return Reporter.CONSOLE;
    }
  }

}
