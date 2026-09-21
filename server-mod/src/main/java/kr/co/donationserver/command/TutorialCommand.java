package kr.co.donationserver.command;

import kr.co.donationserver.service.TutorialService;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class TutorialCommand extends PublicPlayerCommand {
    @Override public String getName() { return "튜토리얼"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public String getUsage(ICommandSender sender) { return "/튜토리얼 [다음|이전|다시]"; }
    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length == 0) { TutorialService.show(player); return; }
        if (args.length != 1) throw new WrongUsageException(getUsage(sender));
        switch (args[0]) {
            case "다음": TutorialService.next(player); break;
            case "이전": TutorialService.previous(player); break;
            case "다시": TutorialService.restart(player); break;
            default: throw new WrongUsageException(getUsage(sender));
        }
    }
}
