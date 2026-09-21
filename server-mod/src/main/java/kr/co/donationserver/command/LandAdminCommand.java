package kr.co.donationserver.command;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.util.Texts;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import java.util.Collections;
import java.util.List;

public class LandAdminCommand extends CommandBase {
    @Override public String getName() { return "영지관리"; }
    @Override public List<String> getAliases() { return Collections.singletonList("landadmin"); }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) { return "/영지관리 최대 <청크 수, 0은 무제한> | 현황 (구매권 가격은 /상점관리 구매권등록)"; }
    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        DonationData data = DonationData.get(server.getWorld(0));
        if (args.length == 1 && (args[0].equals("현황") || args[0].equals("status"))) {
            sender.sendMessage(Texts.text("&e영지 확장: 구매권 1장 / 최대: " + (data.maxLandClaims > 0 ? data.maxLandClaims + "청크" : "무제한") +
                    " / 등록: " + data.landClaims.size() + "청크"));
            return;
        }
        if (args.length == 2 && (args[0].equals("최대") || args[0].equals("max"))) {
            data.maxLandClaims = parseInt(args[1], 0, Integer.MAX_VALUE);
            data.markDirty();
            sender.sendMessage(Texts.text("&a영지 최대 보유: " + (data.maxLandClaims == 0 ? "무제한" : data.maxLandClaims + "청크")));
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }
}
