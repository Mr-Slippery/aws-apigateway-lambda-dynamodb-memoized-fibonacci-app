package fib

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.*

class AmazonDynamoDBFacade(region: String) {

    private val dynamoDB: AmazonDynamoDB

    init {
        dynamoDB = createDynamoDbClient(region)
    }

    fun doPutItem(tableName: String, n: String, value: String) {
        try {
            // Create PutItemRequest
            val putItemRequest = createPutItemRequest(tableName, n, value)
            dynamoDB.putItem(putItemRequest)
            println("Successfully put item.")
            // Handle putItemResult
        } catch (e: java.lang.Exception) {
            handlePutItemErrors(e)
        }
    }

    // Handles errors during PutItem execution. Use recommendations in error messages below to add error handling specific to
    // your application use-case.
    private fun handlePutItemErrors(exception: java.lang.Exception) {
        try {
            throw exception
        } catch (ccfe: ConditionalCheckFailedException) {
            println(
                "Condition check specified in the operation failed, review and update the condition " +
                        "check before retrying. Error: " + ccfe.errorMessage
            )
        } catch (tce: TransactionConflictException) {
            println(
                ("Operation was rejected because there is an ongoing transaction for the item, generally " +
                        "safe to retry with exponential back-off. Error: " + tce.errorMessage)
            )
        } catch (icslee: ItemCollectionSizeLimitExceededException) {
            println(
                ("An item collection is too large, you\'re using Local Secondary Index and exceeded " +
                        "size limit of items per partition key. Consider using Global Secondary Index instead. Error: " + icslee.errorMessage)
            )
        } catch (e: java.lang.Exception) {
            handleCommonErrors(e)
        }
    }

    fun doGetItem(tableName: String, n: String): String? {
        // Create the DynamoDB Client with the region you want
        try {
            // Create GetItemRequest
            val getItemRequest = createGetItemRequest(tableName, n)
            val getItemResult = dynamoDB.getItem(getItemRequest)
            println("GetItem request processed.")
            getItemResult?.let { getIt ->
                getIt.item?.let {
                    if (it.containsKey("value")) {
                        val value = it["value"]!!.s
                        println("Cached value returned: $value")
                        return value
                    }
                }
            }
            // Handle getItemResult
        } catch (e: Exception) {
            handleGetItemErrors(e)
        }
        println("No cached value found, will compute.")
        return null
    }

    private fun createDynamoDbClient(region: String): AmazonDynamoDB {
        return AmazonDynamoDBClientBuilder.standard().withRegion(region).build()
    }

    private fun createGetItemRequest(tableName: String, key: String): GetItemRequest {
        val getItemRequest = GetItemRequest()
        getItemRequest.tableName = tableName
        getItemRequest.key = getKey(key)
        return getItemRequest
    }

    private fun getKey(n: String): Map<String, AttributeValue> {
        val key: MutableMap<String, AttributeValue> = HashMap()
        key["n"] = AttributeValue().withN(n)
        return key
    }

    private fun createPutItemRequest(tableName: String, n: String, value: String): PutItemRequest {
        val putItemRequest = PutItemRequest()
        putItemRequest.tableName = tableName
        putItemRequest.item = getItem(n, value)
        return putItemRequest
    }

    private fun getItem(n: String, value: String): Map<String, AttributeValue> {
        val item: MutableMap<String, AttributeValue> = HashMap()
        item["n"] = AttributeValue().withN(n)
        item["value"] = AttributeValue(value)
        return item
    }

    // Handles errors during GetItem execution. Use recommendations in error messages below to add error handling specific to
    // your application use-case.
    private fun handleGetItemErrors(exception: Exception) {
        try {
            throw exception
        } catch (e: Exception) {
            // There are no API specific errors to handle for GetItem, common DynamoDB API errors are handled below
            handleCommonErrors(e)
        }
    }

    private fun handleCommonErrors(exception: Exception) {
        try {
            throw exception
        } catch (isee: InternalServerErrorException) {
            println("Internal Server Error, generally safe to retry with exponential back-off. Error: " + isee.errorMessage)
        } catch (rlee: RequestLimitExceededException) {
            println(
                "Throughput exceeds the current throughput limit for your account, increase account level throughput before " +
                        "retrying. Error: " + rlee.errorMessage
            )
        } catch (ptee: ProvisionedThroughputExceededException) {
            println(
                "Request rate is too high. If you're using a custom retry strategy make sure to retry with exponential back-off. " +
                        "Otherwise consider reducing frequency of requests or increasing provisioned capacity for your table or secondary index. Error: " +
                        ptee.errorMessage
            )
        } catch (rnfe: ResourceNotFoundException) {
            println("One of the tables was not found, verify table exists before retrying. Error: " + rnfe.errorMessage)
        } catch (ase: AmazonServiceException) {
            println(
                ("An AmazonServiceException occurred, indicates that the request was correctly transmitted to the DynamoDB " +
                        "service, but for some reason, the service was not able to process it, and returned an error response instead. Investigate and " +
                        "configure retry strategy. Error type: " + ase.errorType + ". Error message: " + ase.errorMessage)
            )
        } catch (ace: AmazonClientException) {
            println(
                ("An AmazonClientException occurred, indicates that the client was unable to get a response from DynamoDB " +
                        "service, or the client was unable to parse the response from the service. Investigate and configure retry strategy. " +
                        "Error: " + ace.message)
            )
        } catch (e: Exception) {
            println("An exception occurred, investigate and configure retry strategy. Error: " + e.message)
        }
    }

}