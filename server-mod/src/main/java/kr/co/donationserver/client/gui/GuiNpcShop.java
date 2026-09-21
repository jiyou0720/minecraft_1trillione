package kr.co.donationserver.client.gui;

import kr.co.donationserver.network.NetworkHandler;
import kr.co.donationserver.network.PacketOpenShop;
import kr.co.donationserver.network.PacketShopTrade;
import kr.co.donationserver.util.Texts;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import java.io.IOException;
import java.util.Arrays;

public class GuiNpcShop extends GuiScreen {
    private static final int PAGE_SIZE = 45;
    private final PacketOpenShop shop;
    private int page, selected = -1, quantity = 1;

    public GuiNpcShop(PacketOpenShop shop) { this.shop = shop; }

    @Override public void initGui() {
        buttonList.clear();
        int x = width / 2 - 88, y = height / 2 - 105;
        if (selected < page * PAGE_SIZE || selected >= Math.min(shop.rows.size(), (page + 1) * PAGE_SIZE))
            selected = page * PAGE_SIZE < shop.rows.size() ? page * PAGE_SIZE : -1;
        buttonList.add(new GuiShopButton(1001, x + 4, y + 116, 48, 18, "이전"));
        buttonList.add(new GuiShopButton(1002, x + 124, y + 116, 48, 18, "다음"));
        buttonList.add(new GuiShopButton(2001, x + 4, y + 151, 52, 18, "1개", quantity == 1));
        buttonList.add(new GuiShopButton(2010, x + 62, y + 151, 52, 18, "10개", quantity == 10));
        buttonList.add(new GuiShopButton(2064, x + 120, y + 151, 52, 18, "64개", quantity == 64));
        PacketOpenShop.Row item = selected < 0 ? null : shop.rows.get(selected);
        buttonList.add(new GuiShopButton(3001, x + 4, y + 174, 82, 20, "구매"));
        buttonList.add(new GuiShopButton(3002, x + 90, y + 174, 82, 20, "판매"));
        for (GuiButton button : buttonList) {
            if (button.id == 1001) button.enabled = page > 0;
            if (button.id == 1002) button.enabled = (page + 1) * PAGE_SIZE < shop.rows.size();
            if (button.id >= 2001 && button.id <= 2064) button.enabled = button.id - 2000 != quantity;
            if (button.id == 3001) button.enabled = item != null && item.buy;
            if (button.id == 3002) button.enabled = item != null && item.sell;
        }
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1001 || button.id == 1002) {
            page += button.id == 1001 ? -1 : 1;
            initGui();
            return;
        }
        if (button.id == 2001 || button.id == 2010 || button.id == 2064) {
            quantity = button.id - 2000;
            initGui();
            return;
        }
        if ((button.id == 3001 || button.id == 3002) && selected >= 0) {
            boolean buy = button.id == 3001;
            PacketOpenShop.Row item = shop.rows.get(selected);
            NetworkHandler.CHANNEL.sendToServer(new PacketShopTrade(shop.shopId, selected, buy, quantity,
                    buy ? item.buyPrice : item.sellPrice));
        }
    }

    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0) {
            int index = hoveredIndex(mx, my);
            if (index >= 0) { selected = index; initGui(); return; }
        }
        super.mouseClicked(mx, my, button);
    }

    private int hoveredIndex(int mx, int my) {
        int x = width / 2 - 88, y = height / 2 - 105;
        int sx = mx - x - 7, sy = my - y - 17;
        if (sx < 0 || sy < 0 || sx >= 162 || sy >= 90) return -1;
        int col = sx / 18, row = sy / 18;
        int index = page * PAGE_SIZE + row * 9 + col;
        return index < shop.rows.size() ? index : -1;
    }

    @Override public void drawScreen(int mx, int my, float partialTicks) {
        drawDefaultBackground();
        int x = width / 2 - 88, y = height / 2 - 105;
        drawRect(x - 5, y - 5, x + 181, y + 215, 0xFFB8B2A5);
        drawRect(x - 3, y - 3, x + 179, y + 213, 0xFF292D34);
        drawRect(x, y, x + 176, y + 112, 0xFF3A3E45);
        drawRect(x, y + 113, x + 176, y + 210, 0xFF30343B);
        String title = fontRenderer.trimStringToWidth(shop.name, 158);
        fontRenderer.drawString(title, x + 8, y + 5, 0xFFE8C879);
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int sx = x + 7 + slot % 9 * 18, sy = y + 17 + slot / 9 * 18;
            drawRect(sx, sy, sx + 18, sy + 18, 0xFF858B91);
            drawRect(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF20242B);
        }
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            if (index >= shop.rows.size()) break;
            int sx = x + 8 + slot % 9 * 18, sy = y + 18 + slot / 9 * 18;
            if (index == selected) drawRect(sx - 1, sy - 1, sx + 17, sy + 17, 0x88FFD76A);
            ItemStack item = shop.rows.get(index).item;
            mc.getRenderItem().renderItemAndEffectIntoGUI(item, sx, sy);
            mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, item, sx, sy, null);
        }
        PacketOpenShop.Row item = selected < 0 ? null : shop.rows.get(selected);
        drawCenteredString(fontRenderer, "페이지 " + (page + 1) + "/" + Math.max(1, (shop.rows.size() + PAGE_SIZE - 1) / PAGE_SIZE), width / 2, y + 121, 0xFFFFFF);
        if (item != null) {
            String name = item.item.getDisplayName();
            if (fontRenderer.getStringWidth(name) > 170) name = fontRenderer.trimStringToWidth(name, 155) + "...";
            drawCenteredString(fontRenderer, name, width / 2, y + 138, 0xFFFFFF);
            if (item.sell) {
                String trend = item.sellPrice > item.previousSellPrice ? "§c▲ 상승"
                        : item.sellPrice < item.previousSellPrice ? "§9▼ 하락" : "§7― 변동 없음";
                drawCenteredString(fontRenderer, "이전 " + Texts.money(item.previousSellPrice) + "  " + trend, width / 2, y + 199, 0xFFFFFF);
            }
        } else drawCenteredString(fontRenderer, "등록된 상품이 없습니다", width / 2, y + 138, 0xAAAAAA);
        super.drawScreen(mx, my, partialTicks);
        int hovered = hoveredIndex(mx, my);
        if (hovered >= 0) renderToolTip(shop.rows.get(hovered).item, mx, my);
        else if (item != null) {
            for (GuiButton button : buttonList) {
                if (mx < button.x || mx >= button.x + button.width || my < button.y || my >= button.y + button.height)
                    continue;
                if (button.id == 3001) drawHoveringText(Arrays.asList("§b구매", item.buy
                        ? "§7단가 " + Texts.money(item.buyPrice) + "  /  수량 " + quantity : "§c구매할 수 없는 상품"), mx, my);
                if (button.id == 3002) drawHoveringText(Arrays.asList("§a판매", item.sell
                        ? "§7단가 " + Texts.money(item.sellPrice) + "  /  수량 " + quantity : "§c판매할 수 없는 상품"), mx, my);
                break;
            }
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
