# pedestal.sqs

FIXME


## Configuration example

```
{
 ;; read more in https://github.com/cognitect-labs/aws-api
 ::sqs/client             {:region            "us-east-1"
                           :endpoint-override {:protocol :http
                                               :hostname "localhost"
                                               :port     9324}}

 ::sqs/configurations     {:auto-create-queue? true}

 ;; Arguments
 ;; queue-name (e.g. foo-queue)
 ;; listener function (e.g. foo-listener)
 ;; queue/listener configurations of library and aws-api (here a shortcut to (aws/doc :ReceiveMessage))
 ;;
 ;; Comments about listeners
 ;; reference of ::sqs/deletion-policy https://github.com/spring-cloud/spring-cloud-aws/blob/v2.1.2.RELEASE/spring-cloud-aws-messaging/src/main/java/org/springframework/cloud/aws/messaging/listener/SqsMessageDeletionPolicy.java#L45
 ::sqs/listeners          #{["foo-queue" foo-listener {:WaitTimeSeconds      20
                                                       ::sqs/deletion-policy :always
                                                       ::sqs/response-type   :json}]
                            ["bar-queue" bar-listener {::sqs/deletion-policy       :on-success
                                                       ::sqs/response-interceptors [sqs.interceptors/json-parser]}]
                            ["egg-queue" egg-listener {:WaitTimeSeconds 10}]}
}
```

Read more in [https://github.com/RenanPalmeira/pedestal.sqs/blob/master/src/pedestal/sample/service.clj#L74-L91](https://github.com/RenanPalmeira/pedestal.sqs/blob/master/src/pedestal/sample/service.clj#L74-L91)

## Getting Started

1. Start the application: `lein run`
2. Go to [localhost:8080](http://localhost:8080/) to see: `Hello World!`
3. Read your app's source code at src/pedestal/sqs/service.clj. Explore the docs of functions
   that define routes and responses.
4. Run your app's tests with `lein test`. Read the tests at test/pedestal/sqs/service_test.clj.
5. Learn more! See the [Links section below](#links).


## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).


## Developing your service

1. Start a new REPL: `lein repl`
2. Start your service in dev-mode: `(def dev-serv (run-dev))`
3. Connect your editor to the running REPL session.
   Re-evaluated code will be seen immediately in the service.

### [Docker](https://www.docker.com/) container support

1. Configure your service to accept incoming connections (edit service.clj and add  ::http/host "0.0.0.0" )
2. Build an uberjar of your service: `lein uberjar`
3. Build a Docker image: `sudo docker build -t pedestal.sqs .`
4. Run your Docker image: `docker run -p 8080:8080 pedestal.sqs`

### [OSv](http://osv.io/) unikernel support with [Capstan](http://osv.io/capstan/)

1. Build and run your image: `capstan run -f "8080:8080"`

Once the image it built, it's cached.  To delete the image and build a new one:

1. `capstan rmi pedestal.sqs; capstan build`


## Links
* [Other Pedestal examples](http://pedestal.io/samples)
