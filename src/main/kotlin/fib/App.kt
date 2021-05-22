package fib

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.lang.NumberFormatException
import java.math.BigInteger

tailrec fun fibAux(a: BigInteger, b: BigInteger, n: Int): BigInteger {
    return if (n == 0) {
        a
    } else {
        fibAux(b, a + b, n - 1)
    }
}

val dynamoDBFacade = AmazonDynamoDBFacade("eu-central-1")

fun fib(n: String?): FibResult {
    n?.let { itsN ->
        try {
            val tableName = "fib"
            val parsedN = Integer.parseInt(itsN)
            if (parsedN < 0) {
                return FibResult.Error("Negative parameter $n supplied!")
            }
            val nStr = n.toString()
            // Try to get the value for n from cache
            val fromDB = dynamoDBFacade.doGetItem(tableName, nStr)
            fromDB?.let {
                return FibResult.Success(Result(parsedN, fromDB, "true"))
            }
            val result = fibAux(BigInteger.ZERO, BigInteger.ONE, parsedN)
            val resStr = result.toString()
            // Memoize the computed item
            dynamoDBFacade.doPutItem(tableName, nStr, resStr)
            return FibResult.Success(Result(parsedN, resStr, "false"))
        } catch (e: NumberFormatException) {
            return FibResult.Error("Could not parse $n into Int!", e.javaClass.name)
        } catch (e: Exception) {
            return FibResult.Error("Unknown error!", e.javaClass.name)
        }
    }
    return FibResult.Error("Parameter n was not passed!")
}

@Suppress("UNUSED")
class FibOutput(
    val isBase64Encoded: String,
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String
)

const val QUERY_STRING_PARAMETERS = "queryStringParameters"
const val KEY_N = "n"
const val HTTP_STATUS_OK = 200
const val HTTP_STATUS_BAD_REQUEST = 400

@Suppress("UNUSED")
class App : RequestHandler<Map<String, Any>, FibOutput> {
    override fun handleRequest(input: Map<String, Any>?, context: Context?): FibOutput {
        val headers = emptyMap<String, String>()
        input?.let { itsInput ->
            if (itsInput.containsKey(QUERY_STRING_PARAMETERS)) {
                val queryParamsValue = itsInput[QUERY_STRING_PARAMETERS]
                if (queryParamsValue is Map<*, *>) {
                    if (queryParamsValue.containsKey(KEY_N)) {
                        val nValue = queryParamsValue[KEY_N]
                        if (nValue is String) {
                            return when (val result: FibResult = fib(nValue)) {
                                is FibResult.Success ->
                                    FibOutput(
                                        "false", HTTP_STATUS_OK, headers,
                                        "{\"n\": \"${result.result.n}\", " +
                                                "\"value\": \"${result.result.value}\", " +
                                                "\"cached\": \"${result.result.cached}\"}"
                                    )
                                is FibResult.Error ->
                                    FibOutput(
                                        "false", HTTP_STATUS_BAD_REQUEST, headers,
                                        "{\"message\": \"${result.message}\", " +
                                                "\"cause\": \"${result.cause}\"}"
                                    )
                            }
                        }
                    }
                }
            }
        }
        return FibOutput(
            "false", HTTP_STATUS_BAD_REQUEST, headers,
            "{\"message\": \"Parameter n not supplied!\", \"cause\": \"Bad request\"}"
        )
    }
}