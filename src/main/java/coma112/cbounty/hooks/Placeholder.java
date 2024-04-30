package coma112.cbounty.hooks;

import coma112.cbounty.CBounty;
import coma112.cbounty.language.MessageKeys;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Placeholder extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "cb";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Coma112";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(@NotNull Player player, @NotNull String params) {
        if (params.equals("isbounty")) {
            return CBounty.getDatabaseManager().isBounty(player) ? MessageKeys.YES : MessageKeys.NO;
        }

        if (params.startsWith("top_")) {
            try {
                int pos = Integer.parseInt(params.split("_")[1]);

                if (CBounty.getDatabaseManager().getTopStreakPlayer(pos) != null) return CBounty.getDatabaseManager().getTopStreakPlayer(pos);
                return "---";
            } catch (Exception exception) {
                return "";
            }
        }

        if (params.startsWith("topstreak_")) {
            try {
                int pos = Integer.parseInt(params.split("_")[1]);

                if (CBounty.getDatabaseManager().getTopStreak(pos) != 0) return String.valueOf(CBounty.getDatabaseManager().getTopStreak(pos));
                return "---";
            } catch (Exception exception) {
                return "";
            }
        }

        return null;
    }
}
