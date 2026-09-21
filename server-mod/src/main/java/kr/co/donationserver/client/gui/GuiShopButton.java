package kr.co.donationserver.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Draws compact shop controls without stretching the vanilla button texture. */
final class GuiShopButton extends GuiButton {
    private final boolean selected;

    GuiShopButton(int id, int x, int y, int width, int height, String label) {
        this(id, x, y, width, height, label, false);
    }

    GuiShopButton(int id, int x, int y, int width, int height, String label, boolean selected) {
        super(id, x, y, width, height, label);
        this.selected = selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int border = selected ? 0xFFE8C879 : !enabled ? 0xFF697079
                : hovered ? 0xFF90D8E8 : 0xFF8998A7;
        int background = selected ? 0xFF4A4533 : !enabled ? 0xFF30343B
                : hovered ? 0xFF435765 : 0xFF38434E;
        int foreground = selected ? 0xFFFFE5A1 : !enabled ? 0xFFBCC2C8 : 0xFFF5F7F8;
        drawRect(x, y, x + width, y + height, border);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, background);
        String label = mc.fontRenderer.trimStringToWidth(displayString, width - 8);
        drawCenteredString(mc.fontRenderer, label, x + width / 2, y + (height - 8) / 2, foreground);
    }
}
