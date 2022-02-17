package com.gufli.brickmovingblocks.app;

import com.gufli.brickmovingblocks.app.schematic.SpongeSchematic;
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

import java.util.*;

public class MovableStructure {

    protected final Instance instance;
    protected Point position;

    protected final Set<Stack> stacks = new HashSet<>();

    public MovableStructure(Instance instance, Point position) {
        this.instance = instance;
        this.position = position;
    }

    public void teleport(Point position) {
        this.position = position;
        // calculate

        for (Stack stack : stacks) {
            Pos absolute = new Pos(position.add(stack.origin()));
            if (!instance.isChunkLoaded(absolute)) {
                instance.loadChunk(absolute).join();
            }
            stack.boat().refreshPosition(absolute);
        }
    }

    public void load(SpongeSchematic schematic) {
        List<SpongeSchematic.RegionBlock> blocks = schematic.blocks();
        for (int x = 0; x < schematic.width(); x++) {
            for (int z = 0; z < schematic.length(); z++) {
                final int finalX = x, finalZ = z;
                List<SpongeSchematic.RegionBlock> column = blocks.stream()
                        .filter(b -> b.position().x() == finalX && b.position().z() == finalZ)
                        .filter(b -> !b.blockState().isAir())
                        .sorted(Comparator.comparingDouble(b -> b.position().y()))
                        .toList();

                if (column.isEmpty()) continue;

                int prevY = column.get(0).position().blockY();
                int startIndex = 0;
                for ( int i = 0; i < column.size(); i++ ) {
                    SpongeSchematic.RegionBlock b = column.get(i);

                    if ( b.position().blockY() != prevY + 1 ) {
                        addStack(new Vec(x, column.get(startIndex).position().y(), z), column.subList(startIndex, i).stream()
                                .map(SpongeSchematic.RegionBlock::blockState).toArray(Block[]::new));
                        startIndex = i;
                    }

                    prevY = b.position().blockY();
                }

                // top part
                addStack(new Vec(x, column.get(startIndex).position().y(), z), column.subList(startIndex, column.size()).stream()
                        .map(SpongeSchematic.RegionBlock::blockState).toArray(Block[]::new));
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
            if ( !block.registry().isBlockEntity() ) {
                Entity pufferfish = spawnPufferfish(absolute);
                previous.addPassenger(pufferfish);
                passengers.add(pufferfish);

                Entity fallingBlock = spawnBlock(absolute, block);
                pufferfish.addPassenger(fallingBlock);
                passengers.add(fallingBlock);
                previous = fallingBlock;
            } else {
                if ( block.registry().material() == null ) {
                    continue;
                }

                Entity armorStand = spawnArmorStand(absolute, block);
                previous.addPassenger(armorStand);
                passengers.add(armorStand);
                previous = armorStand;
            }
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

    protected Entity spawnArmorStand(Point position, Block block) {
        LivingEntity armorStand = new LivingEntity(EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setHelmet(ItemStack.fromNBT(block.registry().material(), block.nbt()));
        armorStand.setNoGravity(true);
        armorStand.setInstance(instance, position);
        ArmorStandMeta meta = (ArmorStandMeta) armorStand.getEntityMeta();

        System.out.println(block.name());

        if ( block.getProperty("facing") != null ) {
            FacingDirection direction = FacingDirection.valueOf(block.getProperty("facing").toUpperCase());
            meta.setHeadRotation(direction.vec());
        }

        return armorStand;
    }

    enum FacingDirection {
        SOUTH(0, 0, 0),
        EAST(0, -90, 0),
        NORTH(0, 180, 0),
        WEST(0, 90, 0),
        UP(0, 0, 90),
        DOWN(0, 0, -90);

        final float roll;
        final float yaw;
        final float pitch;

        FacingDirection(float roll, float yaw, float pitch) {
            this.roll = roll;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        Vec vec() {
            return new Vec(roll, yaw, pitch);
        }
    }

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
    }

}
