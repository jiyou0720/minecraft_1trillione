package kr.co.donationserver.command;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.data.LocationData;
import kr.co.donationserver.util.Texts;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import java.util.Arrays;
import java.util.List;

public class JoinSpawnCommand extends CommandBase {
    @Override public String getName() { return "유저접속장소설정"; }
    @Override public List<String> getAliases() { return Arrays.asList("접속장소설정", "joinspawn"); }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) { return "/유저접속장소설정 [현재|기본|정보]"; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        DonationData data = DonationData.get(server.getWorld(0));
        if (args.length == 0 || args.length == 1 && (args[0].equals("현재") || args[0].equals("here"))) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            data.joinSpawn = LocationData.of(player);
            data.markDirty();
            sender.sendMessage(Texts.text("&a신규 유저 접속 장소를 현재 위치로 설정했습니다."));
            return;
        }
        if (args.length == 1 && (args[0].equals("기본") || args[0].equals("default"))) {
            BlockPos surface = server.getWorld(0).getHeight(new BlockPos(0, 0, 0));
            data.joinSpawn = new LocationData(0, 0.5, surface.getY(), 0.5, 0, 0);
            data.markDirty();
            sender.sendMessage(Texts.text("&a신규 유저 접속 장소를 오버월드 0, 0 지표면으로 설정했습니다."));
            return;
        }
        if (args.length == 1 && (args[0].equals("정보") || args[0].equals("info"))) {
            LocationData spawn = data.joinSpawn;
            sender.sendMessage(Texts.text(spawn == null ? "&e신규 유저 접속 장소: 기본 0, 0 지표면"
                    : "&e신규 유저 접속 장소: 차원 " + spawn.dimension + " / " + spawn.x + ", " + spawn.y + ", " + spawn.z));
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }
}
