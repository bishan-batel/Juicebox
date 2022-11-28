package app.bishan.juicebox.utils

import kotlin.math.exp

fun sigmoid(x: Double): Double {
	return 1 / (1 + exp(x))
}

fun sigmoidSigned(x: Double): Double {
	return sigmoid(x) * 2 - 1
}
