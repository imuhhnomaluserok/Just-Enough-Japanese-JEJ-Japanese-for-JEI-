package net.jej;

import com.mojang.blaze3d.platform.InputConstants;
import net.jej.config.JEJConfig;
import net.jej.gui.ConfigHubScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.lwjgl.glfw.GLFW;

/**
 * JEJ - Just Enough Japanese Config
 *
 * JEIをはじめとする、ForgeConfigSpecを使っているあらゆるMODの設定を、
 * 完全日本語のGUIから編集できるようにするMODです。
 * おまけでイースターエッグの「Chaosモード」も入っています(JEJ自身の設定でON/OFF可能)。
 */
@Mod(JEJMod.MOD_ID)
public class JEJMod {

    public static final String MOD_ID = "jej";

    public static KeyMapping OPEN_HUB_KEY;

    public JEJMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        JEJConfig.register();

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerKeyMappings);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // JEJ自身の「Config」ボタン(Modリスト上)からもハブ画面を開けるようにする。
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parentScreen) -> new ConfigHubScreen(parentScreen))
        );
    }

    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        OPEN_HUB_KEY = new KeyMapping(
                "key.jej.open_hub",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // デフォルトでは未割り当て。「コントロール > キー割り当て」から設定してください。
                "key.categories.jej"
        );
        event.register(OPEN_HUB_KEY);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (OPEN_HUB_KEY == null) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        while (OPEN_HUB_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ConfigHubScreen(null));
            }
        }
    }
}
