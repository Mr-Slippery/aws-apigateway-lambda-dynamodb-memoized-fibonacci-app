# Kotlin sample app using AWS API Gateway, Lambda and DynamoDB

The application uses a Lambda function behind API Gateway to compute values of the Fibonacci sequence.

It also uses DynamoDB as a memoization cache to read previously computed values and to add newly computed values.

**Note:** The first time you deploy you have to add an inline policy to the Lambda role allowing it access to DynamoDB.

The structure of the deployed system should be:

![System structure](aws-lambda-view.png "AWS API Gateway and Lambda Structure")

Once the system is deployed, you can use the application's endpoint to retrieve cached or computed values:

* Value is not cached but computed:
![Computed value](value-computed.png "Value must be computed")
* Value is now cached (retrieved from DynamoDB):
![Cached value](value-cached.png "Value returned from cache")

We note the presence of the new entry for `n: 9, value: 34` in the corresponding view of AWS NoSQL Workbench:
![NoSQL Workbench view](nosql.png "NoSQL Workbench with n:9, value:34")
