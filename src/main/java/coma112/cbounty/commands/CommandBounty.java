package coma112.cbounty.commands;

import coma112.cbounty.CBounty;
import coma112.cbounty.database.AbstractDatabase;
import coma112.cbounty.enums.RewardType;
import coma112.cbounty.enums.keys.ConfigKeys;
import coma112.cbounty.enums.keys.MessageKeys;
import coma112.cbounty.events.BountyRemoveEvent;
import coma112.cbounty.hooks.PlayerPoints;
import coma112.cbounty.hooks.Vault;
import coma112.cbounty.item.IItemBuilder;
import coma112.cbounty.managers.Top;
import coma112.cbounty.menu.menus.BountiesMenu;
import coma112.cbounty.utils.MenuUtils;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("deprecation")
@Command({"bounty", "cbounty"})
public class CommandBounty {

    @Subcommand("reload")
    public void reload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("cbounty.reload") || !sender.hasPermission("cbounty.admin")) {
            sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
            return;
        }

        CBounty.getInstance().getLanguage().reload();
        CBounty.getInstance().getConfiguration().reload();
        CBounty.getDatabaseManager().reconnect();
        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }

    @Subcommand("streaktop")
    public void streaktop(@NotNull CommandSender sender, int value) {
        if (!sender.hasPermission("cbounty.streaktop") || !sender.hasPermission("cbounty.admin")) {
            sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
            return;
        }

        try {
            value = Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            sender.sendMessage(MessageKeys.INVALID_NUMBER.getMessage());
        }


        if (value <= 0) {
            sender.sendMessage(MessageKeys.NO_NEGATIVE.getMessage());
            return;
        }

        if (value > 15) {
            sender.sendMessage(MessageKeys.MAX_TOP
                    .getMessage()
                    .replace("{top}", String.valueOf(ConfigKeys.MAXIMUM_TOP.getInt())));
            return;
        }

        sender.sendMessage(Top.getTopStreak(value).toPlainText());
    }

    @Subcommand("menu")
    public void menu(@NotNull CommandSender sender) {
        if (!sender.hasPermission("cbounty.menu") || !sender.hasPermission("cbounty.admin")) {
            sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
            return;
        }

        if (!(sender instanceof @NotNull Player player)) {
            sender.sendMessage(MessageKeys.PLAYER_REQUIRED.getMessage());
            return;
        }

        new BountiesMenu(MenuUtils.getMenuUtils(player)).open();
    }

    @Subcommand("set")
    public void set(@NotNull CommandSender sender, @NotNull Player target, RewardType rewardType, int reward) {
        AbstractDatabase databaseManager = CBounty.getDatabaseManager();

        if (!sender.hasPermission("cbounty.set") || !sender.hasPermission("cbounty.admin")) {
            sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
            return;
        }

        if (!(sender instanceof @NotNull Player player)) {
            sender.sendMessage(MessageKeys.PLAYER_REQUIRED.getMessage());
            return;
        }

        if (!target.isOnline()) {
            player.sendMessage(MessageKeys.PLAYER_NOT_FOUND.getMessage());
            return;
        }

        if (target == player) {
            player.sendMessage(MessageKeys.CANT_BE_YOURSELF.getMessage());
            return;
        }

        if (databaseManager.isBounty(target)) {
            player.sendMessage(MessageKeys.ALREADY_BOUNTY.getMessage());
            return;
        }

        if (reward <= 0) {
            player.sendMessage(MessageKeys.NO_NEGATIVE.getMessage());
            return;
        }

        if (reward > ConfigKeys.MAX_REWARD_LIMIT.getInt()) {
            player.sendMessage(MessageKeys.MAX_REWARD_LIMIT
                    .getMessage()
                    .replace("{limit}", String.valueOf(ConfigKeys.MAX_REWARD_LIMIT.getInt())));
            return;
        }

        if (databaseManager.reachedMaximumBounty(player)) {
            player.sendMessage(MessageKeys.MAX_BOUNTY.getMessage());
            return;
        }

        boolean success = false;
        switch (rewardType) {
            case TOKEN -> success = handleTokenReward(player, reward);
            case MONEY -> success = handleMoneyReward(player, reward);
            case PLAYERPOINTS -> success = handlePlayerPointsReward(player, reward);
            case LEVEL -> success = handleLevelReward(player, reward);
        }

        if (success) {
            databaseManager.createBounty(player, target, rewardType, reward);
            player.sendMessage(MessageKeys.SUCCESSFUL_SET.getMessage());
        }
    }

    @Subcommand("remove")
            public void remove (@NotNull CommandSender sender, @NotNull Player target){
                if (!sender.hasPermission("cbounty.remove") || !sender.hasPermission("cbounty.admin")) {
                    sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
                    return;
                }

                if (!(sender instanceof @NotNull Player player)) {
                    sender.sendMessage(MessageKeys.PLAYER_REQUIRED.getMessage());
                    return;
                }

                if (!player.hasPermission("cbounty.remove") || !player.hasPermission("cbounty.admin")) {
                    player.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
                    return;
                }

                if (!target.isOnline()) {
                    player.sendMessage(MessageKeys.PLAYER_NOT_FOUND.getMessage());
                    return;
                }

                if (!CBounty.getDatabaseManager().isBounty(target)) {
                    player.sendMessage(MessageKeys.NOT_BOUNTY.getMessage());
                    return;
                }

                CBounty.getDatabaseManager().removeBounty(target);
                player.sendMessage(MessageKeys.REMOVE_PLAYER.getMessage());
                target.sendMessage(MessageKeys.REMOVE_TARGET.getMessage());
                CBounty.getInstance().getServer().getPluginManager().callEvent(new BountyRemoveEvent(player, target));
            }

            @Subcommand("raise")
            public void raise (@NotNull CommandSender sender, @NotNull Player target,int newReward){
                if (!sender.hasPermission("cbounty.raise") || !sender.hasPermission("cbounty.admin")) {
                    sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
                    return;
                }

                if (!(sender instanceof @NotNull Player player)) {
                    sender.sendMessage(MessageKeys.PLAYER_REQUIRED.getMessage());
                    return;
                }

                if (!target.isOnline()) {
                    player.sendMessage(MessageKeys.PLAYER_NOT_FOUND.getMessage());
                    return;
                }

                if (!CBounty.getDatabaseManager().isBounty(target)) {
                    player.sendMessage(MessageKeys.NOT_BOUNTY.getMessage());
                    return;
                }

                if (newReward <= 0) {
                    player.sendMessage(MessageKeys.NO_NEGATIVE.getMessage());
                    return;
                }

                if (newReward > ConfigKeys.MAX_REWARD_LIMIT.getInt()) {
                    player.sendMessage(MessageKeys.MAX_REWARD_LIMIT.getMessage());
                    return;
                }

                if (CBounty.getDatabaseManager().getSender(target) != player) {
                    player.sendMessage(MessageKeys.NOT_MATCHING_OWNERS.getMessage());
                    return;
                }

                int oldReward = CBounty.getDatabaseManager().getReward(target);

                CBounty.getDatabaseManager().changeReward(target, newReward);
                player.sendMessage(MessageKeys.PLAYER_RAISE.getMessage());
                target.sendMessage(MessageKeys.TARGET_RAISE
                        .getMessage()
                        .replace("{old}", String.valueOf(oldReward))
                        .replace("{new}", String.valueOf(oldReward + newReward)));
            }

            @Subcommand("takeoff")
            public void takeOff (@NotNull CommandSender sender, @NotNull Player target){
                if (!sender.hasPermission("cbounty.takeoff") || !sender.hasPermission("cbounty.admin")) {
                    sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
                    return;
                }

                if (!(sender instanceof @NotNull Player player)) {
                    sender.sendMessage(MessageKeys.PLAYER_REQUIRED.getMessage());
                    return;
                }

                if (!target.isOnline()) {
                    player.sendMessage(MessageKeys.PLAYER_NOT_FOUND.getMessage());
                    return;
                }

                if (!CBounty.getDatabaseManager().isBounty(target)) {
                    player.sendMessage(MessageKeys.NOT_BOUNTY.getMessage());
                    return;
                }

                if (CBounty.getDatabaseManager().getSender(target) != player) {
                    player.sendMessage(MessageKeys.NOT_MATCHING_OWNERS.getMessage());
                    return;
                }


                switch (CBounty.getDatabaseManager().getRewardType(target)) {
                    case TOKEN ->
                            CBounty.getTokenManager().addTokens(player, CBounty.getDatabaseManager().getReward(target));
                    case MONEY ->
                            Vault.getEconomy().depositPlayer(player, CBounty.getDatabaseManager().getReward(target));
                    case PLAYERPOINTS ->
                            CBounty.getPlayerPointsManager().give(player.getUniqueId(), CBounty.getDatabaseManager().getReward(target));
                    case LEVEL -> player.setLevel(player.getLevel() + CBounty.getDatabaseManager().getReward(target));
                }

                player.sendMessage(MessageKeys.SUCCESSFUL_TAKEOFF_PLAYER
                        .getMessage()
                        .replace("{target}", target.getName()));

                target.sendMessage(MessageKeys.SUCCESSFUL_TAKEOFF_TARGET
                        .getMessage()
                        .replace("{player}", player.getName()));
                CBounty.getDatabaseManager().removeBounty(target);
                CBounty.getInstance().getServer().getPluginManager().callEvent(new BountyRemoveEvent(player, target));
            }

            @Subcommand("bountyfinder")
            public void giveBountyFinder (@NotNull CommandSender sender, @NotNull @Default("me") Player target){
                if (!sender.hasPermission("cbounty.bountyfinder") || !sender.hasPermission("cbounty.admin")) {
                    sender.sendMessage(MessageKeys.NO_PERMISSION.getMessage());
                    return;
                }

                if (!(sender instanceof @NotNull Player player)) {
                    sender.sendMessage(MessageKeys.PLAYER_REQUIRED.getMessage());
                    return;
                }

                if (!target.isOnline()) {
                    player.sendMessage(MessageKeys.PLAYER_NOT_FOUND.getMessage());
                    return;
                }

                if (target.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(MessageKeys.FULL_INVENTORY.getMessage());
                    return;
                }

                target.getInventory().addItem(IItemBuilder.createItemFromSection("bountyfinder-item"));
    }

    private boolean handleTokenReward(@NotNull Player player, int reward) {
        if (CBounty.getInstance().getToken().getTokens(player) < reward) {
            player.sendMessage(MessageKeys.NOT_ENOUGH_TOKEN.getMessage());
            return false;
        } else {
            CBounty.getTokenManager().removeTokens(player, reward);
            return true;
        }
    }

    private boolean handleMoneyReward(@NotNull Player player, int reward) {
        Economy economy = Vault.getEconomy();
        if (economy.getBalance(player) < reward) {
            player.sendMessage(MessageKeys.NOT_ENOUGH_MONEY.getMessage());
            return false;
        } else {
            economy.withdrawPlayer(player, reward);
            return true;
        }
    }

    private boolean handlePlayerPointsReward(@NotNull Player player, int reward) {
        PlayerPointsAPI api = CBounty.getPlayerPointsManager();
        UUID uuid = player.getUniqueId();
        if (api.look(uuid) < reward) {
            player.sendMessage(MessageKeys.NOT_ENOUGH_PLAYERPOINTS.getMessage());
            return false;
        } else {
            api.take(uuid, reward);
            return true;
        }
    }

    private boolean handleLevelReward(@NotNull Player player, int reward) {
        if (player.getLevel() < reward) {
            player.sendMessage(MessageKeys.NOT_ENOUGH_LEVEL.getMessage());
            return false;
        } else {
            player.setLevel(player.getLevel() - reward);
            return true;
        }
    }
}
