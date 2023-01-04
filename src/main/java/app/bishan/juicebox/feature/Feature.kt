package app.bishan.juicebox.feature

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.cmd.JuiceboxSubCommand
import app.bishan.juicebox.cmd.JuiceboxTabCompleter
import app.bishan.juicebox.feature.cringe.*
import app.bishan.juicebox.feature.blizzard.DynamicSnow
import app.bishan.juicebox.feature.blizzard.Hypothermia
import app.bishan.juicebox.feature.blizzard.RockSnowballs
import app.bishan.juicebox.feature.emotions.ChatColors
import app.bishan.juicebox.feature.emotions.ChatEmoticons
import app.bishan.juicebox.feature.emotions.HeadPat
import app.bishan.juicebox.feature.internal.*
import app.bishan.juicebox.feature.lock.*
import app.bishan.juicebox.feature.lore.*
import app.bishan.juicebox.feature.qol.*
import app.bishan.juicebox.feature.real.DeepSlateGenerator
import app.bishan.juicebox.feature.real.EndReactor
import app.bishan.juicebox.feature.real.food.Coffee
import app.bishan.juicebox.feature.real.food.FortuneShears
import app.bishan.juicebox.feature.real.tools.Bonker
import app.bishan.juicebox.feature.real.tools.instrument.Trumpet
import app.bishan.juicebox.feature.vehicle.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.yaml.snakeyaml.Yaml
import java.io.File

abstract class Feature(
	val name: String, val defaultActive: Boolean = false, val scope: Scope = Scope.PUBLIC
) : Listener {
	companion object {
		val allFeatures = arrayOf(
			CustomEnchants,
			CustomHorns,
			WanderingTrades,
			Hypothermia,
			RockSnowballs,
			MultishotEnderpearl,
			JuiceBot,
			Snowmobile,
			VolaRavager,
			ChatEmoticons,
			ChatColors,
			RunPastebin,
			BrewingStandRecipes,
			Anomaly,
			DeepSlateGenerator,
			GiveSubCommand,
			DynamicSnow,
			FortuneShears,
			InfGoatHorn,
			LargeFern,
			BucketOfOrca,
			Bonker,
			Trumpet,
			Coffee,
			ResourcePack,
			CBT,
			SuburuForester,
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
			CustomTags,
			CursedVision,
			HeadPat,
			Sleigh,
		).distinct().sortedBy { it.name }.sortedWith { i, j ->
			// sort by scope
			i.scope.compareTo(j.scope)
		}.associateBy {
			JuiceboxPlugin.instance.logger.info("Registered feature ${it.name}")
			it.name
		}

		fun saveFeaturesActive() {
			val file = File(JuiceboxPlugin.instance.dataFolder, "features.yml")
			file.createNewFile()

			val yaml = Yaml()
			yaml.dump(allFeatures.filter { it.value.active != it.value.defaultActive }.mapValues { it.value.isActive() },
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
				if (activate) feat.enable(false)
			}
		}
	}

	/**
	 * Scope of the feature.
	 */
	enum class Scope {
		/**
		 * Features for internal use only.
		 */
		INTERNAL,

		/**
		 * Features that can be manipulated by admins.
		 */
		PUBLIC
	}

	private var active: Boolean = false
	private val commands = mutableSetOf<String>()
	private val recipes = mutableSetOf<Recipe>()
	private val giveItems = mutableMapOf<String, ItemStack>()
	private val trades = mutableSetOf<WanderingRecipe>()
	private val enchantments = mutableSetOf<Enchantment>()
	val customItems: Map<String, ItemStack> get() = giveItems


	fun isActive(): Boolean = active

	/**
	 * Adds a new feature bound juicebox sub command, should only be called in onEnable
	 */
	protected fun addCommand(
		cmdName: String, cmd: JuiceboxSubCommand, tab: JuiceboxTabCompleter? = null, permission: String? = null
	) {
		commands.add(cmdName)
		JuiceboxPlugin.instance.jbCmdHandler.registerCommand(cmdName, cmd, tab, permission)
	}

	/**
	 * Adds a new feature bound give item command, should only be called in onEnable
	 */
	protected fun addCustomItem(itemName: String, item: ItemStack) {
		giveItems[itemName] = item
	}

	/**
	 * Adds a new feature bound recipe, should only be called in onEnable
	 */
	protected fun addRecipe(recipe: Recipe): Recipe {
		recipes.add(recipe)
		Bukkit.addRecipe(recipe)
		return recipe
	}

	/**
	 * Adds a new feature bound wandering trade, should only be called in onEnable
	 */
	protected fun addWanderingTrade(recipe: WanderingRecipe) {
		trades.add(recipe)
		WanderingTrades.registerTrade(recipe)
	}

	protected fun addEnchantment(enchantment: Enchantment) {
		enchantments.add(enchantment)
		CustomEnchants.registerEnchantment(enchantment)
	}

	/**
	 * Enables the feature and registers the event listeners,
	 * SHOULD NOT BE CALLED DIRECTLY
	 */
	fun enable(save: Boolean = true) {
		Bukkit.getPluginManager().registerEvents(this, JuiceboxPlugin.instance)
		active = true

		try {
			onEnable()
			if (save) saveFeaturesActive()
		} catch (e: Exception) {
			JuiceboxPlugin.instance.logger.severe("Error enabling feature $name")
			JuiceboxPlugin.instance.logger.severe(e.stackTraceToString())
			active = false
		}
	}

	/**
	 * Disables the feature and unregisters the event listeners,
	 * SHOULD NOT BE CALLED DIRECTLY
	 */
	fun disable(save: Boolean = true) {
		HandlerList.unregisterAll(this)
		active = false

		try {
			onDisable()
		} catch (e: Exception) {
			JuiceboxPlugin.instance.logger.severe("Error disabling feature $name")
			JuiceboxPlugin.instance.logger.severe(e.stackTraceToString())
		}

		commands.forEach(JuiceboxPlugin.instance.jbCmdHandler::unregisterCommand)

		// remove recipes
		val recipeIter = Bukkit.recipeIterator()
		while (recipeIter.hasNext()) if (recipes.contains(recipeIter.next())) recipeIter.remove()
		recipes.clear()

		// remove wandering trades
		trades.forEach(WanderingTrades::unregisterTrade)
		trades.clear()

		// remove give items
		giveItems.clear()

		// remove enchantments
		enchantments.forEach(CustomEnchants::unregisterEnchantment)

		if (save) saveFeaturesActive()
	}

	protected fun customData(suffix: String = "yml"): File {
		var node = JuiceboxPlugin.instance.dataFolder
		node = File(node, "features")
		if (!node.exists()) node.mkdirs()

		node = File(node, "$name.$suffix")
		return node
	}

	protected fun config(): ConfigurationSection =
		JuiceboxPlugin.instance.config.getConfigurationSection("features.$name")!!

	open val description: Component get() = Component.text("No description provided", NamedTextColor.RED)
	protected open fun onEnable() {}
	protected open fun onDisable() {}

}
