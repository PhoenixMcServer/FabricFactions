package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;

public class WarzoneCommand implements Command {

    private int addForced(CommandContext<ServerCommandSource> context, int size) {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        Faction faction = Faction.getByName("Warzone");
        String dimension = world.getRegistryKey().getValue().toString();
        ArrayList<ChunkPos> chunks = new ArrayList<ChunkPos>();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos = world.getChunk(player.getBlockPos().add(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null) {
                    if (size == 1) {
                        String owner = existingClaim.getFaction().getID() == faction.getID() ? "Your" : "Another";
                        new Message(owner + " faction already owns this chunk").fail().send(player, false);
                        return 0;
                    } else if (existingClaim.getFaction().getID() != faction.getID()) {
                        new Message("Another faction already owns a chunk").fail().send(player, false);
                        return 0;
                    }
                }

                chunks.add(chunkPos);
            }
        }

        chunks.forEach(chunk -> faction.addClaim(chunk.x, chunk.z, dimension));
        if (size == 1) {
            new Message("Chunk (%d, %d) claimed by %s", chunks.get(0).x, chunks.get(0).z, player.getName().getString())
                    .send(player, false);
        } else {
            new Message("Chunks (%d, %d) to (%d, %d) claimed by %s", chunks.get(0).x, chunks.get(0).z,
                    chunks.get(0).x + size - 1, chunks.get(0).z + size - 1, player.getName().getString())
                    .send(player, false);
        }

        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = Faction.getByName("Warzone");

        return addForced(context, 1);
    }

    private int addSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = Faction.getByName("Warzone");

        return addForced(context, size);
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (existingClaim == null) {
            new Message("Cannot remove a claim on an unclaimed chunk").fail().send(player, false);
            return 0;
        }

        Faction faction = Faction.getByName("Warzone");

        if (existingClaim.getFaction().getID() != faction.getID()) {
            new Message("Cannot remove a claim owned by another faction").fail().send(player, false);
            return 0;
        }

        existingClaim.remove();
        new Message("Claim (%d, %d) removed by %s", existingClaim.x, existingClaim.z, player.getName().getString()).send(faction);
        return 1;
    }

    private int removeSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        Faction faction = Faction.getByName("Warzone");

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos = world.getChunk(player.getBlockPos().add(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null && (existingClaim.getFaction().getID() == faction.getID())) {
                    existingClaim.remove();
                }
            }
        }

        ChunkPos chunkPos = world.getChunk(player.getBlockPos().add((-size + 1) * 16, 0, (-size + 1) * 16)).getPos();
        new Message("Claims (%d, %d) to (%d, %d) removed by %s ", chunkPos.x, chunkPos.z,
            chunkPos.x + size - 1, chunkPos.z + size - 1, player.getName().getString())
            .send(player, false);

        return 1;
    }

    private int removeAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName("Warzone");
        faction.removeAllClaims();

        new Message("All claims removed by %s", player.getName().getString()).send(faction);
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("warzone")
            .requires(Requires.hasPerms("factions.admin.warzone", 0))
            .then(CommandManager.literal("claim")
                .then(CommandManager.literal("add")
                    .requires(Requires.hasPerms("factions.claim.add", 0))
                    .then(
                        CommandManager.argument("size", IntegerArgumentType.integer(1, 7))
                            .requires(Requires.hasPerms("factions.claim.add.size", 0))
                            .then(
                                CommandManager.literal("force")
                                    .requires(Requires.hasPerms("factions.claim.add.force", 0))
                                    .executes(context -> addForced(context, IntegerArgumentType.getInteger(context, "size")))
                            )
                            .executes(this::addSize)
                    )
                    .executes(this::add)
                )
                .then(
                    CommandManager.literal("remove")
                        .requires(Requires.hasPerms("factions.claim.remove", 0))
                        .then(
                            CommandManager.argument("size", IntegerArgumentType.integer(1, 7))
                                .requires(Requires.hasPerms("factions.claim.remove.size", 0))
                                .executes(this::removeSize)
                        )
                        .then(
                            CommandManager.literal("all")
                                .requires(Requires.hasPerms("factions.claim.remove.all", 0))
                                .executes(this::removeAll)
                        )
                        .executes(this::remove)
                ))
            .build();
    }
}
