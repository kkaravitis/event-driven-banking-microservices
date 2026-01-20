# Event-Driven Banking Case Study

## Prerequisites
 - JDK 21 
 - Maven 3.8+ 
 - Docker Desktop / Docker Engine

## Quick start

### Build the project
``
mvn clean install
``

### Run the infrastructure
First of all we need to create the docker network

```shell script
docker network create banking-net
```
Then we can start all the infrastructure services

```shell script
docker compose up -d
```
The next step is to run our microservices:

```shell script
mvn -pl anti-fraud-service spring-boot:run

mvn -pl bank-account-service spring-boot:run

mvn -pl bank-transfer-service spring-boot:run
```
Before we try to start a money transfer we need to install the debezium connectors.
Execute the following commands:

```shell script
curl -i -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   --data-binary @./transfer-outbox.json
curl -i -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   --data-binary @./account-outbox.json
curl -i -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   --data-binary @./antifraud-outbox.json
```
and verify that the connectors are installed by executing:

```shell script
curl -s http://localhost:8083/connectors
```

You should see the following result:

```shell script
$ curl -s http://localhost:8083/connectors
["account-outbox","antifraud-outbox","transfer-outbox"]
```

In order to start a money transfer you can call the REST API as below:
```shell script
 
curl --location 'http://localhost:8080/banking/transfer' \
--header 'X-CUSTOMER-ID: CC-100' \
--header 'Content-Type: application/json' \
--data '{
    "fromAccountId": "ACC-101",
    "toAccountId": "ACC-201",
    "amount": 10000,
    "currency": "EUR"
}'

```
The response body includes the transfer identifier, you can use later to get the current transfer status by making a GET request to the API as below:

```shell script
transferId="THE TRANSFER ID VALUE"
curl --location http://localhost:8080/banking/transfer/${transferId}
```

You can try to cancel the transfer by executing:
```shell script
transferId="THE TRANSFER ID VALUE"

curl --location --request POST 'http://localhost:8080/banking/transfer/${transferId}/cancel' \
--header 'X-CUSTOMER-ID: CC-100'

```











