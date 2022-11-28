package app.bishan.juicebox.feature.vehicle

import app.bishan.juicebox.feature.Feature
import com.mojang.authlib.GameProfile
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.ProfilePublicKey
import net.minecraft.world.level.chunk.ChunkGenerators
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.*

object Vehicles : Feature("vehicles", true) {
	@EventHandler
	private fun spawnPlane(ev: PlayerInteractEvent) {
		if (!ev.hasItem()) return
		if (ev.item?.type != Material.BLAZE_ROD) return
		ev.clickedBlock ?: return

//		val loc = ev.clickedBlock!!.location.add(0.0, 1.0, 0.0)
//		((ev.player.world as CraftWorld).handle).addFreshEntity(Biplane(loc), CreatureSpawnEvent.SpawnReason.COMMAND)

		ev.player.sendMessage("huh")

		val cp = (ev.player as CraftPlayer)
		val sp = cp.handle
		val npcProfile = GameProfile(UUID.randomUUID(), "bishan_")

		// create a public key for the npc
		val pubKey = object : PublicKey {
			override fun getAlgorithm() = "RSA"
			override fun getFormat() = "X.509"
			override fun getEncoded() = byteArrayOf()
		}
		ProfilePublicKey.createValidated(
			{ _, _ -> true },
			npcProfile.id,
			ProfilePublicKey.Data(Instant.now(), pubKey, pubKey.encoded),
			Duration.ofDays(1)
		)
		val npc = ServerPlayer(sp.server, sp.level as ServerLevel, npcProfile, sp.profilePublicKey)
		npc.setPos(
			ev.clickedBlock!!.location.x, ev.clickedBlock!!.location.y, ev.clickedBlock!!.location.z
		)

		sp.connection.send(ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc))
		sp.connection.send(ClientboundAddPlayerPacket(npc))
	}
}
