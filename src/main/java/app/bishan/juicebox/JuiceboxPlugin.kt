package app.bishan.juicebox

import app.bishan.juicebox.cmd.JuiceboxCommandHandler
import app.bishan.juicebox.feature.Feature
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class JuiceboxPlugin : JavaPlugin() {
	companion object {
		val instance get() = Bukkit.getServer().pluginManager.getPlugin("Juicebox") as JuiceboxPlugin
		fun key(name: String) = NamespacedKey(instance, name)
	}

	val jbCmdHandler = JuiceboxCommandHandler()

	override fun onEnable() {
		dataFolder.mkdirs()

		// activate all features that are activated by default
		Feature.loadFeaturesActive()
		getCommand("juicebox")!!.setExecutor(jbCmdHandler)
	}

	fun getFeature(name: String): Feature? = Feature.allFeatures[name]

	fun activateFeature(name: String, feature: Feature?, save: Boolean = true) {
		val feat = (Feature.allFeatures[name] ?: feature) ?: return
		if (!feat.isActive()) feat.enable(save)
	}

	fun deactivateFeature(name: String, feature: Feature?, save: Boolean = true) {
		val feat = (Feature.allFeatures[name] ?: feature) ?: return
		if (feat.isActive()) feat.disable(save)
	}

	override fun onDisable() {}
}
