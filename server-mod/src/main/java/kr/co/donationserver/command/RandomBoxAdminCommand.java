package kr.co.donationserver.command;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.data.RandomBoxData;
import kr.co.donationserver.service.RandomBoxService;
import kr.co.donationserver.util.Texts;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import java.util.Collections;
import java.util.List;

public class RandomBoxAdminCommand extends CommandBase {
    @Override public String getName() { return "스킨박스관리"; }
    @Override public List<String> getAliases() { return Collections.singletonList("skinboxadmin"); }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) {
        return "/스킨박스관리 생성 <이름> | 목록 | 보상목록 <ID> | 보상추가 <ID> <가중치> | 보상삭제 <ID> <번호> | 받기 <ID>";
    }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        DonationData data = DonationData.get(server.getWorld(0));
        if (args.length >= 2 && (args[0].equals("생성") || args[0].equals("create"))) {
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            if (name.length() > 30) throw new CommandException("이름은 30자 이하여야 합니다.");
            RandomBoxData box = new RandomBoxData();
            box.id = data.nextRandomBoxId++;
            box.name = name;
            data.randomBoxes.put(box.id, box);
            data.markDirty();
            sender.sendMessage(Texts.text("&a스킨 랜덤박스 생성: ID " + box.id + " / " + name));
            return;
        }
        if (args.length == 1 && (args[0].equals("목록") || args[0].equals("list"))) {
            if (data.randomBoxes.isEmpty()) sender.sendMessage(Texts.text("&e등록된 스킨 랜덤박스가 없습니다."));
            for (RandomBoxData box : data.randomBoxes.values())
                sender.sendMessage(Texts.text("&e#" + box.id + " " + box.name + " / 보상 " + box.rewards.size() + "개"));
            return;
        }
        if (args.length < 2) throw new WrongUsageException(getUsage(sender));
        int id = parseInt(args[1], 1);
        RandomBoxData box = data.randomBoxes.get(id);
        if (box == null) throw new CommandException("스킨 랜덤박스 ID를 찾을 수 없습니다: " + id);
        if (args.length == 2 && (args[0].equals("보상목록") || args[0].equals("rewards"))) {
            int total = 0;
            for (RandomBoxData.Reward reward : box.rewards) total += reward.weight;
            for (int i = 0; i < box.rewards.size(); i++) {
                RandomBoxData.Reward reward = box.rewards.get(i);
                sender.sendMessage(Texts.text("&e#" + (i + 1) + " " + reward.item.getDisplayName()
                        + " x" + reward.item.getCount() + " / 가중치 " + reward.weight + "/" + total));
            }
            return;
        }
        if (args.length == 3 && (args[0].equals("보상추가") || args[0].equals("addreward"))) {
            EntityPlayerMP admin = getCommandSenderAsPlayer(sender);
            ItemStack held = admin.getHeldItemMainhand();
            if (held.isEmpty()) throw new CommandException("보상 아이템을 주 손에 들어 주세요.");
            if (RandomBoxService.boxId(held) > 0) throw new CommandException("랜덤박스를 보상으로 등록할 수 없습니다.");
            if (box.rewards.size() >= 100) throw new CommandException("보상은 최대 100개까지 등록할 수 있습니다.");
            int weight = parseInt(args[2], 1, 1000000);
            box.rewards.add(new RandomBoxData.Reward(held.copy(), weight));
            data.markDirty();
            sender.sendMessage(Texts.text("&a보상 #" + box.rewards.size() + " 등록: " + held.getDisplayName() + " / 가중치 " + weight));
            return;
        }
        if (args.length == 3 && (args[0].equals("보상삭제") || args[0].equals("removereward"))) {
            int index = parseInt(args[2], 1) - 1;
            if (index >= box.rewards.size()) throw new CommandException("보상 번호가 없습니다.");
            box.rewards.remove(index);
            data.markDirty();
            sender.sendMessage(Texts.text("&a보상을 삭제했습니다."));
            return;
        }
        if (args.length == 2 && (args[0].equals("받기") || args[0].equals("get"))) {
            EntityPlayerMP admin = getCommandSenderAsPlayer(sender);
            ItemStack item = RandomBoxService.create(box);
            if (!admin.inventory.addItemStackToInventory(item) && !item.isEmpty()) admin.dropItem(item, false);
            admin.inventoryContainer.detectAndSendChanges();
            sender.sendMessage(Texts.text("&a[스킨 랜덤박스] " + box.name + " 1개를 받았습니다."));
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }
}
