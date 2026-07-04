package net.jej.util;

import net.jej.config.JEJConfig;
import net.minecraft.network.chat.Component;

/**
 * lang/ja_jp.json 側で「通常キー」と「通常キー + .chaos」のペアを用意しておき、
 * JEJConfig.isChaosMode() の値に応じてどちらを使うか自動で切り替えます。
 *
 * 例:
 *   "jej.error.null_value": "Nullはその値に対応していません。他の値に変更してください。"
 *   "jej.error.null_value.chaos": "ちょーっと待って！そこにはNullを入れれないよ！対応する他の値を入力してね！"
 */
public final class ChaosText {

    private ChaosText() {}

    /** 通常キーの翻訳、Chaosモード中は "キー.chaos" があればそちらを優先して返す。 */
    public static Component of(String baseKey, Object... args) {
        String key = JEJConfig.isChaosMode() ? baseKey + ".chaos" : baseKey;
        return Component.translatable(key, args);
    }

    /** ボタンのラベルなど、常にどちらかの固定文言を選びたい場合用。 */
    public static Component pick(Component normal, Component chaos) {
        return JEJConfig.isChaosMode() ? chaos : normal;
    }
}
