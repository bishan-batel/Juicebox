package app.bishan.juicebox.feature.blizzard

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import io.papermc.paper.enchantments.EnchantmentRarity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.enchantments.EnchantmentWrapper
import org.bukkit.entity.EntityCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack


@Deprecated("This feature is deprecated and will be removed in the future.")
object FrostBreather : Feature("frost_breather", false) {
	private val FROST_BREATHER_ENCHANT = object : Enchantment(JuiceboxPlugin.key("frost_breather")) {
		override fun translationKey() = "juicebox.enchantment.frost_breather"

		override fun getName() = "Frost Breather"

		override fun getMaxLevel() = 1

		override fun getStartLevel() = 1

		override fun getItemTarget() = EnchantmentTarget.ARMOR_HEAD

		override fun isTreasure() = false

		override fun isCursed() = true

		override fun conflictsWith(other: Enchantment) = other == WATER_WORKER

		override fun canEnchantItem(item: ItemStack) = when (item.type) {
			Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.TURTLE_HELMET, Material.NETHERITE_HELMET -> true

			else -> false
		}


		override fun displayName(level: Int) = Component.translatable(translationKey())

		override fun isTradeable() = false

		override fun isDiscoverable() = false

		override fun getRarity() = EnchantmentRarity.VERY_RARE

		override fun getDamageIncrease(level: Int, entityCategory: EntityCategory) = 0.0f

		override fun getActiveSlots() = mutableSetOf(EquipmentSlot.HEAD)
	}

	override fun onEnable() {
		addEnchantment(FROST_BREATHER_ENCHANT)

		addCustomItem("frost_breather_helmet", ItemStack(Material.LEATHER_HELMET).apply {
			addEnchantment(FROST_BREATHER_ENCHANT, 1)

			itemMeta = itemMeta.apply {
				lore(
					listOf(
						Component.translatable("juicebox.enchantment.frost_breather").decoration(TextDecoration.ITALIC, false)
							.color(NamedTextColor.GRAY),
					)
				)
			}
		})
	}

	@EventHandler
	fun onItem(ev: PlayerMoveEvent) {
		val player = ev.player
		val helmet = player.inventory.helmet ?: return

		if (helmet.enchantments.none { it.key.key.compareTo(FROST_BREATHER_ENCHANT.key) == 0 && it.value > 0 }) return

		// draw a sphere of ice blocks around the player
		for (x in -2..2) {
			for (y in -2..2) {
				for (z in -2..2) {
					val block = player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
					if (block.location.distanceSquared(player.location) <= 2.0 * 2.0) {
						if (block.type == Material.AIR) {
							block.type = Material.FROSTED_ICE
						}
					}

				}
			}
		}

	}


	override fun onDisable() {
	}
}
