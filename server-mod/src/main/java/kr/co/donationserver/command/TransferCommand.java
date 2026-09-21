package kr.co.donationserver.command;

import kr.co.donationserver.service.EconomyService;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import java.util.Collections;
import java.util.List;

public class TransferCommand extends PublicPlayerCommand {
    @Override public String getName() { return "송금"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public List<String> getAliases() { return Collections.singletonList("pay"); }
    @Override public String getUsage(ICommandSender sender) { return "/송금 <접속 중인 닉네임> <금액>"; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP from = getCommandSenderAsPlayer(sender);
        if (args.length != 2) throw new WrongUsageException(getUsage(sender));
        EntityPlayerMP to = getPlayer(server, sender, args[0]);
        long amount = parseLong(args[1], 1, Long.MAX_VALUE);
        EconomyService.transfer(from, to, amount);
    }
}
