package app.bishan.juicebox.utils

import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

fun sigmoid(x: Double): Double {
	return 1 / (1 + exp(x))
}

fun sigmoidSigned(x: Double): Double {
	return sigmoid(x) * 2 - 1
}

fun Boolean.toInt(): Int = if (this) 1 else 0
fun Boolean.toByte(): Byte = if (this) 1 else 0


fun directionOfDegrees(yaw: Double, pitch: Double) = directionOfRadians(yaw.radians, pitch.radians)

fun directionOfRadians(yaw: Double, pitch: Double): Vector {
	val pCos = cos(pitch)
	return Vector(
		cos(yaw) * pCos,
		sin(pitch),
		sin(yaw) * pCos
	)
}

val Float.radians: Float
	get() = Math.toRadians(this.toDouble()).toFloat()

val Float.degrees: Float
	get() = Math.toDegrees(this.toDouble()).toFloat()

val Double.radians: Double
	get() = Math.toRadians(this)

val Double.degrees: Double
	get() = Math.toDegrees(this)
