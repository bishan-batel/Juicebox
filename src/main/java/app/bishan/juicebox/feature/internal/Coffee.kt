package app.bishan.juicebox.feature.internal

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.isAir
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.BrewingStand
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.function.Predicate

object BrewingStandRecipes : Feature("internal:brewing_stand_recipes", false, Scope.INTERNAL) {
	enum class Slot {
		MIDDLE_ONE, MIDDLE_TWO, MIDDLE_THREE, INGREDIENT, FUEL;
	}

	private val recipes = mutableSetOf<PotionRecipe>()

	init {
		recipes.add(PotionRecipe(Material.COCOA_BEANS) {
			it.apply { itemMeta = itemMeta.apply { displayName(Component.text("cringe ahh")) } }
		})
	}

	@EventHandler
	private fun shiftClickToBrewingStand(ev: InventoryClickEvent) {
		if (!ev.isShiftClick) return

		val brewing = ev.whoClicked.openInventory
		if (brewing.type != InventoryType.BREWING) return


		val item = ev.currentItem ?: return

		for (slot in Slot.values()) {
			if (brewing.getItem(slot.ordinal)?.type != Material.AIR) continue

			if (!canGoIntoSlot(slot, item)) continue

			brewing.setItem(Slot.INGREDIENT.ordinal, item)
			ev.currentItem = ItemStack(Material.AIR)
			updateBrewingStand(brewing.topInventory)


			ev.isCancelled = true
			return
		}


	}

	// Directly Clicking into a brewing stand
	@EventHandler
	private fun onClick(ev: InventoryClickEvent) {
		val brewing = ev.clickedInventory ?: return
		if (brewing.type != InventoryType.BREWING) return

		val itemInSlot = ev.currentItem

		if (itemInSlot != null && !itemInSlot.isAir) return

		val cursorItem = ev.cursor ?: return

		if (!canGoIntoSlot(Slot.values()[ev.slot], cursorItem)) return
		brewing.setItem(ev.slot, cursorItem.clone())

		updateBrewingStand(brewing)

		ev.whoClicked.setItemOnCursor(null)
		ev.isCancelled = true
	}

	private fun canGoIntoSlot(slot: Slot, item: ItemStack) = when (slot) {
		Slot.INGREDIENT -> recipes.any { it.ingredient.test(item) }
		else -> false
	}

	private fun updateBrewingStand(inv: Inventory) {
		val brewing = inv.holder as? BrewingStand ?: return

		val effector = inv.getItem(Slot.INGREDIENT.ordinal) ?: return

		val recipe = recipes.firstOrNull { it.ingredient.test(effector) } ?: return

		val potions = listOf(Slot.MIDDLE_ONE, Slot.MIDDLE_TWO, Slot.MIDDLE_THREE).map { inv.getItem(it.ordinal) }

		val canBrew = potions.any { it?.let { recipe.transformer(it.clone()) } != null }

		if (!canBrew) {
			brewing.brewingTime = 0
			return
		}

		if (brewing.brewingTime > 0) return

		brewing.brewingTime = 400
		brewing.update()
	}
}

typealias PotionTransformer = (ItemStack) -> ItemStack?

class PotionRecipe(val ingredient: Predicate<ItemStack>, val transformer: PotionTransformer) {
	constructor(ingredient: Material, transformer: PotionTransformer) : this(
		Predicate { it.type == ingredient },
		transformer
	)

	constructor(item: ItemStack, transformer: PotionTransformer) : this(Predicate { it.isSimilar(item) }, transformer)
}
