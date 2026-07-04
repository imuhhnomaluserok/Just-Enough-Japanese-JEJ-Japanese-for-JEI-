package net.jej.gui;

import net.jej.config.JEJConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JEJのメイン画面。「JEIなどのMODコンフィグを日本語で編集する」ハブです。
 * ForgeConfigSpecを登録している全MODを一覧表示し、選ぶとそのMODの設定編集画面(ConfigEditScreen)に飛びます。
 */
public class ConfigHubScreen extends Screen {

    private final Screen parent;
    private ModListWidget list;

    public ConfigHubScreen(Screen parent) {
        super(Component.translatable("jej.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = new ModListWidget(this.minecraft, this.width, this.height, 40, this.height - 40, 24);
        this.addWidget(this.list);
        this.populateList();

        this.addRenderableWidget(Button.builder(Component.translatable("jej.button.back"), b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private void populateList() {
        boolean showModId = JEJConfig.SHOW_MOD_ID.get();

        // ConfigTracker.INSTANCE.configSets() は Type(COMMON/CLIENT/SERVER/STARTUP) 毎に
        // 登録済みの ModConfig 集合を返す。ここでは全種類まとめて一覧にする。
        Map<ModConfig.Type, Set<ModConfig>> allConfigs = ConfigTracker.INSTANCE.configSets();

        List<ModConfig> configs = new ArrayList<>();
        for (Set<ModConfig> set : allConfigs.values()) {
            configs.addAll(set);
        }
        configs.sort((a, b) -> a.getModId().compareToIgnoreCase(b.getModId()));

        for (ModConfig config : configs) {
            String modId = config.getModId();
            String displayName = ModList.get().getModContainerById(modId)
                    .map(c -> c.getModInfo().getDisplayName())
                    .orElse(modId);

            String label = showModId
                    ? displayName + "  [" + modId + " / " + config.getType().name() + "]"
                    : displayName + "  (" + config.getType().name() + ")";

            this.list.children().add(this.list.new ModEntry(Component.literal(label), config));
        }

        if (configs.isEmpty()) {
            this.list.children().add(this.list.new ModEntry(
                    Component.translatable("jej.hub.empty"), null));
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        this.list.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    /** MOD一覧を表示するリストウィジェット。 */
    public class ModListWidget extends ObjectSelectionList<ModListWidget.ModEntry> {

        public ModListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight) {
            super(mc, width, height, y0, y1, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + this.getRowWidth() / 2 + 10;
        }

        public class ModEntry extends ObjectSelectionList.Entry<ModListWidget.ModEntry> {
            private final Component label;
            private final ModConfig config;

            public ModEntry(Component label, ModConfig config) {
                this.label = label;
                this.config = config;
            }

            @Override
            public void render(GuiGraphics gfx, int index, int top, int left, int width, int height,
                                int mouseX, int mouseY, boolean hovering, float partialTick) {
                gfx.drawString(ConfigHubScreen.this.font, this.label, left + 4, top + height / 2 - 4, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (config != null) {
                    Minecraft.getInstance().setScreen(new ConfigEditScreen(ConfigHubScreen.this, config, new ArrayList<>()));
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return this.label;
            }
        }
    }
}
