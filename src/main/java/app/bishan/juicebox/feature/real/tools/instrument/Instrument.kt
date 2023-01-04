package app.bishan.juicebox.feature.real.tools.instrument

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.minecraft.util.Mth.clamp
import org.bukkit.entity.Player
import kotlin.math.pow

object Instrument {
	private const val MAX_NOTEBLOCK_NOTE = 24

	enum class NOTE(name: String) {
		A("A"),
		B_FLAT("B♭"),
		B("B"),
		C("C"),
		C_SHARP("C♯"),
		D("D"),
		E_FLAT("E♭"),
		E("E"),
		F("F"),
		F_SHARP("F♯"),
		G("G"),
		G_SHARP("G♯");
	}

	private val NOTE_TEXT = listOf(
		"F# / G♭",
		"G",
		"G# / A♭",
		"A",
		"A# / B♭",
		"B",
		"C",
		"C# / D♭",
		"D",
		"D# / E♭",
		"E",
		"F",
		"F# / G♭",
		"F# / G♭",
		"G",
		"G# / A♭",
		"A",
		"A# / B♭",
		"B",
		"C",
		"C# / D♭",
		"D",
		"D# / E♭",
		"E",
		"F",
		"F# / G♭"
	)


	private val NOTE_COLORS = listOf(
		TextColor.fromCSSHexString("#77D700"),
		TextColor.fromCSSHexString("#95C000"),
		TextColor.fromCSSHexString("#B2A500"),
		TextColor.fromCSSHexString("#CC8600"),
		TextColor.fromCSSHexString("#E26500"),
		TextColor.fromCSSHexString("#F34100"),
		TextColor.fromCSSHexString("#FC1E00"),
		TextColor.fromCSSHexString("#FE000F"),
		TextColor.fromCSSHexString("#F70033"),
		TextColor.fromCSSHexString("#E8005A"),
		TextColor.fromCSSHexString("#CF0083"),
		TextColor.fromCSSHexString("#AE00A9"),
		TextColor.fromCSSHexString("#8600CC"),
		TextColor.fromCSSHexString("#8600CC"),
		TextColor.fromCSSHexString("#5B00E7"),
		TextColor.fromCSSHexString("#2D00F9"),
		TextColor.fromCSSHexString("#020AFE"),
		TextColor.fromCSSHexString("#0037F6"),
		TextColor.fromCSSHexString("#0068E0"),
		TextColor.fromCSSHexString("#009ABC"),
		TextColor.fromCSSHexString("#00C68D"),
		TextColor.fromCSSHexString("#00E958"),
		TextColor.fromCSSHexString("#00FC21"),
		TextColor.fromCSSHexString("#1FFC00"),
		TextColor.fromCSSHexString("#59E800"),
		TextColor.fromCSSHexString("#94C100))")
	)

	fun Player.getEyeNote(): Float = (180 - (eyeLocation.pitch + 90)) * MAX_NOTEBLOCK_NOTE / 180

	fun noteAsPitch(note: Float): Float = 2.0.pow((note - 12.0) / 12.0).toFloat()

	fun getNoteName(note: Int): String = NOTE_TEXT[clamp(note, 0, NOTE_TEXT.size)]

	fun getNoteColor(note: Int): TextColor? = NOTE_COLORS[clamp(note, 0, NOTE_COLORS.size)]

	fun getNoteTextComponent(note: Int): Component = Component.text(getNoteName(note), getNoteColor(note))
}
