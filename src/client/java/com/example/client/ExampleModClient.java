package com.example.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {

    static boolean enabled = false;
    static KeyBinding toggleKey;

    static final double RANGE = 3.2;
    static final float SMOOTH = 0.6f;
    static final float MAX_STEP = 45f;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aimassist.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyBinding.Category.create(Identifier.of("aimassist", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("AimAssist: " + (enabled ? "ON" : "OFF")), true);
                }
            }

            if (!enabled || client.player == null || client.world == null
                    || client.currentScreen != null) return;

            ClientPlayerEntity me = client.player;

            PlayerEntity target = null;
            double best = RANGE;
            for (PlayerEntity p : client.world.getPlayers()) {
                if (p == me || !p.isAlive() || p.isSpectator()) continue;
                double dist = me.distanceTo(p);
                if (dist <= best) { best = dist; target = p; }
            }
            if (target == null) return;

            Vec3d aim = target.getBoundingBox().getCenter();
            Vec3d d = aim.subtract(me.getEyePos());

            float tYaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
            float tPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));

            float dYaw = MathHelper.wrapDegrees(tYaw - me.getYaw());
            float dPitch = tPitch - me.getPitch();

            me.setYaw(me.getYaw() + MathHelper.clamp(dYaw * SMOOTH, -MAX_STEP, MAX_STEP));
            me.setPitch(MathHelper.clamp(
                    me.getPitch() + MathHelper.clamp(dPitch * SMOOTH, -MAX_STEP, MAX_STEP),
                    -90f, 90f));
        });
    }
}
