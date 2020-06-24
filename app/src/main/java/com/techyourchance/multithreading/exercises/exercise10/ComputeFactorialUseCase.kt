package com.techyourchance.multithreading.exercises.exercise10

import android.util.Log
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ComputeFactorialUseCase  {

    private var numberOfThreads: AtomicInteger = AtomicInteger(0)
    private var threadsComputationRanges: Array<ComputationRange?> = arrayOf()
    @Volatile private var threadsComputationResults: Array<BigInteger?> = arrayOf()
    private var numOfFinishedThreads: AtomicInteger = AtomicInteger(0)

    private var isTimedOut: AtomicBoolean = AtomicBoolean(false)


    suspend fun computeFactorialAndNotify(argument: Int, timeout: Int) : UIState {
        return withContext(Dispatchers.IO) {

            if (argument < 20) {
                numberOfThreads.set(1)
            } else {
                numberOfThreads.set(Runtime.getRuntime().availableProcessors()
                )
            }

            numOfFinishedThreads.set(0)

            threadsComputationResults = arrayOfNulls(numberOfThreads.get())

            threadsComputationRanges = arrayOfNulls(numberOfThreads.get())

            initThreadsComputationRanges(argument)

            isTimedOut.set(false)

            val computation = async(Dispatchers.IO) {
                try {
                    withTimeout(timeMillis = timeout.toLong()) {
                        for (i in 0 until numberOfThreads.get()) {
                            startMultiplyingShit(i)
                        }
                    }
                } catch (e : TimeoutCancellationException) {
                    Log.d("Ex10UseCase", "timeout ")
                    isTimedOut.set(true)
                }
            }
            awaitAll(computation)
            processComputationResults()
        }
    }

    private fun initThreadsComputationRanges(factorialArgument: Int) {
        val computationRangeSize = factorialArgument / numberOfThreads.get()

        var nextComputationRangeEnd = factorialArgument.toLong()
        for (i in numberOfThreads.get() - 1 downTo 0) {
            threadsComputationRanges[i] = ComputationRange(
                    nextComputationRangeEnd - computationRangeSize + 1,
                    nextComputationRangeEnd
            )
            nextComputationRangeEnd = threadsComputationRanges[i]!!.start - 1
        }

        // add potentially "remaining" values to first thread's range
        threadsComputationRanges[0] = ComputationRange(1, threadsComputationRanges[0]!!.end)
    }

    private fun CoroutineScope.startMultiplyingShit(index: Int) = launch(Dispatchers.IO) {
        val rangeStart = threadsComputationRanges[index]!!.start
        val rangeEnd = threadsComputationRanges[index]!!.end
        var product = BigInteger("1")
        for (num in rangeStart..rangeEnd) {
            product = product.multiply(BigInteger(num.toString()))
        }
        threadsComputationResults[index] = product

        numOfFinishedThreads.incrementAndGet()
    }

    private fun processComputationResults() : UIState {

        if(isTimedOut.get()) {
            return TIMEOUT
        }

        val result = computeFinalResult()

        return SUCCESS(result)
    }

    private fun computeFinalResult(): BigInteger {
        var result = BigInteger("1")
        for (i in 0 until numberOfThreads.get()) {
            result = result.multiply(threadsComputationResults[i])
        }
        return result
    }

    private data class ComputationRange(val start: Long, val end: Long)
}
