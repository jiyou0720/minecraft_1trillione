package kr.co.donationserver.command;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.util.Texts;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import java.util.Collections;
import java.util.List;

public class ShopRegionCommand extends CommandBase {
    @Override public String getName() { return "상점가설정"; }
    @Override public List<String> getAliases() { return Collections.singletonList("shopregion"); }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) { return "/상점가설정 <반경 1~256|정보|해제>"; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) throw new WrongUsageException(getUsage(sender));
        DonationData data = DonationData.get(server.getWorld(0));
        if (args[0].equals("정보") || args[0].equals("info")) {
            sender.sendMessage(Texts.text(data.shopRegionRadius == 0 ? "&e상점가 보호가 설정되지 않았습니다."
                    : "&e상점가: 차원 " + data.shopRegionDimension + " / 중심 " + data.shopRegionX + ", " + data.shopRegionZ
                    + " / 반경 " + data.shopRegionRadius + "블록"));
            return;
        }
        if (args[0].equals("해제") || args[0].equals("clear")) {
            data.shopRegionRadius = 0;
            data.markDirty();
            sender.sendMessage(Texts.text("&a상점가 보호를 해제했습니다."));
            return;
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int radius = parseInt(args[0], 1, 256);
        data.shopRegionDimension = player.dimension;
        data.shopRegionX = player.getPosition().getX();
        data.shopRegionZ = player.getPosition().getZ();
        data.shopRegionRadius = radius;
        data.markDirty();
        sender.sendMessage(Texts.text("&a현재 위치를 중심으로 반경 " + radius + "블록의 상점가를 보호합니다."));
    }
}
