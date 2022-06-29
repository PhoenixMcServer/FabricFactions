package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class WorldManager {
    private static Map<UUID, String> _lastUserVisits = new HashMap<>();

    public static void register() {
        PlayerEvents.ON_MOVE.register(WorldManager::onMove);
        MiscEvents.ON_MOB_SPAWN_ATTEMPT.register(WorldManager::onMobSpawnAttempt);
    }

    private static void onMobSpawnAttempt() {
        // TODO Implement this
    }
 
    private static void onMove(ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        ServerWorld world = player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        // handle autoclaim system
        if (user.autoclaim && claim == null) {
            Faction faction = user.getFaction();
            int requiredPower = (faction.getClaims().size() + 1) * FactionsMod.CONFIG.CLAIM_WEIGHT;
            int maxPower = faction.getUsers().size() * FactionsMod.CONFIG.MEMBER_POWER + FactionsMod.CONFIG.BASE_POWER;

            if (maxPower < requiredPower) {
                new Message("Not enough faction power to claim chunk, autoclaim toggled off").fail().send(player, false);
                user.autoclaim = false;
            } else {
                faction.addClaim(chunkPos.x, chunkPos.z, dimension);
                claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
                new Message("Chunk (%d, %d) claimed by %s", chunkPos.x, chunkPos.z, player.getName().getString())
                    .send(faction);
            }
        }

        // radar system
        if (FactionsMod.CONFIG.RADAR && user.radar) {
            String factionName = claim == null ? "Wilderness" : claim.getFaction().getName();
            String subtitle = claim == null ? FactionsMod.CONFIG.WildernessDescription : claim.getFaction().getDescription();
            Formatting color = claim == null ? Formatting.GREEN : claim.getFaction().getColor();

            if (_lastUserVisits.containsKey(player.getUuid()) && _lastUserVisits.get(player.getUuid()) == factionName) {
                return;
            }

            _lastUserVisits.put(player.getUuid(), factionName);

            world.getServer().getCommandManager().execute(world.getServer().getCommandSource().withSilent(), "title " + player.getCommandSource().getName() + " subtitle \"" + subtitle + "\"");
            world.getServer().getCommandManager().execute(world.getServer().getCommandSource().withSilent(), "title " + player.getCommandSource().getName() + " title {\"text\":\"" + factionName + "\",\"color\":\"" + color.asString() + "\"}");
        }
    }
}
