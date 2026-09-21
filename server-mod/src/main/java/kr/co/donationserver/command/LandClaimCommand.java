package kr.co.donationserver.command;

import kr.co.donationserver.service.LandService;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.util.Texts;
import kr.co.donationserver.event.ServerEvents;
import net.minecraft.entity.player.EntityPlayerMP;
import java.util.Collections;
import java.util.List;

public class LandClaimCommand extends PublicPlayerCommand {
    @Override public String getName() { return "영지설정"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public List<String> getAliases() { return Collections.singletonList("claim"); }
    @Override public String getUsage(ICommandSender sender) { return "/영지설정 [정보|표시|해제|유저추가 <접속 중인 닉네임>|유저삭제 <닉네임 또는 UUID>|유저목록]"; }
    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP owner = getCommandSenderAsPlayer(sender);
        if (args.length == 0) { LandService.claim(owner); return; }
        if (args.length == 1 && (args[0].equals("해제") || args[0].equals("unclaim"))) { LandService.release(owner); return; }
        DonationData data = DonationData.get(owner.world);
        if (args.length == 1 && args[0].equals("표시")) { ServerEvents.INSTANCE.showLandBorders(owner); return; }
        if (args.length == 1 && (args[0].equals("정보") || args[0].equals("info"))) {
            int x = owner.getPosition().getX() >> 4;
            int z = owner.getPosition().getZ() >> 4;
            java.util.UUID holder = data.landClaims.get(DonationData.claimKey(owner.dimension, x, z));
            String status = holder == null ? "미등록" : holder.equals(owner.getUniqueID()) ? "내 영지" : "다른 유저의 영지";
            owner.sendMessage(Texts.text("&e현재 청크 (" + x + ", " + z + "): " + status + " / 내 보유: " + data.claimCount(owner.getUniqueID()) + "청크"));
            return;
        }
        if (data.claimCount(owner.getUniqueID()) == 0) throw new CommandException("먼저 /영지설정으로 영지를 등록해 주세요.");
        if (args.length == 1 && (args[0].equals("유저목록") || args[0].equals("trusted"))) {
            java.util.Set<java.util.UUID> members = data.landTrusted.get(owner.getUniqueID());
            if (members == null || members.isEmpty()) { owner.sendMessage(Texts.text("&e추가된 유저가 없습니다.")); return; }
            for (java.util.UUID id : members) {
                EntityPlayerMP online = server.getPlayerList().getPlayerByUUID(id);
                owner.sendMessage(Texts.text("&e" + (online == null ? id.toString() : online.getName())));
            }
            return;
        }
        if (args.length == 2 && (args[0].equals("유저추가") || args[0].equals("유저삭제") || args[0].equals("trust") || args[0].equals("untrust"))) {
            boolean added = args[0].equals("유저추가") || args[0].equals("trust");
            EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(args[1]);
            java.util.UUID targetId;
            if (target != null) targetId = target.getUniqueID();
            else if (!added) try { targetId = java.util.UUID.fromString(args[1]); }
            catch (IllegalArgumentException ex) { throw new CommandException("삭제할 유저가 접속 중이 아니면 /영지설정 유저목록에 표시된 UUID를 사용하세요."); }
            else throw new CommandException("추가할 유저가 접속 중이어야 합니다.");
            if (targetId.equals(owner.getUniqueID())) throw new CommandException("본인은 이미 영지를 사용할 수 있습니다.");
            boolean changed = added ? data.trusted(owner.getUniqueID()).add(targetId)
                    : data.trusted(owner.getUniqueID()).remove(targetId);
            if (changed) data.markDirty();
            owner.sendMessage(Texts.text("&a" + (target == null ? targetId.toString() : target.getName()) + "님을 영지 이용 목록에서 " + (added ? "추가" : "삭제") + "했습니다."));
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }
}
