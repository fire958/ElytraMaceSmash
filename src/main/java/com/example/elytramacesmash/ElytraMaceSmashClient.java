package com.example.elytramacesmash;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ElytraMaceSmashClient implements ClientModInitializer {
    public enum Mode { PLAYERS, MOBS }

    private static final double RANGE = 96.0;

    private static KeyBinding playerKey, mobKey, guiKey;
    private static boolean active = false;
    private static Mode mode = Mode.PLAYERS;
    private static LivingEntity target = null;
    private static boolean diving = false;
    private static boolean warnedNoElytra = false;
    private static boolean warnedNoMace = false;
    private static int fireworkCooldown = 0;
    private static int smashFlash = 0;
    private static String status = "IDLE";

    @Override
    public void onInitializeClient() {
        Friends.load();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("elytramacesmash", "main"));
        playerKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.elytramacesmash.players", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, cat));
        mobKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.elytramacesmash.mobs", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, cat));
        guiKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.elytramacesmash.friends", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, cat));
        ClientTickEvents.END_CLIENT_TICK.register(ElytraMaceSmashClient::tick);
        Hud.register();
        TitleHooks.register();
    }

    public static boolean isActive() { return active; }
    public static LivingEntity getTarget() { return target; }
    public static Mode getMode() { return mode; }
    public static String getStatus() { return status; }

    private static void msg(MinecraftClient mc, String s, Formatting color) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[ElytraMaceSmash] " + s).formatted(color), true);
        }
    }

    private static void stop(MinecraftClient mc, String why) {
        active = false;
        target = null;
        diving = false;
        status = "IDLE";
        msg(mc, why, Formatting.YELLOW);
    }

    private static void toggle(MinecraftClient mc, ClientPlayerEntity p, Mode m) {
        if (active && mode == m) {
            stop(mc, "Lock-on disabled");
            return;
        }
        mode = m;
        diving = false;
        target = findTarget(mc, p, m);
        if (target == null) {
            active = false;
            msg(mc, "No " + (m == Mode.PLAYERS ? "players" : "mobs") + " found nearby", Formatting.RED);
            return;
        }
        active = true;
        msg(mc, "Target locked: " + target.getName().getString(), Formatting.GREEN);
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            active = false; target = null; diving = false; status = "IDLE";
            return;
        }

        while (guiKey.wasPressed()) mc.setScreen(new FriendsScreen(null));
        while (playerKey.wasPressed()) toggle(mc, p, Mode.PLAYERS);
        while (mobKey.wasPressed()) toggle(mc, p, Mode.MOBS);

        if (smashFlash > 0) smashFlash--;
        if (fireworkCooldown > 0) fireworkCooldown--;
        if (!active || mc.currentScreen != null) return;

        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            if (!warnedNoElytra) { msg(mc, "Equip an Elytra first!", Formatting.RED); warnedNoElytra = true; }
            status = "NO ELYTRA";
            return;
        }
        warnedNoElytra = false;

        if (!valid(target)) {
            LivingEntity next = findTarget(mc, p, mode);
            if (next == null) { stop(mc, "All targets eliminated - lock-on disabled"); return; }
            target = next;
            diving = false;
            msg(mc, "Next target: " + target.getName().getString(), Formatting.AQUA);
        }

        // Take off
        if (!p.isGliding()) {
            status = "TAKING OFF";
            if (p.isOnGround()) {
                p.jump();
            } else if (p.getVelocity().y < 0) {
                p.startGliding();
                p.networkHandler.sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        double dist = p.distanceTo(target);
        double dy = p.getY() - target.getY();

        // Climb high, then dive onto the target for a mace smash
        if (!diving && dy >= 10 && dist > 4) diving = true;
        if (diving && dy < -2) diving = false;

        Vec3d eye = target.getEyePos();
        Vec3d aim = diving ? eye : eye.add(0, 16, 0);
        Vec3d d = aim.subtract(p.getEyePos());
        double hor = Math.sqrt(d.x * d.x + d.z * d.z);
        float wantYaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(d.y, hor));

        // Smooth turning
        float step = 35f;
        p.setYaw(p.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(wantYaw - p.getYaw()), -step, step));
        p.setPitch(MathHelper.clamp(
                p.getPitch() + MathHelper.clamp(wantPitch - p.getPitch(), -step, step), -90f, 90f));

        // Firework boost
        double speed = p.getVelocity().length();
        if (p.isGliding() && fireworkCooldown == 0 && speed < 1.6 && !(diving && dist < 10)) {
            int fw = findHotbar(p, Items.FIREWORK_ROCKET);
            if (fw != -1) {
                p.getInventory().setSelectedSlot(fw);
                mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                fireworkCooldown = 22;
            }
        }

        // Mace attack
        int mace = findHotbar(p, Items.MACE);
        if (mace == -1 && !warnedNoMace) {
            msg(mc, "Put a Mace in your hotbar!", Formatting.RED);
            warnedNoMace = true;
        }
        if (mace != -1) warnedNoMace = false;

        if (dist <= 3.5) {
            if (mace != -1) {
                p.getInventory().setSelectedSlot(mace);
                if (p.getAttackCooldownProgress(0.5f) >= 0.95f) {
                    mc.interactionManager.attackEntity(p, target);
                    p.swingHand(Hand.MAIN_HAND);
                    smashFlash = 15;
                }
            }
            diving = false;
        } else if (diving && dist < 12 && mace != -1) {
            p.getInventory().setSelectedSlot(mace);
        }

        if (!p.isGliding()) status = "TAKING OFF";
        else if (smashFlash > 0) status = "SMASH!";
        else if (diving) status = "DIVING";
        else status = "CLIMBING";
    }

    private static boolean valid(LivingEntity e) {
        if (e == null || !e.isAlive() || e.isRemoved() || e.getHealth() <= 0) return false;
        if (mode == Mode.PLAYERS && e instanceof PlayerEntity pl && Friends.isFriend(pl.getName().getString())) return false;
        return true;
    }

    private static boolean isMobTarget(LivingEntity e) {
        if (e instanceof Monster) return true;
        return "mannequin".equals(Registries.ENTITY_TYPE.getId(e.getType()).getPath());
    }

    private static LivingEntity findTarget(MinecraftClient mc, ClientPlayerEntity p, Mode m) {
        LivingEntity best = null;
        double bestD = RANGE * RANGE;
        if (m == Mode.PLAYERS) {
            for (PlayerEntity o : mc.world.getPlayers()) {
                if (o == p || !o.isAlive() || o.isSpectator()) continue;
                if (Friends.isFriend(o.getName().getString())) continue;
                double dd = p.squaredDistanceTo(o);
                if (dd < bestD) { bestD = dd; best = o; }
            }
        } else {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity le)) continue;
                if (e instanceof PlayerEntity || e instanceof ArmorStandEntity) continue;
                if (!le.isAlive() || !isMobTarget(le)) continue;
                double dd = p.squaredDistanceTo(le);
                if (dd < bestD) { bestD = dd; best = le; }
            }
        }
        return best;
    }

    private static int findHotbar(ClientPlayerEntity p, Item item) {
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }
}
