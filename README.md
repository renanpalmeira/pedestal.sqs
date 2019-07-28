
# pedestal.sqs

[![Build Status](https://travis-ci.org/RenanPalmeira/pedestal.sqs.svg?branch=master)](https://travis-ci.org/RenanPalmeira/pedestal.sqs)
[![Clojars Project](https://img.shields.io/clojars/v/pedestal.sqs.svg)](https://clojars.org/pedestal.sqs)

A simple Pedestal interface for AWS SQS.

**Requires Clojure 1.10.*, Java 1.8+ and Servlet 3.1**

## Usage

In your service map is necessary put `::sqs/client` and `::sqs/listeners`  , optional `::sqs/configurations`

### SQS Client

Here is a shortcut to [aws-api](https://github.com/cognitect-labs/aws-api), to use a local sqs server follow [https://github.com/cognitect-labs/aws-api#endpoint-override](https://github.com/cognitect-labs/aws-api#endpoint-override) to configure AWS credentials [https://github.com/cognitect-labs/aws-api#credentials](https://github.com/cognitect-labs/aws-api#credentials)

```
{
 ;; read more in https://github.com/cognitect-labs/aws-api
 ::sqs/client {:region            "us-east-1"
               :endpoint-override {:protocol :http
                                   :hostname "localhost"
                                   :port     9324}}
}
```

### SQS Listener

Time to be happy, here is just pass name of queue, a listener function to receive messages and configurations of queue (like response type, deletion policy)

```
{
 ::sqs/listeners #{["foo-queue" foo-listener {:WaitTimeSeconds      20
                                              ::sqs/deletion-policy :always
                                              ::sqs/response-type   :json}]
                   ["bar-queue" bar-listener {::sqs/deletion-policy       :on-success
                                              ::sqs/response-interceptors [sqs.interceptors/json-parser]}]
                   ["egg-queue" egg-listener {:WaitTimeSeconds 10}]}
}
```

Example listener function

```

(defn foo-listener
  [{:keys [message]}]
  (prn message))

;; => {:MessageId "29d942a3-fd85-4f47-bf5d-d966f102364f"
       :ReceiptHandle "29d942a3-fd85-4f47-bf5d-d966f102364f#60b168b7-3a33-482e-a0e3-a9dd77b679b7"
       :MD5OfBody "9d822b36a135bc8d94a09b67128cb63b"
       :Body {:test "test"}}

```

Follow available queue configurations write by pedestal.sqs

```
{
 ::sqs/deletion-policy :on-success ;; options :never, :on-success and :always, default is :never
 ::sqs/response-interceptors ``[sqs.interceptors/json-parser] ;; here we have access a put interceptors to manage received messages
 ::sqs/response-type :json ;; built-in :json, :transit-json, :transit-mgspack, default is string
}
```

Another configurations come from [aws-api](https://github.com/cognitect-labs/aws-api), using `(aws/doc client :ReceiveMessage)` we have this configurations

```
{:AttributeNames [:seq-of string],
 :MessageAttributeNames [:seq-of string],
 :MaxNumberOfMessages integer,
 :VisibilityTimeout integer,
 :WaitTimeSeconds integer,
 :ReceiveRequestAttemptId string}
```

### SQS Listener - Deletion Policy

By default, aws-api don't delete the messages when pass in for listener function, but inspired by [spring aws deletion policy](https://stackoverflow.com/questions/45710139/spring-cloud-aws-sqs-deletion-policy)  pedestal.sqs implement this feature, follow options:

* `:always` when pedestal.sqs receive message before call your listener function, the message is deleted
* `:on-success` when pedestal.sqs receive message after call your listener function, the message is deleted
* `:never` your message will never delete by pedestal.sqs

### SQS Listener - Built-in Interceptors

pedestal.sqs provides some interceptors that help process messages.

* `pedestal.sqs.interceptors/json-parser`
* `pedestal.sqs.interceptors/transit-json-value`
* `pedestal.sqs.interceptors/transit-msgpack-value`
* `pedestal.sqs.interceptors/string-parser`

A shortcut to built-in interceptors is `::sqs/response-type :json ;; or :transit-json, :transit-mgspack, default is string`

Is something missing?

Use `::sqs/response-interceptors`, just set a list of interceptors

```
{
 ::sqs/listeners #{["bar-queue" bar-listener {::sqs/response-interceptors [sqs.interceptors/transit-json-parser]}]
}
```

### SQS Global configuration

This library provide configurations to manage all queues, follow options available:

* `:auto-create-queue?` create queue if not found in your startup application, default is false
* `:auto-startup?` start listen SQS messages in startup application, default is true

## Full configuration example

Example of service map

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

Example to valid sqs configurations and start sqs listeners

```
(-> service/service
    sqs-listener/sqs-server ;; check sqs configurations
    sqs-listener/start ;; start sqs listeners
    server/create-server
    server/start)
```

### Publish in a queue from pedestal route

If you want publish in a queue from a pedestal route request argument have :sqs-client and :queues (configured in your project, if is a external queue use `(queue/get-queue-id sqs-client "queue-name")`) 

```
(defn home-page
  [request]
  (let [sqs-client (:sqs-client request)
        queues (:queues request)]
    (messaging/send-message!
      sqs-client ;; same client configured by you and used internally of pedestal.sqs
      (get queues "bar-queue") ;; here a shortcut to queues configured in your project
      (messaging/to-json {:example "example"}))
    (ring-resp/response "Hello from pedestal.sqs!")))
```

Read more in [https://github.com/RenanPalmeira/basic-pedestal-sqs-example](https://github.com/RenanPalmeira/basic-pedestal-sqs-example)

The `messaging/send-message!` has the arguments:

* client is a aws/client
* queue-urls string or list string of queue urls
* message string
* addons-payload a optional argument to add some attributes in AWS SQS payload

### Namespaces 

* `pedestal.sqs`
* `pedestal.sqs.listener`
* `pedestal.sqs.queue`
* `pedestal.sqs.messaging`
* `pedestal.sqs.interceptors`

## Links
* [pedestal.kafka](https://github.com/cognitect-labs/pedestal.kafka)
* [cognitect-labs/aws-api](https://github.com/cognitect-labs/aws-api)
* [AWS, meet Clojure - David Chelimsky](https://www.youtube.com/watch?v=ppDtDP0Rntw)
* [common-sqs](https://github.com/99Taxis/common-sqs)
