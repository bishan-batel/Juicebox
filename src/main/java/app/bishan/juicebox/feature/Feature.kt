package app.bishan.juicebox.feature

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.cmd.JuiceboxSubCommand
import app.bishan.juicebox.cmd.JuiceboxTabCompleter
import app.bishan.juicebox.feature.cringe.*
import app.bishan.juicebox.feature.deco.fish.FishBowl
import app.bishan.juicebox.feature.emotions.HeadPat
import app.bishan.juicebox.feature.lock.*
import app.bishan.juicebox.feature.lore.*
import app.bishan.juicebox.feature.qol.*
import app.bishan.juicebox.feature.real.EndReactor
import app.bishan.juicebox.feature.real.bank.BellBank
import app.bishan.juicebox.feature.real.food.Coffee
import app.bishan.juicebox.feature.real.friendly_fox.FriendlyFox
import app.bishan.juicebox.feature.vehicle.*
import app.bishan.juicebox.utils.giveItem
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.yaml.snakeyaml.Yaml
import java.io.File

abstract class Feature(name: String, defaultActive: Boolean = false) : Listener {
	companion object {
		val allFeatures = arrayOf(
			BucketOfOrca,
			Coffee,
			ResourcePack,
			CBT,
			BugNet,
			TheFog,
			BackpackShulkers,
			MooMilk,
			WaterMunch,
			NPC,
			Vehicles,
			TotemGift,
			FishingVox,
			VolaImmunity,
			SammieShear,
			ModernRabbitCatching,
			JadInsomnia,
			FunnyStick,
			LegacyBiplane,
			Snad,
			BeeHoneyFactory,
			VolaCrystal,
			ChestLocking,
			LegacyVolaHelm,
			JesusGoose,
			CheapRenaming,
			LittleGuy,
			EndReactor,
			FishBowl,
			CustomTags,
			CursedVision,
			FriendlyFox,
			HeadPat,
			BellBank,
			Sleigh,
		).distinct().associateBy { it.name }

		fun saveFeaturesActive() {
			val file = File(JuiceboxPlugin.instance.dataFolder, "features.yml")
			file.createNewFile()

			val yaml = Yaml()
			yaml.dump(
				allFeatures
					.filter { it.value.active != it.value.defaultActive }
					.mapValues { it.value.isActive() },
				file.writer()
			)
		}

		fun loadFeaturesActive() {
			val file = File(JuiceboxPlugin.instance.dataFolder, "features.yml")

			val yaml = Yaml()
			val features = if (file.exists()) yaml.load<Map<String, Boolean>>(file.reader()) ?: mapOf() else mapOf()

			for ((name, feat) in allFeatures) {
				val activate = features[name] ?: feat.defaultActive
				Bukkit.broadcast(Component.text("Loading feature $name: $activate"))
				if (activate)
					feat.enable(false)
			}
		}
	}

	/**
	 * Name of the feature
	 */
	val name: String

	/**
	 * Whether the feature is enabled on startup
	 */
	val defaultActive: Boolean

	private var active: Boolean = false
	private val commands = mutableListOf<String>()
	private val recipes = mutableListOf<Recipe>()


	fun isActive(): Boolean = active

	init {
		this.name = name
		this.defaultActive = defaultActive
	}

	/**
	 * Adds a new feature bound juicebox sub command, should only be called in onEnable
	 */
	protected fun addCommand(
		cmdName: String,
		cmd: JuiceboxSubCommand,
		tab: JuiceboxTabCompleter? = null,
		permission: String? = null
	) {
		commands.add(cmdName)
		JuiceboxPlugin.instance.jbCmdHandler.registerCommand(cmdName, cmd, tab, permission)
	}

	/**
	 * Adds a new feature bound give item command, should only be called in onEnable
	 */
	protected fun addGiveItemCommand(itemName: String, item: ItemStack) = addCommand("give_$itemName",
		{ sender, _ ->
			if (sender !is Player) {
				sender.sendMessage("You must be a player to use this command")
				false
			} else {
				sender.giveItem(item)
				true
			}
		})

	/**
	 * Adds a new feature bound recipe, should only be called in onEnable
	 */
	protected fun addRecipe(recipe: Recipe): Recipe {
		recipes.add(recipe)
		Bukkit.addRecipe(recipe)
		return recipe
	}

	/**
	 * Enables the feature and registers the event listeners,
	 * SHOULD NOT BE CALLED DIRECTLY
	 */
	fun enable(save: Boolean = true) {
		Bukkit.getPluginManager().registerEvents(this, JuiceboxPlugin.instance)
		active = true
		onEnable()
		if (save) saveFeaturesActive()
	}

	/**
	 * Disables the feature and unregisters the event listeners,
	 * SHOULD NOT BE CALLED DIRECTLY
	 */
	fun disable(save: Boolean = true) {
		HandlerList.unregisterAll(this)
		active = false
		onDisable()
		commands.forEach(JuiceboxPlugin.instance.jbCmdHandler::unregisterCommand)

		// remove recipes
		val recipeIter = Bukkit.recipeIterator()
		while (recipeIter.hasNext())
			if (recipes.contains(recipeIter.next()))
				recipeIter.remove()

		if (save) saveFeaturesActive()
	}

	fun customData(): File {
		var node = JuiceboxPlugin.instance.dataFolder
		node = File(node, "features")
		if (!node.exists()) node.mkdirs()

		node = File(node, name)
		return node
	}


	protected open fun onEnable() {}
	protected open fun onDisable() {}
}
