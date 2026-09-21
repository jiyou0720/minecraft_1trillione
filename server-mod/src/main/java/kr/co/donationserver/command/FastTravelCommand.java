package kr.co.donationserver.command;
import kr.co.donationserver.network.NetworkHandler;import net.minecraft.command.*;import net.minecraft.entity.player.EntityPlayerMP;import net.minecraft.server.MinecraftServer;
public class FastTravelCommand extends PublicPlayerCommand {public String getName(){return "거점이동";}public int getRequiredPermissionLevel(){return 0;}public String getUsage(ICommandSender s){return "/거점이동";}public void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{NetworkHandler.open(getCommandSenderAsPlayer(sender),0);}}
