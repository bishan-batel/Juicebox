package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import io.papermc.paper.event.player.PlayerPurchaseEvent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe

object NPC : Feature("npc", false) {
//	@EventHandler
//	fun onInteract(ev: PlayerInteractEntityEvent) {
//		if (!isEntityUUID(ev.rightClicked, PlayersUUID.BISHAN)) return
//
//		val player = ev.player
//
//		val inv = Bukkit.createMerchant(Component.text("Bishan's Wares"))
//		inv.recipes = listOf(run {
//			val bishan = ev.rightClicked as Player
//			val result = bishan.inventory.itemInMainHand
//			if (result.type == Material.AIR) return
//			val recipe = BishanRecipe(bishan, result, 1)
//			recipe.addIngredient(ItemStack(Material.EMERALD, 10))
//			recipe.maxUses = 1
//			recipe.setIgnoreDiscounts(true)
//			recipe
//		})
//		player.openMerchant(inv, true)
//	}
//
//	class BishanRecipe(val bishan: Player, stack: ItemStack, maxUses: Int) : MerchantRecipe(stack, maxUses)
}
