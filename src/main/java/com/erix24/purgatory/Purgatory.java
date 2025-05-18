package com.erix24.purgatory;


import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


import java.util.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Purgatory.MOD_ID)
public class Purgatory {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "purgatory";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace

    public Purgatory(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }


    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientModEvents {
        // Player Dies
        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            Entity entity = event.getEntity();
            MinecraftServer minecraftServer = entity.level().getServer();
            if (minecraftServer != null) {
                if (!minecraftServer.isHardcore()) {
                    // Check if the entity that died is a player
                    if (entity instanceof Player) {
                        ServerPlayer player = (ServerPlayer) entity;
                        player.setLastDeathLocation(Optional.of(GlobalPos.of(event.getEntity().level().dimension(), event.getEntity().blockPosition())));

                        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD, 1);

                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("SkullOwner", player.getName().getString());

                        playerHead.setTag(nbt);

                        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX() + 0.5, player.getY(), player.getZ() + 0.5, playerHead));
                    }
                }
            }

        }

        // Player Respawns
        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            Entity entity = event.getEntity();
            MinecraftServer minecraftServer = entity.level().getServer();
            if (minecraftServer != null) {
                if (!minecraftServer.isHardcore()) {
                    Player player = event.getEntity();
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    serverPlayer.setGameMode(GameType.SPECTATOR);

                    player.teleportTo(player.getLastDeathLocation().get().pos().getX() + 0.5,
                            player.getLastDeathLocation().get().pos().getY(),
                            player.getLastDeathLocation().get().pos().getZ() + 0.5);

                }
            }
        }

        @SubscribeEvent
        public static void playerReincarnation(PlayerInteractEvent.RightClickBlock event) {
            Entity entity = event.getEntity();
            Level level = event.getLevel();
            MinecraftServer minecraftServer = entity.level().getServer();
            if (minecraftServer != null) {
                if (!minecraftServer.isHardcore()) {
                    Block targetedBlock = level.getBlockState(event.getPos()).getBlock();
                    Player player = event.getEntity();
                    if (targetedBlock == Blocks.PLAYER_HEAD || targetedBlock == Blocks.PLAYER_WALL_HEAD) {
                        CompoundTag nbt = level.getBlockEntity(event.getPos()).saveWithoutMetadata();
                        CompoundTag skullOwner = (CompoundTag) nbt.get("SkullOwner");
                        if (skullOwner != null) {
                            String targetedPlayerName = skullOwner.get("Name").getAsString();
                            ServerPlayer targetedPlayer = minecraftServer.getPlayerList().getPlayerByName(targetedPlayerName);

                            if (targetedPlayer != null && !player.getName().getString().equals(targetedPlayerName)) {  //  [Add back later]
                                if (targetedPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) { // Change back the GameType.SPECTATOR later
                                    if (player.getMainHandItem().getItem() == Items.GOLDEN_APPLE) {
                                        player.getMainHandItem().shrink(1);
                                        if (level.getBlockState(new BlockPos(event.getPos().getX(), event.getPos().getY() + 1, event.getPos().getZ())).isAir()) {
                                            targetedPlayer.teleportTo(event.getPos().getX() + 0.5, event.getPos().getY(), event.getPos().getZ() + 0.5);
                                        } else {
                                            targetedPlayer.teleportTo(event.getPos().getX() + 0.5, event.getPos().getY() - 1, event.getPos().getZ() + 0.5);
                                        }
                                        targetedPlayer.level().removeBlock(event.getPos(), true);
                                        targetedPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, 3));
                                        targetedPlayer.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 20));
                                        targetedPlayer.getFoodData().setFoodLevel(3 * 2);
                                        targetedPlayer.setGameMode(GameType.SURVIVAL);
                                        ServerLevel serverLevel = (ServerLevel) level;
                                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                                targetedPlayer.getX(), targetedPlayer.getY() + 1.5, targetedPlayer.getZ(),
                                                30, (Math.random() * 2) - 1,
                                                (Math.random() * 2) - 1, (Math.random() * 2) - 1, 0);

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void createPlayerHead(PlayerInteractEvent.RightClickBlock event) {
            Entity entity = event.getEntity();
            Level level = event.getLevel();
            MinecraftServer minecraftServer = entity.level().getServer();
            if (minecraftServer != null) {
                if (!minecraftServer.isHardcore()) {
                    Block targetedBlock = level.getBlockState(event.getPos()).getBlock();
                    Player player = event.getEntity();
                    if (targetedBlock == Blocks.SKELETON_SKULL || targetedBlock == Blocks.SKELETON_WALL_SKULL) {
                        String targetedPlayerNameWithBrackets = player.getMainHandItem().getDisplayName().getString();
                        String targetedPlayerName = targetedPlayerNameWithBrackets.substring(1,targetedPlayerNameWithBrackets.length()-1);

                        ItemStack playerHeadItem = new ItemStack(Blocks.PLAYER_HEAD.asItem());

                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("SkullOwner", targetedPlayerName);
                        playerHeadItem.setTag(nbt);

                        level.removeBlock(event.getPos(), true);

                        BlockPos eventPos = event.getPos();
                        level.addFreshEntity(new ItemEntity(player.level(), eventPos.getX() + 0.5, eventPos.getY(), eventPos.getZ() + 0.5, playerHeadItem));
                    }
                }
            }
        }
    }
}
