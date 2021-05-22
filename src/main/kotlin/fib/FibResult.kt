package fib

data class Result(val n: Int, val value: String, val cached: String)

sealed class FibResult {
    data class Success(val result: Result) : FibResult()
    data class Error(val message: String, val cause: String? = null) : FibResult()
}