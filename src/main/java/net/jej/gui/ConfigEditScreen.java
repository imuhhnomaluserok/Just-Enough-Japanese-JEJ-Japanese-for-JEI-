package net.jej.gui;

import net.jej.util.ChaosText;
import net.jej.util.ConfigWalker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 1つのModConfig（あるカテゴリ階層）の項目を一覧表示し、値を編集できる画面。
 * 項目数が多いMODでも壊れないよう、スクロールの代わりにページ送り方式にしています。
 *
 * バリデーションに失敗した時のメッセージ、および保存確認ダイアログの文言は
 * JEJConfig の chaosMode 設定によって切り替わります（ChaosTextユーティリティ参照）。
 */
public class ConfigEditScreen extends Screen {

    private static final int PER_PAGE = 6;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_START_Y = 46;

    private final Screen parent;
    private final ModConfig modConfig;
    private final List<String> categoryPath;

    private List<ConfigWalker.Entry> entries;
    private int page = 0;

    private Component statusMessage = Component.empty();
    private boolean statusIsError = false;
    private boolean dirty = false;

    private final List<RowLabel> rowLabels = new ArrayList<>();

    public ConfigEditScreen(Screen parent, ModConfig modConfig, List<String> categoryPath) {
        super(titleFor(modConfig, categoryPath));
        this.parent = parent;
        this.modConfig = modConfig;
        this.categoryPath = categoryPath;
    }

    private static Component titleFor(ModConfig modConfig, List<String> categoryPath) {
        String suffix = categoryPath.isEmpty() ? "" : " > " + String.join(" > ", categoryPath);
        return Component.translatable("jej.edit.title", modConfig.getModId() + suffix);
    }

    @Override
    protected void init() {
        this.entries = ConfigWalker.listDirectChildren(modConfig, categoryPath);
        buildPage();
    }

    private void buildPage() {
        this.clearWidgets();
        this.rowLabels.clear();

        int start = page * PER_PAGE;
        int end = Math.min(entries.size(), start + PER_PAGE);
        int y = ROW_START_Y;

        for (int i = start; i < end; i++) {
            ConfigWalker.Entry entry = entries.get(i);
            addRowFor(entry, y);
            y += ROW_HEIGHT;
        }

        // ページ送り
        if (page > 0) {
            this.addRenderableWidget(Button.builder(Component.translatable("jej.button.prevPage"),
                            b -> { page--; buildPage(); })
                    .bounds(20, this.height - 56, 80, 20).build());
        }
        if (end < entries.size()) {
            this.addRenderableWidget(Button.builder(Component.translatable("jej.button.nextPage"),
                            b -> { page++; buildPage(); })
                    .bounds(this.width - 100, this.height - 56, 80, 20).build());
        }

        // 戻る / 保存
        this.addRenderableWidget(Button.builder(Component.translatable("jej.button.back"), b -> this.onClose())
                .bounds(this.width / 2 - 165, this.height - 28, 160, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("jej.button.save"), b -> onSavePressed())
                .bounds(this.width / 2 + 5, this.height - 28, 160, 20).build());
    }

    private void addRowFor(ConfigWalker.Entry entry, int y) {
        String labelText = entry.lastName();
        rowLabels.add(new RowLabel(Component.literal(labelText), y));

        if (entry.isCategory) {
            this.addRenderableWidget(Button.builder(Component.translatable("jej.edit.openCategory"), b -> {
                        List<String> next = new ArrayList<>(entry.path);
                        this.minecraft.setScreen(new ConfigEditScreen(this, modConfig, next));
                    })
                    .bounds(this.width - 220, y - 2, 200, 20)
                    .build());
            return;
        }

        Object current = ConfigWalker.getCurrentValue(modConfig, entry);
        Class<?> clazz = entry.valueSpec.getClazz();

        if (clazz == Boolean.class) {
            addBooleanControl(entry, current, y);
        } else if (clazz != null && Enum.class.isAssignableFrom(clazz)) {
            addEnumControl(entry, current, clazz, y);
        } else {
            addTextControl(entry, current, y);
        }
    }

    private void addBooleanControl(ConfigWalker.Entry entry, Object current, int y) {
        boolean value = Boolean.TRUE.equals(current);
        Button[] holder = new Button[1];
        holder[0] = Button.builder(labelForBoolean(value), b -> {
                    boolean newValue = !labelIsTrue(holder[0]);
                    if (applyValue(entry, newValue)) {
                        holder[0].setMessage(labelForBoolean(newValue));
                    }
                })
                .bounds(this.width - 220, y - 2, 200, 20)
                .build();
        this.addRenderableWidget(holder[0]);
    }

    private boolean labelIsTrue(Button button) {
        return button.getMessage().getString().equals(
                Component.translatable("jej.value.true").getString());
    }

    private Component labelForBoolean(boolean value) {
        return Component.translatable(value ? "jej.value.true" : "jej.value.false");
    }

    private void addEnumControl(ConfigWalker.Entry entry, Object current, Class<?> clazz, int y) {
        Object[] constants = clazz.getEnumConstants();
        String currentName = current == null ? "?" : current.toString();

        Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.literal(currentName), b -> {
                    String shown = holder[0].getMessage().getString();
                    int idx = 0;
                    for (int i = 0; i < constants.length; i++) {
                        if (((Enum<?>) constants[i]).name().equalsIgnoreCase(shown)) {
                            idx = i;
                            break;
                        }
                    }
                    int nextIdx = (idx + 1) % constants.length;
                    String nextName = ((Enum<?>) constants[nextIdx]).name();
                    if (applyValue(entry, nextName)) {
                        holder[0].setMessage(Component.literal(nextName));
                    }
                })
                .bounds(this.width - 220, y - 2, 200, 20)
                .build();
        this.addRenderableWidget(holder[0]);
    }

    private void addTextControl(ConfigWalker.Entry entry, Object current, int y) {
        EditBox box = new EditBox(this.font, this.width - 220, y - 2, 150, 20, Component.empty());
        box.setValue(current == null ? "" : String.valueOf(current));
        box.setMaxLength(4096);
        this.addRenderableWidget(box);

        this.addRenderableWidget(Button.builder(Component.translatable("jej.button.apply"), b -> {
                    Object parsed = parseForClazz(box.getValue(), entry.valueSpec.getClazz());
                    if (parsed == null) {
                        showError(ChaosText.of("jej.error.parse_failed"));
                        return;
                    }
                    applyValue(entry, parsed);
                })
                .bounds(this.width - 60, y - 2, 50, 20)
                .build());
    }

    private Object parseForClazz(String text, Class<?> clazz) {
        try {
            if (clazz == Integer.class) return Integer.parseInt(text.trim());
            if (clazz == Long.class) return Long.parseLong(text.trim());
            if (clazz == Double.class) return Double.parseDouble(text.trim());
            if (clazz == String.class) return text;
            // 型が不明な場合は文字列としてそのまま渡し、ValueSpec#test() の判定に委ねる
            return text;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** entry に newValue を適用する。null は明示的に弾き、Chaosモード対応メッセージを出す。 */
    private boolean applyValue(ConfigWalker.Entry entry, Object newValue) {
        if (newValue == null) {
            showError(ChaosText.of("jej.error.null_value"));
            return false;
        }

        boolean ok = ConfigWalker.trySetValue(modConfig, entry, newValue);
        if (!ok) {
            showError(ChaosText.of("jej.error.invalid_value"));
            return false;
        }

        dirty = true;
        statusIsError = false;
        statusMessage = Component.translatable("jej.edit.changed");
        return true;
    }

    private void showError(Component message) {
        statusIsError = true;
        statusMessage = message;
    }

    private void onSavePressed() {
        if (!dirty) {
            showError(ChaosText.of("jej.error.nothing_to_save"));
            return;
        }
        this.minecraft.setScreen(new SaveConfirmScreen(this, () -> {
            ConfigWalker.save(modConfig);
            dirty = false;
            statusIsError = false;
            statusMessage = Component.translatable("jej.edit.saved");
        }));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);

        gfx.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        for (RowLabel rl : rowLabels) {
            gfx.drawString(this.font, rl.label, 20, rl.y + 4, 0xE0E0E0);
        }

        if (!statusMessage.getString().isEmpty()) {
            int color = statusIsError ? 0xFF5555 : 0x55FF55;
            gfx.drawCenteredString(this.font, statusMessage, this.width / 2, this.height - 74, color);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static final class RowLabel {
        final Component label;
        final int y;

        RowLabel(Component label, int y) {
            this.label = label;
            this.y = y;
        }
    }
}
