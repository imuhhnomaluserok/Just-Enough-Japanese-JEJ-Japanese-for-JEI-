package net.jej.util;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 他MODも含めた ForgeConfigSpec の中身を、リフレクションなしで読み書きするための補助クラスです。
 *
 * 仕組み:
 *  - ForgeConfigSpec#getSpec() が「項目の定義情報」(ValueSpec)のツリーを持っている
 *  - ModConfig#getConfigData() が「実際に今読み込まれている値」(CommentedConfig)を持っている
 *  - 両方を同じパス("category.subcategory.key")で突き合わせて読み書きする
 *
 * 注意: Forgeのバージョンによって内部APIが微妙に変わることがあります。
 * 1.20.1 / Forge 47.x を前提に書いていますが、もしビルドエラーが出たら
 * このファイル周辺のメソッド名を疑ってください（getSpec/getConfigData/ValueSpecのAPIなど）。
 */
public final class ConfigWalker {

    private ConfigWalker() {}

    public static final class Entry {
        public final List<String> path;
        public final boolean isCategory;
        public final ForgeConfigSpec.ValueSpec valueSpec; // isCategory=false のときのみ非null

        Entry(List<String> path, boolean isCategory, ForgeConfigSpec.ValueSpec valueSpec) {
            this.path = path;
            this.isCategory = isCategory;
            this.valueSpec = valueSpec;
        }

        public String pathString() {
            return String.join(".", path);
        }

        public String lastName() {
            return path.get(path.size() - 1);
        }
    }

/** 指定したカテゴリ直下の項目一覧を返す（サブカテゴリの中身は再帰しない。1階層だけ）。 */
    public static List<Entry> listDirectChildren(ModConfig modConfig, List<String> categoryPath) {
        List<Entry> out = new ArrayList<>();

        // 1. modConfig から getSpec() を取得して型チェック
        Object specObj = modConfig.getSpec();
        if (!(specObj instanceof com.electronwill.nightconfig.core.UnmodifiableConfig)) {
            return out;
        }
        com.electronwill.nightconfig.core.UnmodifiableConfig specRoot = 
            (com.electronwill.nightconfig.core.UnmodifiableConfig) specObj;

        // 2. 指定されたカテゴリノードを取得
        com.electronwill.nightconfig.core.UnmodifiableConfig node = categoryPath.isEmpty()
                ? specRoot
                : specRoot.get(String.join(".", categoryPath));
        if (node == null) return out;
        
        // 3. var を使って複雑なジェネリクス型を自動推論させる
        for (var e : node.entrySet()) {
            List<String> childPath = new ArrayList<>(categoryPath);
            childPath.add(e.getKey());

            Object raw = e.getValue();
            // ForgeConfigSpec.ValueSpec への安全なキャスト
            if (raw instanceof net.minecraftforge.common.ForgeConfigSpec.ValueSpec valueSpec) {
                out.add(new Entry(childPath, false, valueSpec));
            } else if (raw instanceof com.electronwill.nightconfig.core.UnmodifiableConfig) {
                out.add(new Entry(childPath, true, null));
            }
        }
        return out;
    }
    
    /** 現在の実値を取得する。値が見つからなければデフォルト値を返す。 */
    public static Object getCurrentValue(ModConfig modConfig, Entry entry) {
        CommentedConfig data = modConfig.getConfigData();
        if (data == null) return entry.valueSpec.getDefault();
        Object v = data.get(entry.pathString());
        return v == null ? entry.valueSpec.getDefault() : v;
    }

    /**
     * 値の変更を試みる。ForgeConfigSpec.ValueSpec#test() によるバリデーションに失敗した場合は
     * 何も書き換えずに false を返す（＝呼び出し側でエラーメッセージを出す）。
     */
    public static boolean trySetValue(ModConfig modConfig, Entry entry, Object newValue) {
        if (entry.isCategory) return false;
        if (!entry.valueSpec.test(newValue)) return false;

        CommentedConfig data = modConfig.getConfigData();
        if (data == null) return false;

        data.set(entry.pathString(), newValue);
        return true;
    }

    /** ディスクに保存し、そのMODに再読み込みイベントを飛ばす。 */
    public static void save(ModConfig modConfig) {
        modConfig.save();
    }
}
