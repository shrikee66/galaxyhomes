package com.galaxyhomes.tpa;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.config.ConfigManager;
import com.galaxyhomes.data.PlayerData;
import com.galaxyhomes.data.PlayerDataManager;
import com.galaxyhomes.utils.MessageUtils;
import com.galaxyhomes.utils.SoundUtils;
import com.galaxyhomes.utils.GeyserUtils;
import com.galaxyhomes.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TPAManager {

    private final GalaxyHomes plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    private final Map<UUID, TPARequest> outgoingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, List<TPARequest>> incomingRequests = new ConcurrentHashMap<>();
    private final BukkitRunnable cleanupTask;

    public TPAManager(GalaxyHomes plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();

        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredRequests();
            }
        };
        this.cleanupTask.runTaskTimer(plugin, 20L, 20L);
    }

    public boolean sendTPARequest(Player sender, Player target) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (senderId.equals(targetId)) {
            MessageUtils.sendMessage(sender, "cannot-target-self");
            return false;
        }

        if (outgoingRequests.containsKey(senderId)) {
            TPARequest existing = outgoingRequests.get(senderId);
            Player existingTarget = Bukkit.getPlayer(existing.getTarget());
            String name = existingTarget != null ? existingTarget.getName() : "Unknown";
            MessageUtils.sendMessage(sender, "tpa-already-pending", Map.of("player", name));
            return false;
        }

        if (outgoingRequests.size() >= configManager.getMaxOutgoingRequests()) {
            MessageUtils.sendMessage(sender, "tpa-already-pending", Map.of("player", "limit reached"));
            return false;
        }

        PlayerData targetData = playerDataManager.getPlayerData(targetId);
        if (targetData.getTPAData().isToggle() || targetData.getTPAData().isBlocked(senderId)) {
            MessageUtils.sendMessage(sender, "tpa-target-blocked", Map.of("player", target.getName()));
            return false;
        }

        List<TPARequest> incoming = incomingRequests.get(targetId);
        if (incoming != null && incoming.size() >= configManager.getMaxIncomingRequests()) {
            MessageUtils.sendMessage(sender, "tpa-target-has-pending", Map.of("player", target.getName()));
            return false;
        }

        TPARequest request = new TPARequest(
                senderId, targetId, TPARequest.Type.TPA,
                configManager.getTPAExpireSeconds()
        );

        outgoingRequests.put(senderId, request);
        incomingRequests.computeIfAbsent(targetId, k -> new ArrayList<>()).add(request);

        MessageUtils.sendMessage(sender, "tpa-sent", Map.of(
                "player", target.getName(),
                "time", String.valueOf(configManager.getTPAExpireSeconds())
        ));
        SoundUtils.playSound(sender, configManager.getSoundConfig("tpa-sent"));

        if (target.isOnline()) {
            MessageUtils.sendMessage(target, "tpa-received", Map.of(
                    "player", sender.getName(),
                    "time", String.valueOf(configManager.getTPAExpireSeconds())
            ));
            SoundUtils.playSound(target, configManager.getSoundConfig("tpa-received"));
            showIncomingRequestForm(target, request);
        }

        return true;
    }

    public boolean sendTPAHereRequest(Player sender, Player target) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (senderId.equals(targetId)) {
            MessageUtils.sendMessage(sender, "cannot-target-self");
            return false;
        }

        if (outgoingRequests.containsKey(senderId)) {
            TPARequest existing = outgoingRequests.get(senderId);
            Player existingTarget = Bukkit.getPlayer(existing.getTarget());
            String name = existingTarget != null ? existingTarget.getName() : "Unknown";
            MessageUtils.sendMessage(sender, "tpa-already-pending", Map.of("player", name));
            return false;
        }

        if (outgoingRequests.size() >= configManager.getMaxOutgoingRequests()) {
            MessageUtils.sendMessage(sender, "tpa-already-pending", Map.of("player", "limit reached"));
            return false;
        }

        PlayerData targetData = playerDataManager.getPlayerData(targetId);
        if (targetData.getTPAData().isToggle() || targetData.getTPAData().isBlocked(senderId)) {
            MessageUtils.sendMessage(sender, "tpa-target-blocked", Map.of("player", target.getName()));
            return false;
        }

        List<TPARequest> incoming = incomingRequests.get(targetId);
        if (incoming != null && incoming.size() >= configManager.getMaxIncomingRequests()) {
            MessageUtils.sendMessage(sender, "tpa-target-has-pending", Map.of("player", target.getName()));
            return false;
        }

        TPARequest request = new TPARequest(
                senderId, targetId, TPARequest.Type.TPAHERE,
                configManager.getTPAExpireSeconds()
        );

        outgoingRequests.put(senderId, request);
        incomingRequests.computeIfAbsent(targetId, k -> new ArrayList<>()).add(request);

        MessageUtils.sendMessage(sender, "tpahere-sent", Map.of(
                "player", target.getName(),
                "time", String.valueOf(configManager.getTPAExpireSeconds())
        ));
        SoundUtils.playSound(sender, configManager.getSoundConfig("tpahere-sent"));

        if (target.isOnline()) {
            MessageUtils.sendMessage(target, "tpahere-received", Map.of(
                    "player", sender.getName(),
                    "time", String.valueOf(configManager.getTPAExpireSeconds())
            ));
            SoundUtils.playSound(target, configManager.getSoundConfig("tpahere-received"));
            showIncomingRequestForm(target, request);
        }

        return true;
    }

    public boolean acceptTPARequest(Player target, UUID senderId) {
        List<TPARequest> incoming = incomingRequests.get(target.getUniqueId());
        if (incoming == null || incoming.isEmpty()) {
            MessageUtils.sendMessage(target, "tpa-no-pending-requests");
            return false;
        }

        for (TPARequest request : incoming) {
            if (request.getSender().equals(senderId)) {
                return processAcceptRequest(target, request);
            }
        }

        MessageUtils.sendMessage(target, "tpa-no-pending-requests");
        return false;
    }

    public boolean acceptTPARequest(Player target) {
        List<TPARequest> incoming = incomingRequests.get(target.getUniqueId());
        if (incoming == null || incoming.isEmpty()) {
            MessageUtils.sendMessage(target, "tpa-no-pending-requests");
            return false;
        }

        return processAcceptRequest(target, incoming.get(incoming.size() - 1));
    }

    private boolean processAcceptRequest(Player target, TPARequest request) {
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null || !sender.isOnline()) {
            MessageUtils.sendMessage(target, "tpa-player-offline");
            removeRequest(request);
            return false;
        }

        if (request.getType() == TPARequest.Type.TPA) {
            if (!TeleportUtils.teleport(sender, target.getLocation(), "tpa-teleporting",
                    Map.of("player", target.getName()))) {
                MessageUtils.sendMessage(target, "tpa-teleport-failed");
                return false;
            }

            MessageUtils.sendMessage(target, "tpa-accepted", Map.of("player", sender.getName()));
            MessageUtils.sendMessage(sender, "tpa-accepted-target", Map.of("player", target.getName()));
        } else {
            if (!TeleportUtils.teleport(target, sender.getLocation(), "tpahere-teleporting",
                    Map.of("player", sender.getName()))) {
                MessageUtils.sendMessage(target, "tpa-teleport-failed");
                return false;
            }

            MessageUtils.sendMessage(target, "tpahere-accepted", Map.of("player", sender.getName()));
            MessageUtils.sendMessage(sender, "tpahere-accepted-target", Map.of("player", target.getName()));
        }

        SoundUtils.playSound(sender, configManager.getSoundConfig("tpa-accept"));
        SoundUtils.playSound(target, configManager.getSoundConfig("tpa-accept"));
        removeRequest(request);
        return true;
    }

    public boolean denyTPARequest(Player target, UUID senderId) {
        List<TPARequest> incoming = incomingRequests.get(target.getUniqueId());
        if (incoming == null || incoming.isEmpty()) {
            MessageUtils.sendMessage(target, "tpa-no-pending-requests");
            return false;
        }

        for (TPARequest request : incoming) {
            if (request.getSender().equals(senderId)) {
                return processDenyRequest(target, request);
            }
        }

        MessageUtils.sendMessage(target, "tpa-no-pending-requests");
        return false;
    }

    public boolean denyTPARequest(Player target) {
        List<TPARequest> incoming = incomingRequests.get(target.getUniqueId());
        if (incoming == null || incoming.isEmpty()) {
            MessageUtils.sendMessage(target, "tpa-no-pending-requests");
            return false;
        }

        return processDenyRequest(target, incoming.get(incoming.size() - 1));
    }

    private boolean processDenyRequest(Player target, TPARequest request) {
        Player sender = Bukkit.getPlayer(request.getSender());

        if (sender != null && sender.isOnline()) {
            MessageUtils.sendMessage(sender, "tpa-denied", Map.of("player", target.getName()));
            SoundUtils.playSound(sender, configManager.getSoundConfig("tpa-deny"));
        }

        MessageUtils.sendMessage(target, "tpa-denied-target",
                Map.of("player", sender != null ? sender.getName() : "Unknown"));
        SoundUtils.playSound(target, configManager.getSoundConfig("tpa-deny"));

        removeRequest(request);
        return true;
    }

    public void cancelTPARequest(Player sender) {
        TPARequest request = outgoingRequests.remove(sender.getUniqueId());
        if (request == null) return;

        List<TPARequest> incoming = incomingRequests.get(request.getTarget());
        if (incoming != null) {
            incoming.remove(request);
            if (incoming.isEmpty()) incomingRequests.remove(request.getTarget());
        }

        Player target = Bukkit.getPlayer(request.getTarget());
        if (target != null && target.isOnline()) {
            MessageUtils.sendMessage(target, "tpa-cancelled", Map.of("player", sender.getName()));
            SoundUtils.playSound(target, configManager.getSoundConfig("tpa-cancel"));
        }

        MessageUtils.sendMessage(sender, "tpa-cancelled-target");
        SoundUtils.playSound(sender, configManager.getSoundConfig("tpa-cancel"));
    }

    private void removeRequest(TPARequest request) {
        outgoingRequests.remove(request.getSender());

        List<TPARequest> incoming = incomingRequests.get(request.getTarget());
        if (incoming != null) {
            incoming.remove(request);
            if (incoming.isEmpty()) incomingRequests.remove(request.getTarget());
        }
    }

    public void expireRequest(TPARequest request) {
        removeRequest(request);

        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null && sender.isOnline()) {
            MessageUtils.sendMessage(sender, "tpa-expired", Map.of(
                    "player", Bukkit.getOfflinePlayer(request.getTarget()).getName()
            ));
            SoundUtils.playSound(sender, configManager.getSoundConfig("tpa-expire"));
        }

        Player target = Bukkit.getPlayer(request.getTarget());
        if (target != null && target.isOnline()) {
            MessageUtils.sendMessage(target, "tpa-expired-target", Map.of(
                    "player", Bukkit.getOfflinePlayer(request.getSender()).getName()
            ));
            SoundUtils.playSound(target, configManager.getSoundConfig("tpa-expire"));
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();

        TPARequest outgoing = outgoingRequests.remove(playerId);
        if (outgoing != null) {
            List<TPARequest> incoming = incomingRequests.get(outgoing.getTarget());
            if (incoming != null) {
                incoming.remove(outgoing);
                if (incoming.isEmpty()) incomingRequests.remove(outgoing.getTarget());
            }
        }

        List<TPARequest> incoming = incomingRequests.remove(playerId);
        if (incoming != null) {
            for (TPARequest request : incoming) {
                outgoingRequests.remove(request.getSender());

                Player sender = Bukkit.getPlayer(request.getSender());
                if (sender != null && sender.isOnline()) {
                    MessageUtils.sendMessage(sender, "tpa-target-offline", Map.of("player", player.getName()));
                    SoundUtils.playSound(sender, configManager.getSoundConfig("tpa-expire"));
                }
            }
        }
    }

    public List<TPARequest> getIncomingRequests(UUID playerId) {
        return incomingRequests.getOrDefault(playerId, Collections.emptyList());
    }

    public TPARequest getOutgoingRequest(UUID playerId) {
        return outgoingRequests.get(playerId);
    }

    public boolean hasOutgoingRequest(UUID playerId) {
        return outgoingRequests.containsKey(playerId);
    }

    public boolean hasIncomingRequests(UUID playerId) {
        List<TPARequest> incoming = incomingRequests.get(playerId);
        return incoming != null && !incoming.isEmpty();
    }

    private void showIncomingRequestForm(Player target, TPARequest request) {
        if (!GeyserUtils.isBedrockPlayer(target)) return;

        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null) return;

        String message = request.getType() == TPARequest.Type.TPA
                ? MessageUtils.getMessage("bedrock-tpa-request", Map.of(
                        "player", sender.getName(),
                        "time", String.valueOf(configManager.getTPAExpireSeconds())
                ))
                : MessageUtils.getMessage("bedrock-tpahere-request", Map.of(
                        "player", sender.getName(),
                        "time", String.valueOf(configManager.getTPAExpireSeconds())
                ));

        GeyserUtils.sendForm(target, "tpa-request-" + request.getSender(),
                "Teleport Request", message,
                List.of(
                        new GeyserUtils.Button("Accept", "accept"),
                        new GeyserUtils.Button("Deny", "deny")
                ),
                (player, response) -> {
                    if ("accept".equals(response)) {
                        acceptTPARequest(player, request.getSender());
                    } else if ("deny".equals(response)) {
                        denyTPARequest(player, request.getSender());
                    }
                }
        );
    }

    private void cleanupExpiredRequests() {
        long now = System.currentTimeMillis();
        for (TPARequest request : new ArrayList<>(outgoingRequests.values())) {
            if (request.getExpiresAt() <= now) expireRequest(request);
        }
    }

    public int getActiveRequestCount() {
        return outgoingRequests.size();
    }

    public void cleanup() {
        cleanupTask.cancel();
        outgoingRequests.clear();
        incomingRequests.clear();
    }
                                                                              }
