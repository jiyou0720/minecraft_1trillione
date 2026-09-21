package kr.co.donationserver.client.gui;

import kr.co.donationserver.network.NetworkHandler;
import kr.co.donationserver.network.PacketAction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** A chest-style destination picker; the server remains responsible for travel checks. */
public class GuiFastTravel extends GuiScreen {
    private static final int[] COLUMNS = {1, 4, 7};
    private static final String[] NAMES = {"§a내 집", "§b상점가", "§e이전 위치"};
    private static final String[] DESCRIPTIONS = {
            "설정한 집으로 이동합니다", "NPC 상점가로 이동합니다", "이동 전 장소로 돌아갑니다"
    };
    private static final ItemStack[] ICONS = {
            new ItemStack(Items.BED), new ItemStack(Items.EMERALD), new ItemStack(Items.COMPASS)
    };

    private int left() { return width / 2 - 88; }
    private int top() { return height / 2 - 43; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = left(), y = top();
        drawRect(x - 5, y - 5, x + 181, y + 81, 0xFFB8B2A5);
        drawRect(x - 3, y - 3, x + 179, y + 79, 0xFF292D34);
        drawRect(x, y, x + 176, y + 76, 0xFF3A3E45);
        fontRenderer.drawString("거점 이동", x + 8, y + 5, 0xFFE8C879);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18, sy = y + 17 + row * 18;
                drawRect(sx, sy, sx + 18, sy + 18, 0xFF858B91);
                drawRect(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF20242B);
            }
        }

        int hovered = hoveredDestination(mouseX, mouseY);
        for (int i = 0; i < ICONS.length; i++) {
            int sx = x + 8 + COLUMNS[i] * 18, sy = y + 36;
            if (hovered == i) drawRect(sx - 1, sy - 1, sx + 17, sy + 17, 0x99FFE7A0);
            mc.getRenderItem().renderItemAndEffectIntoGUI(ICONS[i], sx, sy);
        }
        if (hovered >= 0) {
            List<String> tooltip = Arrays.asList(NAMES[hovered], "§7" + DESCRIPTIONS[hovered],
                    "§7이동 중 움직이면 취소됩니다", "§e클릭하여 선택");
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    private int hoveredDestination(int mouseX, int mouseY) {
        int x = left(), y = top();
        for (int i = 0; i < COLUMNS.length; i++) {
            int sx = x + 8 + COLUMNS[i] * 18, sy = y + 36;
            if (mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17) return i;
        }
        return -1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int destination = hoveredDestination(mouseX, mouseY);
        if (mouseButton == 0 && destination >= 0) {
            NetworkHandler.CHANNEL.sendToServer(new PacketAction(destination));
            mc.displayGuiScreen(null);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
