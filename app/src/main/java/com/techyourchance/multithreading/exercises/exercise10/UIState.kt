package com.techyourchance.multithreading.exercises.exercise10

import java.math.BigInteger

sealed class UIState

object TIMEOUT : UIState()

data class SUCCESS (
        var result : BigInteger
) : UIState()


