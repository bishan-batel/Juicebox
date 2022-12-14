package app.bishan.juicebox.feature.vehicle

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import io.papermc.paper.event.block.BeaconActivatedEvent
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object Vehicles : Feature("vehicles", true) {
	@EventHandler
	private fun spawnPlane(ev: PlayerInteractEvent) {
		if (!ev.hasItem()) return
		if (ev.item?.type != Material.BLAZE_ROD) return
		if (!isEntityUUID(ev.player, PlayersUUID.BISHAN)) ev.clickedBlock ?: return

//		val loc = ev.clickedBlock!!.location.add(0.0, 1.0, 0.0)
//		((ev.player.world as CraftWorld).handle).addFreshEntity(Biplane(loc), CreatureSpawnEvent.SpawnReason.COMMAND)

		ev.player.sendMessage("huh")

		val cp = (ev.player as CraftPlayer)
		val sp = cp.handle

		val limit = 1
		val center =
			ev.player.getTargetBlock(120)?.location?.toCenterLocation()?.toVector()
				?: ev.player.location.toCenterLocation().toVector()

//		val boban = Bukkit.getOfflinePlayer(PlayersUUID.BISHAN)
//		val skinProperty = when (val pfp = boban.playerProfile) {
//			is CraftPlayerProfile -> pfp.gameProfile
//			is org.bukkit.craftbukkit.v1_19_R1.profile.CraftPlayerProfile -> pfp.buildGameProfile()
//			else -> return
//		}.properties.get("textures").first()
//
//		Bukkit.broadcast(Component.text(skinProperty.toString()))
//		Bukkit.broadcast(Component.text(skinProperty.name))
//		Bukkit.broadcast(Component.text(skinProperty.value))

		for (i in 1..limit) {
			val npcProfile = GameProfile(UUID.randomUUID(), "bishan_")

			npcProfile.properties.put(
				"textures", Property(
					"textures",
					"ewogICJ0aW1lc3RhbXAiIDogMTY3MDQwNzA4NDkzMiwKICAicHJvZmlsZUlkIiA6ICI2YWFmMmQwY2RiMWM0NTI5YjM2M2FlZjhhZjAxMGRjZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJiaXNoYW5fIiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NlNDg0ODllY2RmYzY2ODc5OTY4NjhmZDc0MzViMjM0NTljMjA0MmQyZGZlZTkzZDkyMzU1NmJiOTNiZTRiMzkiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMzQwYzBlMDNkZDI0YTExYjE1YThiMzNjMmE3ZTllMzJhYmIyMDUxYjI0ODFkMGJhN2RlZmQ2MzVjYTdhOTMzIgogICAgfQogIH0KfQ=="
				)
			)
			val npc = ServerPlayer(sp.server, sp.level as ServerLevel, npcProfile, sp.profilePublicKey)

			val offsetX = sin(Math.PI * 2.0 * i.toDouble() / limit) * 0f
			val offsetY = cos(Math.PI * 2.0 * i.toDouble() / limit) * 0f

			npc.setPos(center.x + offsetX, center.y + 1, center.z + offsetY)

			Bukkit.getOnlinePlayers().forEach {
				val conn = (it as CraftPlayer).handle.connection

				val diff = it.location.subtract(Location(it.world, npc.position().x, npc.position().y, npc.position().z))
				val yaw = atan2(diff.z, diff.x) * 180 / Math.PI - 90
//				val yaw = Math.random() * 360
//				npc.yHeadRot = yaw.toFloat()

//				val denom = diff.toVector().multiply(Vector(1, 0, 1)).length()
//				npc.xRot = (atan2(diff.y, denom) * 180 / Math.PI - 90).toFloat()
//				npc.yHeadRotO = npc.xRot

				conn.send(ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc))
				conn.send(ClientboundAddPlayerPacket(npc))


//				ClientboundTeleportEntityPacket
				Bukkit.getScheduler().runTask(JuiceboxPlugin.instance, fun() {
					conn.send(ClientboundRotateHeadPacket(npc, ((yaw % 360) * 256 / 360).toInt().toByte()))
				})
			}


			Bukkit.getScheduler().runTaskLater(
				JuiceboxPlugin.instance, fun() {
					Bukkit.getOnlinePlayers().forEach {
						val conn = (it as CraftPlayer).handle.connection
						conn.send(ClientboundRemoveEntitiesPacket(npc.id))
						conn.send(ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, npc))
					}
				}, 1 * 20L
			)
		}
	}
}
