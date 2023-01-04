package app.bishan.juicebox.feature.real.tools

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.feature.internal.ResourcePack
import app.bishan.juicebox.feature.internal.WanderingRecipe
import app.bishan.juicebox.utils.addEnchantmentGlint
import app.bishan.juicebox.utils.hasFlag
import app.bishan.juicebox.utils.missingFlag
import app.bishan.juicebox.utils.raiseFlag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

object Bonker : Feature("bonker", true) {
	private val IS_BONKER = JuiceboxPlugin.key("is_bonker_item")

	private val BONKER_ITEM = ItemStack(Material.GOLDEN_AXE).apply {
		addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		itemMeta = itemMeta.apply {
			displayName(
				Component.text("Bonker", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
			)
			lore(
				listOf(
					Component.text("Right click to bonk!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("Bonking will permanently disable an mob's AI", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, true)
				) + ResourcePack.NO_TEXTURE_MESSAGE
			)
			persistentDataContainer.raiseFlag(IS_BONKER)
		}
		addEnchantmentGlint()
	}

	private val BONKER_TRADE = WanderingRecipe(BONKER_ITEM, 3)
		.withIngredients(ItemStack(Material.EMERALD, 32))
		.chance(0.01 * 9.2)

	private val INVALID_BONKEES = setOf(
		EntityType.ARMOR_STAND,
		EntityType.PLAYER,
		EntityType.WITHER,
		EntityType.ENDER_DRAGON,
	)

	override fun onEnable() {
		addCustomItem("bonker", BONKER_ITEM)
		addWanderingTrade(BONKER_TRADE)
	}

	@EventHandler
	private fun onBonk(ev: EntityDamageByEntityEvent) {
		val damager = ev.damager

		if (damager !is Player) return

		val item = damager.inventory.itemInMainHand
		if (item.type == Material.AIR) return
		if (item.itemMeta.persistentDataContainer.missingFlag(IS_BONKER)) return
		ev.isCancelled = true

		val entity = ev.entity as? LivingEntity ?: return
		if (!entity.hasAI()) return

		if (entity.type in INVALID_BONKEES) return

		entity.setAI(false)
		entity.world.playSound(entity, Sound.BLOCK_ANVIL_DESTROY, 1f, 1f)
		entity.world.spawnParticle(Particle.CLOUD, entity.location, 50, 0.2, 0.2, 0.2, 0.5)

		if (damager.gameMode == GameMode.CREATIVE) return

		item.itemMeta = item.itemMeta.apply {
			if (this is org.bukkit.inventory.meta.Damageable) {
				damage += 4
				if (damage >= item.type.maxDurability) {
					damager.inventory.setItemInMainHand(null)
					damager.world.playSound(damager, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
				}
			}
		}
		item.addEnchantmentGlint()
	}

	@EventHandler
	private fun onStripLog(ev: PlayerInteractEvent) {
		if (ev.action != Action.RIGHT_CLICK_BLOCK) return
		if (!ev.hasItem()) return

		if (ev.item?.itemMeta?.persistentDataContainer?.hasFlag(IS_BONKER) != true) return
		if (!Tag.LOGS.isTagged(ev.clickedBlock!!.type)) return
		ev.isCancelled = true
	}
}
