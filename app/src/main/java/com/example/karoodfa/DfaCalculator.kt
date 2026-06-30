package com.example.karoodfa

class DfaCalculator {
    // Zero-allocation RR buffer and DFA a1 state
    private val rrBuffer = DoubleArray(MAX_RR_SAMPLES)
    private var rrIndex = 0
    private var rrCount = 0

    // Pre-allocated arrays for zero-allocation computation
    private val tempRr = DoubleArray(MAX_RR_SAMPLES)
    private val integrated = DoubleArray(MAX_RR_SAMPLES)
    
    private val boxSizes = intArrayOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
    private val lnN = DoubleArray(boxSizes.size)
    private val lnFn = DoubleArray(boxSizes.size)

    init {
        for (i in boxSizes.indices) {
            lnN[i] = Math.log(boxSizes[i].toDouble())
        }
    }

    companion object {
        const val MAX_RR_SAMPLES = 512
    }

    fun addRrInterval(rr: Double) {
        rrBuffer[rrIndex] = rr
        rrIndex = (rrIndex + 1) % MAX_RR_SAMPLES
        if (rrCount < MAX_RR_SAMPLES) {
            rrCount++
        }
    }

    fun compute(): Double {
        val N = Math.min(rrCount, 256)
        if (N < 128) return 0.0
        
        // 1. Extract the last N intervals chronologically
        val startIdx = (rrIndex - N + MAX_RR_SAMPLES) % MAX_RR_SAMPLES
        for (i in 0 until N) {
            tempRr[i] = rrBuffer[(startIdx + i) % MAX_RR_SAMPLES]
        }
        
        // 2. Calculate mean RR
        var sum = 0.0
        for (i in 0 until N) {
            sum += tempRr[i]
        }
        val mean = sum / N
        
        // 3. Integrate the series
        var accum = 0.0
        for (i in 0 until N) {
            accum += (tempRr[i] - mean)
            integrated[i] = accum
        }
        
        // 4. Calculate F(n) for each box size n
        for (idx in boxSizes.indices) {
            val n = boxSizes[idx]
            val K = N / n
            var sumSqDiff = 0.0
            
            for (j in 0 until K) {
                val start = j * n
                val end = start + n - 1
                
                // Fit a linear trend inside this box: y = a * i + b
                var sumI = 0.0
                var sumY = 0.0
                for (i in start..end) {
                    sumI += i
                    sumY += integrated[i]
                }
                val meanI = sumI / n
                val meanY = sumY / n
                
                var num = 0.0
                var den = 0.0
                for (i in start..end) {
                    val diffI = i - meanI
                    num += diffI * (integrated[i] - meanY)
                    den += diffI * diffI
                }
                val a = if (den != 0.0) num / den else 0.0
                val b = meanY - a * meanI
                
                // Calculate squared diffs
                for (i in start..end) {
                    val yFit = a * i + b
                    val d = integrated[i] - yFit
                    sumSqDiff += d * d
                }
            }
            
            val fn = Math.sqrt(sumSqDiff / (K * n))
            // Prevent log(0.0)
            lnFn[idx] = Math.log(Math.max(fn, 1e-6))
        }
        
        // 5. Fit linear regression of lnFn on lnN to find the slope (alpha1)
        val M = boxSizes.size
        var sumX = 0.0
        var sumY = 0.0
        for (i in 0 until M) {
            sumX += lnN[i]
            sumY += lnFn[i]
        }
        val meanX = sumX / M
        val meanY = sumY / M
        
        var numSlope = 0.0
        var denSlope = 0.0
        for (i in 0 until M) {
            val diffX = lnN[i] - meanX
            numSlope += diffX * (lnFn[i] - meanY)
            denSlope += diffX * diffX
        }
        
        return if (denSlope != 0.0) numSlope / denSlope else 0.0
    }
}
