package app.bishan.juicebox.utils

import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.entity.Player

val Player.connection: ServerGamePacketListenerImpl get() = (this as CraftPlayer).handle.connection
