package kr.co.donationserver.command;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.data.LocationData;
import kr.co.donationserver.data.ShopData;
import kr.co.donationserver.entity.EntityShopNpc;
import kr.co.donationserver.service.LandVoucher;
import kr.co.donationserver.service.ShopService;
import kr.co.donationserver.util.Texts;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class ShopAdminCommand extends CommandBase {
    @Override public String getName() { return "상점관리"; }
    @Override public java.util.List<String> getAliases() { return java.util.Collections.singletonList("shopadmin"); }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) {
        return "/상점관리 위치설정 | 생성 <이름> | 스킨 <상점ID> <닉네임> | 목록 | 시세 | 시세갱신 | 상품 <상점ID> | 구매권등록 <상점ID> <가격> | 등록 <상점ID> <구매|판매|둘다> <구매가> <판매가> <최저판매가> | 삭제상품 <상점ID> <상품번호> | 삭제 <상점ID>";
    }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) throw new WrongUsageException(getUsage(sender));
        DonationData data = DonationData.get(server.getWorld(0));
        if (args.length == 1 && (args[0].equals("위치설정") || args[0].equals("setlocation"))) {
            EntityPlayerMP admin = getCommandSenderAsPlayer(sender);
            data.trade = LocationData.of(admin);
            data.markDirty();
            sender.sendMessage(Texts.text("&a현재 위치를 거점 이동 메뉴의 상점가 목적지로 설정했습니다."));
            return;
        }
        if (args.length == 1 && (args[0].equals("시세") || args[0].equals("price"))) {
            long remaining = Math.max(0, data.nextShopPriceChange - System.currentTimeMillis());
            sender.sendMessage(Texts.text("&e다음 판매 시세 변경까지 " + (remaining + 59999) / 60000 + "분 남았습니다."));
            return;
        }
        if (args.length == 1 && (args[0].equals("시세갱신") || args[0].equals("updateprices"))) {
            int count = ShopService.updatePrices(server, true);
            sender.sendMessage(Texts.text("&a판매 시세 갱신 완료: " + count + "개 상품 변경"));
            return;
        }
        if ((args[0].equals("생성") || args[0].equals("create")) && args.length >= 2) {
            EntityPlayerMP admin = getCommandSenderAsPlayer(sender);
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            if (name.length() > 40) throw new CommandException("상점 이름은 40자 이하여야 합니다.");
            EntityShopNpc npc = new EntityShopNpc(admin.world);
            npc.setPositionAndRotation(admin.posX, admin.posY, admin.posZ, admin.rotationYaw, 0);
            npc.setCustomNameTag(name);
            npc.setAlwaysRenderNameTag(true);
            npc.setNoAI(true);
            npc.enablePersistence();
            if (!admin.world.spawnEntity(npc)) throw new CommandException("NPC 생성에 실패했습니다.");
            ShopData shop = new ShopData();
            shop.id = data.nextShopId++;
            shop.entityId = npc.getUniqueID();
            shop.dimension = admin.dimension;
            shop.name = name;
            data.shops.put(shop.id, shop);
            data.markDirty();
            sender.sendMessage(Texts.text("&a상점 NPC 생성 완료. ID: " + shop.id + " / " + name));
            return;
        }
        if ((args[0].equals("목록") || args[0].equals("list")) && args.length == 1) {
            if (data.shops.isEmpty()) sender.sendMessage(Texts.text("&e등록된 상점이 없습니다."));
            for (ShopData shop : data.shops.values()) sender.sendMessage(Texts.text("&e#" + shop.id + " " + shop.name + " / 상품 " + shop.products.size() + "개"));
            return;
        }
        if (args.length < 2) throw new WrongUsageException(getUsage(sender));
        int shopId = parseInt(args[1], 1);
        ShopData shop = data.shops.get(shopId);
        if (shop == null) throw new CommandException("상점 ID를 찾을 수 없습니다: " + shopId);
        if ((args[0].equals("스킨") || args[0].equals("skin")) && args.length == 3) {
            if (!args[2].matches("[A-Za-z0-9_]{3,16}")) throw new CommandException("유효한 Minecraft 닉네임을 입력해 주세요.");
            GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(args[2]);
            if (profile == null || profile.getId() == null) throw new CommandException("해당 닉네임의 스킨 프로필을 찾지 못했습니다.");
            profile = server.getMinecraftSessionService().fillProfileProperties(profile, true);
            Property texture = profile.getProperties().get("textures").isEmpty()
                    ? null : profile.getProperties().get("textures").iterator().next();
            if (texture == null || texture.getValue() == null || texture.getValue().isEmpty())
                throw new CommandException("해당 계정의 커스텀 스킨 정보를 받지 못했습니다. 정품 Java Edition 닉네임과 서버 인터넷 연결을 확인해 주세요.");
            WorldServer world = server.getWorld(shop.dimension);
            if (world == null) throw new CommandException("상점 NPC가 있는 차원을 찾지 못했습니다.");
            Entity old = world.getEntityFromUuid(shop.entityId);
            if (old == null) throw new CommandException("상점 NPC가 로딩되지 않았습니다. NPC 주변에서 다시 시도해 주세요.");
            if (old instanceof EntityShopNpc) ((EntityShopNpc) old).setSkin(profile.getName(), profile.getId().toString(),
                    texture.getValue(), texture.hasSignature() ? texture.getSignature() : "");
            else {
                EntityShopNpc npc = new EntityShopNpc(world);
                npc.setPositionAndRotation(old.posX, old.posY, old.posZ, old.rotationYaw, old.rotationPitch);
                npc.setCustomNameTag(shop.name);
                npc.setAlwaysRenderNameTag(true);
                npc.setSkin(profile.getName(), profile.getId().toString(), texture.getValue(),
                        texture.hasSignature() ? texture.getSignature() : "");
                if (!world.spawnEntity(npc)) throw new CommandException("플레이어형 NPC 생성에 실패했습니다.");
                shop.entityId = npc.getUniqueID();
                old.setDead();
            }
            data.markDirty();
            sender.sendMessage(Texts.text("&a상점 #" + shopId + " 외형을 " + profile.getName() + " 스킨으로 변경했습니다."));
            return;
        }
        if ((args[0].equals("구매권등록") || args[0].equals("voucher")) && args.length == 3) {
            long price = parseLong(args[2], 1, Long.MAX_VALUE);
            ShopData.Product product = new ShopData.Product();
            product.item = LandVoucher.create();
            product.buyEnabled = true;
            product.buyPrice = price;
            int index = -1;
            for (int i = 0; i < shop.products.size(); i++)
                if (LandVoucher.isVoucher(shop.products.get(i).item)) { index = i; break; }
            if (index < 0) {
                if (shop.products.size() >= 100) throw new CommandException("상점당 상품은 최대 100개입니다.");
                shop.products.add(product);
                index = shop.products.size() - 1;
            } else shop.products.set(index, product);
            data.markDirty();
            sender.sendMessage(Texts.text("&a#" + shop.id + " 상점의 영지 구매권 가격을 " + Texts.money(price) + "으로 설정했습니다."));
            return;
        }
        if ((args[0].equals("상품") || args[0].equals("products")) && args.length == 2) {
            if (shop.products.isEmpty()) sender.sendMessage(Texts.text("&e등록된 상품이 없습니다."));
            for (int i = 0; i < shop.products.size(); i++) {
                ShopData.Product p = shop.products.get(i);
                sender.sendMessage(Texts.text("&e#" + (i + 1) + " " + p.item.getDisplayName() +
                        " / 구매 " + (p.buyEnabled ? Texts.money(p.buyPrice) : "불가") +
                        " / 판매 " + (p.sellEnabled ? Texts.money(p.currentSellPrice) : "불가") +
                        " / 이전 " + Texts.money(p.previousSellPrice) + " / 최저 " + Texts.money(p.minSellPrice) + " / 연속 하락 " + p.consecutiveDrops));
            }
            return;
        }
        if ((args[0].equals("등록") || args[0].equals("register")) && args.length == 6) {
            EntityPlayerMP admin = getCommandSenderAsPlayer(sender);
            ItemStack held = admin.getHeldItemMainhand();
            if (held.isEmpty()) throw new CommandException("등록할 상품을 주 손에 들어 주세요.");
            boolean buy = args[2].equals("구매") || args[2].equals("둘다") || args[2].equals("buy") || args[2].equals("both");
            boolean sell = args[2].equals("판매") || args[2].equals("둘다") || args[2].equals("sell") || args[2].equals("both");
            if (!buy && !sell) throw new CommandException("거래 방식은 구매, 판매, 둘다 중 하나입니다.");
            long buyPrice = parseLong(args[3], 0, Long.MAX_VALUE);
            long sellPrice = parseLong(args[4], 0, Long.MAX_VALUE);
            long floor = parseLong(args[5], 0, Long.MAX_VALUE);
            if (buy && buyPrice < 1 || sell && (sellPrice < 1 || floor < 1 || floor > sellPrice))
                throw new CommandException("활성화된 가격은 1원 이상, 최저판매가는 판매가 이하여야 합니다.");
            ShopData.Product product = new ShopData.Product();
            product.item = held.copy();
            product.item.setCount(1);
            product.buyEnabled = buy;
            product.sellEnabled = sell;
            product.buyPrice = buy ? buyPrice : 0;
            product.baseSellPrice = sell ? sellPrice : 0;
            product.minSellPrice = sell ? floor : 0;
            product.currentSellPrice = sell ? sellPrice : 0;
            product.previousSellPrice = product.currentSellPrice;
            int index = -1;
            for (int i = 0; i < shop.products.size(); i++) {
                ItemStack old = shop.products.get(i).item;
                if (ItemStack.areItemsEqual(old, product.item) && ItemStack.areItemStackTagsEqual(old, product.item)) { index = i; break; }
            }
            if (index < 0) {
                if (shop.products.size() >= 100) throw new CommandException("상점당 상품은 최대 100개입니다.");
                shop.products.add(product); index = shop.products.size() - 1;
            }
            else shop.products.set(index, product);
            data.markDirty();
            sender.sendMessage(Texts.text("&a상품 #" + (index + 1) + " 등록/수정: " + product.item.getDisplayName()));
            return;
        }
        if ((args[0].equals("삭제상품") || args[0].equals("removeproduct")) && args.length == 3) {
            int index = parseInt(args[2], 1) - 1;
            if (index >= shop.products.size()) throw new CommandException("상품 번호가 없습니다.");
            shop.products.remove(index);
            data.markDirty();
            sender.sendMessage(Texts.text("&a상품을 삭제했습니다."));
            return;
        }
        if ((args[0].equals("삭제") || args[0].equals("delete")) && args.length == 2) {
            WorldServer world = server.getWorld(shop.dimension);
            if (world != null) {
                Entity npc = world.getEntityFromUuid(shop.entityId);
                if (npc != null) npc.setDead();
            }
            data.shops.remove(shop.id);
            data.markDirty();
            sender.sendMessage(Texts.text("&a상점 NPC와 등록 상품을 삭제했습니다."));
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }
}
