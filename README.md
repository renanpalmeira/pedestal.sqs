
# pedestal.sqs

A simple Pedestal interface for AWS SQS.

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

Time to be happy , here is just pass name of queue, a listener function to receive messages and configurations of queue (like response type, deletion policy)

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

Follow available queue configurations write by pedestal.sqs

```
{::sqs/deletion-policy :on-success ;; options :never, :on-success and :always, default is :never
 ::sqs/response-interceptors [sqs.interceptors/json-parser] ;; here we have access a put interceptors to manage received messages
 ::sqs/response-type :json ;; for now only json and string is response-type supported, default is string}
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

### SQS Global configuration

The library provide configurations to manage all queues, follow options available:

* `:auto-create-queue?` create queue if not found in your startup application, default is false

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

Read more in [https://github.com/RenanPalmeira/pedestal.sqs/blob/master/src/pedestal/sample/service.clj#L74-L94](https://github.com/RenanPalmeira/pedestal.sqs/blob/master/src/pedestal/sample/service.clj#L74-L94)

## Links
* [pedestal.kafka](https://github.com/cognitect-labs/pedestal.kafka)
* [cognitect-labs/aws-api](https://github.com/cognitect-labs/aws-api)