package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {

    static boolean enabled = false;
    static KeyMapping toggleKey;

    static final double RANGE = 3.2;
    static final float SMOOTH = 0.6f;
    static final float MAX_STEP = 45f;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aimassist.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aimassist", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.displayClientMessage(
                            Component.literal("AimAssist: " + (enabled ? "ON" : "OFF")), true);
                }
            }

            if (!enabled || client.player == null || client.level == null
                    || client.screen != null) return;

            LocalPlayer me = client.player;

            Player target = null;
            double best = RANGE;
            for (Player p : client.level.players()) {
                if (p == me || !p.isAlive() || p.isSpectator()) continue;
                double dist = me.distanceTo(p);
                if (dist <= best) { best = dist; target = p; }
            }
            if (target == null) return;

            Vec3 aim = target.getBoundingBox().getCenter();
            Vec3 d = aim.subtract(me.getEyePosition());

            float tYaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
            float tPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));

            float dYaw = Mth.wrapDegrees(tYaw - me.getYRot());
            float dPitch = tPitch - me.getXRot();

            me.setYRot(me.getYRot() + Mth.clamp(dYaw * SMOOTH, -MAX_STEP, MAX_STEP));
            me.setXRot(Mth.clamp(
                    me.getXRot() + Mth.clamp(dPitch * SMOOTH, -MAX_STEP, MAX_STEP),
                    -90f, 90f));
        });
    }
}
