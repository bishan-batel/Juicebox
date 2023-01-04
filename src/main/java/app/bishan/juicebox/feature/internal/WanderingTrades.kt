package app.bishan.juicebox.feature.internal

import app.bishan.juicebox.feature.Feature
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe

object WanderingTrades : Feature("internal:wandering_trades", true, Scope.INTERNAL) {
	private val trades = mutableSetOf<WanderingRecipe>()

	@EventHandler
	private fun onTraderSpawn(ev: EntitySpawnEvent) {
		val trader = ev.entity
		if (trader !is WanderingTrader) return

		val trade = trades
			.filter { it.isAllowed(trader.location.block) }
			.filter { it.chance > Math.random() }.randomOrNull() ?: return

		if (trader.recipeCount == 0) return
		val replace = (0 until trader.recipeCount).random()

		val recipes = trader.recipes.toMutableList()
		recipes[replace] = trade

		trader.recipes = recipes
//		})
	}

	fun registerTrade(recipe: WanderingRecipe) = trades.add(recipe)

	fun unregisterTrade(recipe: WanderingRecipe) = trades.remove(recipe)
}

class WanderingRecipe(result: ItemStack, maxUses: Int) : MerchantRecipe(result, maxUses) {
	val biomes get() = allowedBiomes
	val chance get() = _chance

	private var allowedBiomes: Set<Biome>? = null
	private var minTemp: Double? = null
	private var maxTemp: Double? = null
	private var minHumidity: Double? = null
	private var maxHumidity: Double? = null
	private var _chance = 1.0

	fun inBiomes(vararg biomes: Biome): WanderingRecipe {
		allowedBiomes = if (biomes.isEmpty())
			null
		else
			biomes.toSet()
		return this
	}

	fun minTemp(temp: Double?): WanderingRecipe {
		minTemp = temp
		return this
	}

	fun maxTemp(temp: Double?): WanderingRecipe {
		maxTemp = temp
		return this
	}

	fun minHumidity(humidity: Double?): WanderingRecipe {
		minHumidity = humidity
		return this
	}

	fun maxHumidity(humidity: Double?): WanderingRecipe {
		maxHumidity = humidity
		return this
	}

	fun chance(chance: Double): WanderingRecipe {
		_chance = chance
		return this
	}

	fun withIngredients(item: ItemStack, item2: ItemStack? = null): WanderingRecipe {
		addIngredient(item)
		item2?.let { addIngredient(it) }
		return this
	}

	fun isAllowed(block: Block) =
		allowedBiomes?.contains(block.biome) ?: true &&
				minTemp?.let { block.temperature >= it } ?: true &&
				maxTemp?.let { block.temperature <= it } ?: true &&
				minHumidity?.let { block.humidity >= it } ?: true &&
				maxHumidity?.let { block.humidity <= it } ?: true
}
