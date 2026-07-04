package net.jej.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * JEJ自身の設定です。ここに「Chaosモード」のON/OFFを持たせています。
 * 他のMODの設定はJEJの中には保存しません（それぞれのMOD自身のconfigファイルを直接読み書きします）。
 */
public class JEJConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CHAOS_MODE;
    public static final ForgeConfigSpec.BooleanValue SHOW_MOD_ID;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");

        CHAOS_MODE = builder
                .comment(
                        "trueにすると「Chaosモード」が有効になります。",
                        "設定変更時のエラーメッセージや確認ダイアログの文言が、賑やかで砕けた言い回しに変わります。",
                        "機能自体は通常モードと完全に同じで、あくまで表示テキストだけが変わるイースターエッグです。"
                )
                .translation("jej.config.chaosMode")
                .define("chaosMode", false);

        SHOW_MOD_ID = builder
                .comment("設定ハブ画面のMOD一覧に、MOD名だけでなくMOD IDも表示するかどうかです。")
                .translation("jej.config.showModId")
                .define("showModId", true);

        builder.pop();

        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "jej-client.toml");
    }

    public static boolean isChaosMode() {
        try {
            return CHAOS_MODE.get();
        } catch (IllegalStateException e) {
            // configロード前に呼ばれた場合の保険。
            return false;
        }
    }
}
