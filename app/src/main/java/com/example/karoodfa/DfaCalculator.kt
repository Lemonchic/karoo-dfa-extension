package com.example.karoodfa

class DfaCalculator {
    // Zero-allocation RR buffer and DFA a1 state
    private val rrBuffer = DoubleArray(MAX_RR_SAMPLES)
    private var rrIndex = 0
    private var rrCount = 0

    companion object {
        const val MAX_RR_SAMPLES = 512
        
        fun calculateDfaA1(rrIntervals: DoubleArray, length: Int): Double {
            if (length < 64) return 0.0
            
            // This is a placeholder for the actual zero-allocation O(N log N) DFA a1 math.
            // A real implementation would compute detrended fluctuation analysis here 
            // without allocating any objects, returning the alpha1 slope.
            // For now, we simulate a value based on the mean RR.
            
            var sum = 0.0
            for (i in 0 until length) {
                sum += rrIntervals[i]
            }
            val mean = sum / length
            
            // Simulated alpha1 between 0.2 and 1.5 based on HR (roughly)
            // Lower RR (higher HR) = lower alpha1
            val alpha1 = (mean / 1000.0) * 1.5 
            return alpha1.coerceIn(0.2, 1.5)
        }
    }

    fun addRrInterval(rr: Double) {
        rrBuffer[rrIndex] = rr
        rrIndex = (rrIndex + 1) % MAX_RR_SAMPLES
        if (rrCount < MAX_RR_SAMPLES) {
            rrCount++
        }
    }

    fun compute(): Double {
        // We pass the active portion of the buffer to the static math function
        // In a true ring-buffer DFA, we'd process it carefully without copying.
        // For this demo, we'll just pass the array and length.
        return calculateDfaA1(rrBuffer, rrCount)
    }
}
