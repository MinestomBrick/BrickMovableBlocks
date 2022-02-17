package org.minestombrick.movablestructures.api;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.entity.metadata.other.BoatMeta;
import net.minestom.server.entity.metadata.other.FallingBlockMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import org.minestombrick.schematics.api.RegionBlock;
import org.minestombrick.schematics.api.Schematic;

import java.util.*;

public class MovableStructure {

    private final static double BOAT_Y_OFFSET = -.3;

    protected final Instance instance;
    protected Point position;

    protected final Set<Stack> stacks = new HashSet<>();

    public MovableStructure(Instance instance, Point position) {
        this.instance = instance;
        this.position = position;
    }

    public void remove() {
        stacks.forEach(Stack::remove);
        stacks.clear();
    }

    public void teleport(Point position) {
        this.position = position;
        // calculate

        for (Stack stack : stacks) {
            Pos absolute = new Pos(position.add(stack.origin()).add(0, BOAT_Y_OFFSET, 0));
            if (!instance.isChunkLoaded(absolute)) {
                instance.loadChunk(absolute).join();
            }
            stack.boat().refreshPosition(absolute);
        }
    }

    public void load(Schematic schematic) {
        Collection<RegionBlock> blocks = schematic.blocks();
        for (int x = 0; x < schematic.width(); x++) {
            for (int z = 0; z < schematic.length(); z++) {
                final int finalX = x, finalZ = z;
                List<RegionBlock> column = blocks.stream()
                        .filter(b -> b.relativePosition().x() == finalX && b.relativePosition().z() == finalZ)
                        .filter(b -> !b.blockState().isAir())
                        .filter(b -> !b.blockState().isLiquid())
                        .sorted(Comparator.comparingDouble(b -> b.relativePosition().y()))
                        .toList();

                if (column.isEmpty()) continue;

                int prevY = column.get(0).relativePosition().blockY();
                int startIndex = 0;
                for (int i = 1; i < column.size(); i++) {
                    RegionBlock b = column.get(i);
                    if (b.relativePosition().blockY() != prevY + 1) {
                        addStack(new Vec(x, column.get(startIndex).relativePosition().y(), z), column.subList(startIndex, i).stream()
                                .map(RegionBlock::blockState).toArray(Block[]::new));
                        startIndex = i;
                    }

                    prevY = b.relativePosition().blockY();
                }

                // top part
                addStack(new Vec(x, column.get(startIndex).relativePosition().y(), z), column.subList(startIndex, column.size()).stream()
                        .map(RegionBlock::blockState).toArray(Block[]::new));
            }
        }
    }

    public void addStack(Point origin, Block... blocks) {
        Point absolute = position.add(origin);
        if (!instance.isChunkLoaded(absolute)) {
            instance.loadChunk(absolute).join();
        }

        Entity boat = spawnBoat(absolute);
        Set<Entity> passengers = new HashSet<>();

        Entity previous = boat;
        for (Block block : blocks) {
            // normal blocks
            if (!block.registry().isBlockEntity()) {
                Entity pufferfish = spawnPufferfish(absolute);
                previous.addPassenger(pufferfish);
                passengers.add(pufferfish);

                Entity fallingBlock = spawnBlock(absolute, block);
                pufferfish.addPassenger(fallingBlock);
                passengers.add(fallingBlock);
                previous = fallingBlock;
                continue;
            }

            // TODO block tiles
        }

        Stack stack = new Stack(origin, boat, passengers);
        stacks.add(stack);
    }

    //

    protected Entity spawnBoat(Point position) {
        Entity boat = new Entity(EntityType.BOAT);
        BoatMeta meta = (BoatMeta) boat.getEntityMeta();
        meta.setType(BoatMeta.Type.ACACIA);
        boat.setNoGravity(true);
        boat.setInvisible(true);
        boat.addEffect(new Potion(PotionEffect.INVISIBILITY, (byte) 1, Integer.MAX_VALUE));
        boat.setInstance(instance, position);
        return boat;
    }

    protected Entity spawnBlock(Point position, Block block) {
        Entity fallingBlock = new Entity(EntityType.FALLING_BLOCK);
        FallingBlockMeta meta = (FallingBlockMeta) fallingBlock.getEntityMeta();
        meta.setBlock(block);
        fallingBlock.setNoGravity(true);
        fallingBlock.setInstance(instance, position);
        return fallingBlock;
    }

    protected Entity spawnPufferfish(Point position) {
        Entity pufferfish = new Entity(EntityType.PUFFERFISH);
        pufferfish.setInvisible(true);
        pufferfish.addEffect(new Potion(PotionEffect.INVISIBILITY, (byte) 1, Integer.MAX_VALUE));
        pufferfish.setNoGravity(true);
        pufferfish.setInstance(instance, position);
        return pufferfish;
    }

    //

    static class Stack {

        private final Point origin;

        private final Entity boat;
        private final Set<Entity> passengers = new HashSet<>();

        private Stack(Point origin, Entity boat, Set<Entity> passengers) {
            this.origin = origin;
            this.boat = boat;
            this.passengers.addAll(passengers);
        }

        public Entity boat() {
            return boat;
        }

        public Point origin() {
            return origin;
        }

        public Set<Entity> passengers() {
            return passengers;
        }

        private void remove() {
            passengers.forEach(Entity::remove);
            boat.remove();
        }
    }

}
