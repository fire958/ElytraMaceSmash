package com.example.elytramacesmash;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class FriendsScreen extends Screen {
    private static final int PER_PAGE = 6;
    private static final int PW = 250, PH = 252;

    private final Screen parent;
    private TextFieldWidget input;
    private int page = 0;

    public FriendsScreen(Screen parent) {
        super(Text.literal("Friends"));
        this.parent = parent;
    }

    private int px() { return (width - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    @Override
    protected void init() {
        clearChildren();
        int px = px(), py = py();
        List<String> all = Friends.all();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;

        input = new TextFieldWidget(textRenderer, px + 12, py + 34, 160, 20, Text.literal("Player name"));
        input.setMaxLength(16);
        input.setPlaceholder(Text.literal("Player name..."));
        addDrawableChild(input);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
            Friends.add(input.getText());
            init();
        }).dimensions(px + 178, py + 34, 60, 20).build());

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            final String n = all.get(start + i);
            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> {
                Friends.remove(n);
                init();
            }).dimensions(px + 178, py + 62 + i * 22, 60, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), b -> {
            if (page > 0) { page--; init(); }
        }).dimensions(px + 12, py + 198, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), b -> {
            if (page < pages - 1) { page++; init(); }
        }).dimensions(px + 168, py + 198, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(px + 12, py + 224, PW - 24, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int px = px(), py = py();
        ctx.fill(px, py, px + PW, py + PH, 0xE0101018);
        Hud.border(ctx, px, py, PW, PH);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int px = px(), py = py();

        String title = "ElytraMaceSmash - Friends";
        Hud.rgbText(ctx, textRenderer, title, px + (PW - textRenderer.getWidth(title)) / 2, py + 10);
        ctx.drawTextWithShadow(textRenderer, "Friends are never targeted.", px + 12, py + 20, 0xFF999999);

        List<String> all = Friends.all();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        int start = page * PER_PAGE;
        if (all.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "No friends added yet.", px + 16, py + 68, 0xFF888888);
        }
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, "- " + all.get(start + i), px + 16, py + 68 + i * 22, 0xFFFFFFFF);
        }
        String info = "Page " + (page + 1) + "/" + pages + "   Friends: " + all.size();
        ctx.drawTextWithShadow(textRenderer, info, px + (PW - textRenderer.getWidth(info)) / 2, py + 203 - 0, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
