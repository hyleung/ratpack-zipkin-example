#Ratpack-Zipkin Demo App

Demo Ratpack service using [ratpack-zipkin](https://github.com/hyleung/ratpack-zipkin) to add Zipkin tracing.

## Running the service

```
./gradlew clean run -DokhttpHost=localhost
```

This will start the service and enable Zipkin tracing using the Http transport.
