package app.bishan.juicebox.utils

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.*

fun PersistentDataContainer.missingFlag(key: NamespacedKey) = !hasFlag(key)
fun PersistentDataContainer.hasFlag(key: NamespacedKey): Boolean =
	getOrDefault(key, PersistentDataType.BYTE, 0) != 0.toByte()

fun PersistentDataContainer.raiseFlag(key: NamespacedKey) = setFlag(key, true)
fun PersistentDataContainer.lowerFlag(key: NamespacedKey) = setFlag(key, false)

fun PersistentDataContainer.setFlag(key: NamespacedKey, value: Boolean) =
	set(key, PersistentDataType.BYTE, if (value) 1 else 0)

fun PersistentDataContainer.getByte(key: NamespacedKey) = get(key, PersistentDataType.BYTE)
fun PersistentDataContainer.getInt(key: NamespacedKey) = get(key, PersistentDataType.INTEGER)
fun PersistentDataContainer.getLong(key: NamespacedKey) = get(key, PersistentDataType.LONG)
fun PersistentDataContainer.getFloat(key: NamespacedKey) = get(key, PersistentDataType.FLOAT)
fun PersistentDataContainer.getDouble(key: NamespacedKey) = get(key, PersistentDataType.DOUBLE)
fun PersistentDataContainer.getString(key: NamespacedKey) = get(key, PersistentDataType.STRING)
fun PersistentDataContainer.getVector(key: NamespacedKey) = get(key, PersistentDataType.LONG_ARRAY)?.let {
	Vector(
		Double.fromBits(it[0]), Double.fromBits(it[1]), Double.fromBits(it[2])
	)
}

fun PersistentDataContainer.getByteArray(key: NamespacedKey) = get(key, PersistentDataType.BYTE_ARRAY)
fun PersistentDataContainer.getIntArray(key: NamespacedKey) = get(key, PersistentDataType.INTEGER_ARRAY)
fun PersistentDataContainer.getLongArray(key: NamespacedKey) = get(key, PersistentDataType.LONG_ARRAY)
fun PersistentDataContainer.getCompound(key: NamespacedKey) = get(key, PersistentDataType.TAG_CONTAINER)

fun PersistentDataContainer.getUUID(key: NamespacedKey) = getString(key)?.let { UUID.fromString(it) }

fun PersistentDataContainer.setByte(key: NamespacedKey, value: Byte) = set(key, PersistentDataType.BYTE, value)
fun PersistentDataContainer.setInt(key: NamespacedKey, value: Int) = set(key, PersistentDataType.INTEGER, value)
fun PersistentDataContainer.setLong(key: NamespacedKey, value: Long) = set(key, PersistentDataType.LONG, value)
fun PersistentDataContainer.setFloat(key: NamespacedKey, value: Float) = set(key, PersistentDataType.FLOAT, value)
fun PersistentDataContainer.setDouble(key: NamespacedKey, value: Double) = set(key, PersistentDataType.DOUBLE, value)
fun PersistentDataContainer.setString(key: NamespacedKey, value: String) = set(key, PersistentDataType.STRING, value)

fun PersistentDataContainer.setUUID(key: NamespacedKey, value: java.util.UUID) =
	setString(key, value.toString())

fun PersistentDataContainer.setVector(key: NamespacedKey, value: Vector) = LongArray(3).apply {
	set(0, value.x.toBits())
	set(1, value.y.toBits())
	set(2, value.z.toBits())
}.let { set(key, PersistentDataType.LONG_ARRAY, it) }

fun PersistentDataContainer.setByteArray(key: NamespacedKey, value: ByteArray) =
	set(key, PersistentDataType.BYTE_ARRAY, value)

fun PersistentDataContainer.setIntArray(key: NamespacedKey, value: IntArray) =
	set(key, PersistentDataType.INTEGER_ARRAY, value)

fun PersistentDataContainer.setLongArray(key: NamespacedKey, value: LongArray) =
	set(key, PersistentDataType.LONG_ARRAY, value)

fun PersistentDataContainer.setCompound(key: NamespacedKey, value: PersistentDataContainer) =
	set(key, PersistentDataType.TAG_CONTAINER, value)
