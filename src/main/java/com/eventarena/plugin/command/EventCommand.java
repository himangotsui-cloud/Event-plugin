package com.eventarena.plugin.command;

import com.eventarena.plugin.EventArenaPlugin;
import com.eventarena.plugin.gui.EventGui;
import com.eventarena.plugin.manager.EventManager;
import com.eventarena.plugin.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "make", "announce", "join", "leave", "start", "stop", "status", "border", "drop", "revive"
    );

    private final EventArenaPlugin plugin;

    public EventCommand(EventArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            handleBareCommand(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "make" -> handleMake(sender);
            case "announce" -> handleAnnounce(sender, args);
            case "join" -> handleJoin(sender);
            case "leave" -> handleLeave(sender);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "status" -> showStatus(sender);
            case "border" -> handleBorder(sender, args);
            case "drop" -> handleDrop(sender);
            case "revive" -> handleRevive(sender, args);
            default -> sender.sendMessage(ColorUtil.legacy(
                    "&cUnknown subcommand. /event [make|announce|join|leave|start|stop|status|border|drop|revive]"));
        }
        return true;
    }

    /**
     * Bare "/event" with no arguments: if there's a joinable event and you're
     * not in it yet, this just joins you - no need to type "/event join".
     * Otherwise it falls back to showing status.
     */
    private void handleBareCommand(CommandSender sender) {
        EventManager em = plugin.eventManager();
        if (sender instanceof Player player
                && em.hasEvent()
                && !em.isParticipant(player.getUniqueId())
                && em.state() == com.eventarena.plugin.state.EventState.WAITING) {
            handleJoin(sender);
            return;
        }
        showStatus(sender);
    }

    private void handleMake(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().msg("player-only"));
            return;
        }
        if (!player.hasPermission("event.make")) {
            player.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (plugin.eventManager().hasEvent()) {
            player.sendMessage(plugin.configManager().msg("event-already-exists"));
            return;
        }
        player.openInventory(EventGui.build());
    }

    private void handleAnnounce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("event.announce")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.legacy("&cUsage: /event announce <message>"));
            return;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        String titleText = plugin.configManager().config().getString("announce.title", "&c&lEVENT");
        int fadeIn = plugin.configManager().config().getInt("announce.fade-in-ticks", 10);
        int stay = plugin.configManager().config().getInt("announce.stay-ticks", 70);
        int fadeOut = plugin.configManager().config().getInt("announce.fade-out-ticks", 20);

        net.kyori.adventure.text.Component title = ColorUtil.component(titleText);
        net.kyori.adventure.text.Component subtitle = ColorUtil.component(message);
        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)
        );
        net.kyori.adventure.title.Title adventureTitle = net.kyori.adventure.title.Title.title(title, subtitle, times);

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.showTitle(adventureTitle);
        }
        sender.sendMessage(ColorUtil.legacy("&aAnnouncement sent."));
    }

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().msg("player-only"));
            return;
        }
        EventManager em = plugin.eventManager();
        EventManager.JoinResult result = em.join(player);
        switch (result) {
            case SUCCESS -> player.sendMessage(ColorUtil.placeholders(
                    plugin.configManager().msg("joined"),
                    "%current%", String.valueOf(em.totalCount()),
                    "%max%", String.valueOf(em.maxPlayers())));
            case NO_EVENT -> player.sendMessage(plugin.configManager().msg("event-not-created"));
            case ALREADY_JOINED -> player.sendMessage(plugin.configManager().msg("already-joined"));
            case FULL -> player.sendMessage(plugin.configManager().msg("event-full"));
            case ALREADY_STARTED -> player.sendMessage(plugin.configManager().msg("cannot-join-active"));
        }
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().msg("player-only"));
            return;
        }
        if (plugin.eventManager().leave(player)) {
            player.sendMessage(plugin.configManager().msg("left"));
        } else {
            player.sendMessage(plugin.configManager().msg("not-in-event"));
        }
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission("event.make")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (!plugin.eventManager().hasEvent()) {
            sender.sendMessage(plugin.configManager().msg("event-not-created"));
            return;
        }
        if (!plugin.eventManager().isCallerInEventWorld(sender)) {
            sender.sendMessage(plugin.configManager().msg("must-be-in-event"));
            return;
        }
        EventManager.StartResult result = plugin.eventManager().startEvent();
        switch (result) {
            case STARTING -> sender.sendMessage(ColorUtil.legacy("&aEvent starting..."));
            case NOT_ENOUGH_PLAYERS -> sender.sendMessage(ColorUtil.placeholders(
                    plugin.configManager().msg("not-enough-players"),
                    "%min%", String.valueOf(plugin.eventManager().minPlayers())));
            case WRONG_STATE -> sender.sendMessage(plugin.configManager().msg("event-not-created"));
        }
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("event.make")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (!plugin.eventManager().hasEvent()) {
            sender.sendMessage(plugin.configManager().msg("event-not-created"));
            return;
        }
        if (!plugin.eventManager().isCallerInEventWorld(sender)) {
            sender.sendMessage(plugin.configManager().msg("must-be-in-event"));
            return;
        }
        plugin.eventManager().stopEvent("event-stopped");
        sender.sendMessage(ColorUtil.legacy("&cEvent stopped."));
    }

    private void showStatus(CommandSender sender) {
        EventManager em = plugin.eventManager();
        if (!em.hasEvent()) {
            sender.sendMessage(plugin.configManager().msg("event-not-created"));
            return;
        }
        sender.sendMessage(plugin.configManager().rawMsg("status-header"));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-mode"),
                "%mode%", ColorUtil.legacy(em.mode().displayName())));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-players"),
                "%current%", String.valueOf(em.activeCount()), "%max%", String.valueOf(em.maxPlayers())));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-border"),
                "%size%", String.valueOf(plugin.borderManager().currentSize())));
        sender.sendMessage(ColorUtil.placeholders(plugin.configManager().rawMsg("status-state"),
                "%state%", em.state().name()));
    }

    private void handleBorder(CommandSender sender, String[] args) {
        if (!sender.hasPermission("event.border")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (!plugin.eventManager().isCallerInEventWorld(sender)) {
            sender.sendMessage(plugin.configManager().msg("must-be-in-event"));
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ColorUtil.legacy("&cUsage: /event border <size>"));
            return;
        }
        int size;
        try {
            size = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.legacy("&cSize must be a whole number."));
            return;
        }
        if (!plugin.borderManager().isValid(size)) {
            sender.sendMessage(ColorUtil.placeholders(
                    plugin.configManager().msg("border-invalid"),
                    "%min%", String.valueOf(plugin.borderManager().minSize()),
                    "%max%", String.valueOf(plugin.borderManager().maxSize())));
            return;
        }
        plugin.borderManager().setBorder(size);
        sender.sendMessage(ColorUtil.placeholders(
                plugin.configManager().msg("border-set"), "%size%", String.valueOf(size)));
    }

    private void handleDrop(CommandSender sender) {
        if (!sender.hasPermission("event.drop")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (!plugin.eventManager().isCallerInEventWorld(sender)) {
            sender.sendMessage(plugin.configManager().msg("must-be-in-event"));
            return;
        }
        if (plugin.worldManager().isDropInProgress()) {
            sender.sendMessage(ColorUtil.legacy("&eA drop is already in progress."));
            return;
        }
        sender.sendMessage(plugin.configManager().msg("drop-started"));
        plugin.worldManager().dropArena(() ->
                plugin.getServer().broadcastMessage(plugin.configManager().msg("drop-done")));
    }

    private void handleRevive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("event.revive")) {
            sender.sendMessage(plugin.configManager().msg("no-permission"));
            return;
        }
        if (!plugin.eventManager().isCallerInEventWorld(sender)) {
            sender.sendMessage(plugin.configManager().msg("must-be-in-event"));
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ColorUtil.legacy("&cUsage: /event revive <all|player>"));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int count = plugin.eventManager().reviveAll();
            sender.sendMessage(plugin.configManager().msg("revive-all-done"));
            sender.sendMessage(ColorUtil.legacy("&7(" + count + " player(s) revived)"));
            return;
        }
        Player target = org.bukkit.Bukkit.getPlayerExact(args[1]);
        if (target == null || !plugin.eventManager().isParticipant(target.getUniqueId())) {
            sender.sendMessage(plugin.configManager().msg("revive-not-found"));
            return;
        }
        if (!plugin.eventManager().reviveOne(target)) {
            sender.sendMessage(plugin.configManager().msg("revive-not-eliminated"));
            return;
        }
        sender.sendMessage(ColorUtil.legacy("&aRevived &f" + target.getName() + "&a."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("revive")) {
            List<String> options = new ArrayList<>();
            options.add("all");
            for (var uuid : plugin.eventManager().participantsView().keySet()) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) options.add(p.getName());
            }
            options.removeIf(s -> !s.toLowerCase().startsWith(args[1].toLowerCase()));
            return options;
        }
        return new ArrayList<>();
    }
}
