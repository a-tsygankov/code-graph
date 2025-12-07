package com.belmonttech.analysis;

import java.util.List;

/**
 * Centralized, lowercase pattern lists for cost signals.
 * All checks are done on lowercased text.
 */
public final class CodePatterns {

    private CodePatterns() {
    }

    // Mongo / general NoSQL / repo-style persistence
    public static final List<String> MONGO_PATTERNS = List.of(
            "mongotemplate",
            "mongodb",
            "mongoclient",
            "mongorepository",
            "mongooperations",
            "document",
            "collection",
            "cursor",
            "batchsize",
            "findone",
            "findAll".toLowerCase(),
            "findbyid",
            "save",
            "insert",
            "update",
            "delete",
            "aggregate",
            "aggregation",
            "dbobject"
    );

    // REST / HTTP / web clients
    public static final List<String> REST_PATTERNS = List.of(
            "resttemplate",
            "webclient",
            "httpclient",
            "closeablehttpclient",
            "urlconnection",
            "httpurlconnection",
            "okhttpclient",
            "retrofit",
            "feign",
            "webtarget",
            "invocation.builder",
            "requestbody",
            "responseentity",
            "http.get",
            "http.post",
            "http.put",
            "http.delete",
            "@getmapping",
            "@postmapping",
            "@putmapping",
            "@deletemapping",
            "@requestmapping"
    );

    // JDBC / SQL / JPA / EntityManager
    public static final List<String> JDBC_PATTERNS = List.of(
            "datasource",
            "connection",
            "preparedstatement",
            "statement.execute",
            "statement.executequery",
            "resultset.next",
            "jdbc",
            "entitymanager",
            "createnativequery",
            "createquery",
            "transaction.begin",
            "transaction.commit",
            "query.setparameter",
            "rowmapper"
    );

    // Generic RPC / messaging / network
    public static final List<String> RPC_PATTERNS = List.of(
            "grpc",
            "channel",
            "managedchannel",
            "socket",
            "serversocket",
            "producer.send",
            "consumer.poll",
            "kafkatemplate",
            "rabbittemplate",
            "jmsTemplate".toLowerCase(),
            "sendrequest",
            "sendmessage",
            "publish",
            "subscribe"
    );
}
