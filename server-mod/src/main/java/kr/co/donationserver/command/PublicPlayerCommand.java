package kr.co.donationserver.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Player-facing commands must not inherit an OP-only command permission check. */
public abstract class PublicPlayerCommand extends CommandBase {
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender instanceof EntityPlayerMP;
    }
}
