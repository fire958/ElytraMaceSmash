package com.example.elytramacesmash;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.awt.Color;

public class Hud {
    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("elytramacesmash", "hud"),
                (ctx, tickCounter) -> render(ctx));
    }

    public static int rainbow(double offset) {
        float hue = (float) (((System.currentTimeMillis() % 3000L) / 3000.0 + offset) % 1.0);
        if (hue < 0) hue += 1f;
        return Color.HSBtoRGB(hue, 0.85f, 1.0f) | 0xFF000000;
    }

    public static void rgbText(DrawContext ctx, TextRenderer tr, String s, int x, int y) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            ctx.drawTextWithShadow(tr, ch, cx, y, rainbow(i * 0.05));
            cx += tr.getWidth(ch);
        }
    }

    public static void border(DrawContext ctx, int x, int y, int w, int h) {
        for (int i = 0; i < w; i += 4) {
            int c = rainbow(i / (double) w);
            int sw = Math.min(4, w - i);
            ctx.fill(x + i, y, x + i + sw, y + 1, c);
            ctx.fill(x + i, y + h - 1, x + i + sw, y + h, c);
        }
        ctx.fill(x, y, x + 1, y + h, rainbow(0.0));
        ctx.fill(x + w - 1, y, x + w, y + h, rainbow(0.5));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int x = 6, y = 6, w = 190;

        if (!ElytraMaceSmashClient.isActive()) {
            int h = 30;
            ctx.fill(x, y, x + w, y + h, 0x99101018);
            border(ctx, x, y, w, h);
            rgbText(ctx, tr, "ElytraMaceSmash", x + 6, y + 5);
            ctx.drawTextWithShadow(tr, "OFF", x + w - 24, y + 5, 0xFF888888);
            ctx.drawTextWithShadow(tr, "G Players  |  V Mobs  |  X Friends", x + 6, y + 17, 0xFFAAAAAA);
            return;
        }

        LivingEntity t = ElytraMaceSmashClient.getTarget();
        int h = 72;
        ctx.fill(x, y, x + w, y + h, 0xAA101018);
        border(ctx, x, y, w, h);

        String modeName = ElytraMaceSmashClient.getMode() == ElytraMaceSmashClient.Mode.PLAYERS ? "PLAYERS" : "MOBS";
        rgbText(ctx, tr, "ElytraMaceSmash", x + 6, y + 5);
        rgbText(ctx, tr, "[" + modeName + "]  ON", x + w - 6 - tr.getWidth("[" + modeName + "]  ON"), y + 5);

        if (t != null) {
            double dist = mc.player.distanceTo(t);
            double speed = mc.player.getVelocity().length() * 20.0;
            ctx.drawTextWithShadow(tr, "Target: " + t.getName().getString(), x + 6, y + 18, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr,
                    String.format("Dist: %.1fm   Speed: %.0f b/s", dist, speed), x + 6, y + 30, 0xFFDDDDDD);

            float hp = t.getHealth();
            float max = Math.max(1f, t.getMaxHealth());
            float frac = Math.max(0f, Math.min(1f, hp / max));
            int bx = x + 6, by = y + 43, bw = w - 12;
            ctx.fill(bx, by, bx + bw, by + 7, 0xFF303030);
            ctx.fill(bx, by, bx + (int) (bw * frac), by + 7, Color.HSBtoRGB(frac * 0.33f, 0.9f, 1f) | 0xFF000000);
            ctx.drawTextWithShadow(tr, String.format("%.1f / %.1f HP", hp, max), bx + 2, by - 1, 0xFFFFFFFF);

            rgbText(ctx, tr, "Status: " + ElytraMaceSmashClient.getStatus(), x + 6, y + 56);
        }
    }
}
