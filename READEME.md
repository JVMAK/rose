# ROSE - An DDD driven and CQRS application development framework

## Main concept:

**Commands** are used to drive changes to **Domain object**, these changes are persisted as **Domain Events**, and are published to **Event Bus**.

TODO: 
1. query-side support.

## Project artifacts:

* rose-core: framework infrastructure
* rose-example-account: microservice - Bank Account service
* rose-example-money-transfer: microservice - Money Transfer service

## Run the example:

1. start mongodb

2. start zookeeper/kafka

3. start rose-example-account application: com.tazhi.rose.example.account.AccountApplication

4. start rose-example-money-transfer application: com.tazhi.rose.example.transfer.MoneyTransferApplication

4. create 2 accounts with some balance and then make transfer:

create 2 accounts:
http://localhost:5001/account/create?accountId=a1&balance=300
http://localhost:5001/account/create?accountId=a2&balance=200

make transfer:
http://localhost:5000/transfer/create?fromAccountId=a1&toAccountId=a2&amount=50 this will return a generated transactionId

get transfer status:
http://localhost:5000/transfer/get?transactionId=<transactionId>

get accounts' balance:
http://localhost:5001/account/get?accountId=a1
http://localhost:5001/account/get?accountId=a2

5. play around, have fun, and feed back, thank you!