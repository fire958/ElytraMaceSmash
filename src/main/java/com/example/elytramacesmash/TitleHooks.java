package com.example.elytramacesmash;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TitleHooks {
    private static Screen lastScreen = null;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen) && !(screen instanceof GameMenuScreen)) return;

            ButtonWidget friends = ButtonWidget.builder(
                    Text.literal("Friends").formatted(Formatting.AQUA),
                    b -> client.setScreen(new FriendsScreen(screen)))
                    .dimensions(8, 24, 90, 20).build();
            Screens.getButtons(screen).add(friends);

            if (screen != lastScreen) {
                lastScreen = screen;
                ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, tickDelta) ->
                        Hud.rgbText(ctx, client.textRenderer, "ElytraMaceSmash", 8, 10));
            }
        });
    }
}
