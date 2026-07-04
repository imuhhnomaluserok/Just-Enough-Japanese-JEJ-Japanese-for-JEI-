package net.jej.gui;

import net.jej.config.JEJConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「変更を保存します、よろしいですか？」の確認画面。
 * Chaosモードがオンだと、質問文とボタンのラベルが賑やかな言い回しに変わります。
 */
public class SaveConfirmScreen extends Screen {

    private final Screen parent;
    private final Runnable onConfirmSave;

    private Component message;

    public SaveConfirmScreen(Screen parent, Runnable onConfirmSave) {
        super(titleFor());
        this.parent = parent;
        this.onConfirmSave = onConfirmSave;
    }

    private static Component titleFor() {
        boolean chaos = JEJConfig.isChaosMode();
        return Component.translatable(chaos ? "jej.save.title.chaos" : "jej.save.title");
    }

    @Override
    protected void init() {
        boolean chaos = JEJConfig.isChaosMode();

        this.message = Component.translatable(chaos ? "jej.save.message.chaos" : "jej.save.message");

        Component confirmLabel = Component.translatable(chaos ? "jej.save.confirm.chaos" : "jej.save.confirm");
        Component cancelLabel = Component.translatable(chaos ? "jej.save.cancel.chaos" : "jej.save.cancel");

        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 20;

        this.addRenderableWidget(Button.builder(confirmLabel, b -> {
                    onConfirmSave.run();
                    this.onClose();
                })
                .bounds(centerX - 155, buttonY, 150, 20)
                .build());

        this.addRenderableWidget(Button.builder(cancelLabel, b -> this.onClose())
                .bounds(centerX + 5, buttonY, 150, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);

        int centerX = this.width / 2;
        gfx.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 40, 0xFFFFFF);

        gfx.drawWordWrap(this.font, this.message, centerX - 150, this.height / 2 - 15, 300, 0xCCCCCC);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
