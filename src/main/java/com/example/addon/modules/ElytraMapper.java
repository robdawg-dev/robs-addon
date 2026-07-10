package com.example.addon.modules;

import com.example.addon.RobsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraMapper extends Module {

    private final SettingGroup sgAltitude  = settings.createGroup("Altitude");
    private final SettingGroup sgDirection = settings.createGroup("Direction");
    private final SettingGroup sgRockets   = settings.createGroup("Rockets");
    private final SettingGroup sgPattern   = settings.createGroup("Pattern");
    private final SettingGroup sgLogout    = settings.createGroup("Logout");

    // ── Altitude ─────────────────────────────────────────────────────────────

    private final Setting<Double> cruiseAltitude = sgAltitude.add(new DoubleSetting.Builder()
        .name("cruise-altitude")
        .description("Y-level to cruise at after a rocket boost.")
        .defaultValue(1500)
        .min(100)
        .sliderRange(100, 5000)
        .build()
    );

    private final Setting<Double> boostAltitude = sgAltitude.add(new DoubleSetting.Builder()
        .name("boost-altitude")
        .description("Y-level at which to pitch up and fire rockets.")
        .defaultValue(400)
        .min(50)
        .sliderRange(50, 2000)
        .build()
    );

    private final Setting<Double> cruisePitch = sgAltitude.add(new DoubleSetting.Builder()
        .name("cruise-pitch")
        .description("Pitch while gliding at cruise altitude (negative = nose slightly up).")
        .defaultValue(-3.0)
        .range(-90, 90)
        .sliderRange(-30, 30)
        .build()
    );

    private final Setting<Double> boostPitch = sgAltitude.add(new DoubleSetting.Builder()
        .name("boost-pitch")
        .description("Pitch while climbing on rockets (negative = nose up).")
        .defaultValue(-45.0)
        .range(-90, 0)
        .sliderRange(-90, 0)
        .build()
    );

    // ── Direction ─────────────────────────────────────────────────────────────

    private final Setting<CardinalDir> laneDirection = sgDirection.add(new EnumSetting.Builder<CardinalDir>()
        .name("lane-direction")
        .description("Direction each survey lane runs.")
        .defaultValue(CardinalDir.NORTH)
        .build()
    );

    private final Setting<Boolean> lockYaw = sgDirection.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Lock yaw to the current flight direction.")
        .defaultValue(true)
        .build()
    );

    // ── Rockets ───────────────────────────────────────────────────────────────

    private final Setting<Boolean> autoRocket = sgRockets.add(new BoolSetting.Builder()
        .name("auto-rocket")
        .description("Automatically fire firework rockets from the hotbar while boosting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> rocketInterval = sgRockets.add(new IntSetting.Builder()
        .name("rocket-interval")
        .description("Seconds between rocket fires while boosting.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 20)
        .visible(autoRocket::get)
        .build()
    );

    // ── Pattern ───────────────────────────────────────────────────────────────

    private final Setting<Boolean> autoPattern = sgPattern.add(new BoolSetting.Builder()
        .name("auto-pattern")
        .description("Automatically shift to adjacent lanes for lawnmower-style area coverage.")
        .defaultValue(true)
        .build()
    );

    private final Setting<PatternMode> patternMode = sgPattern.add(new EnumSetting.Builder<PatternMode>()
        .name("pattern-mode")
        .description("Lane-Length: turn after a fixed distance. Bounding-Box: turn when reaching X/Z coordinate boundaries.")
        .defaultValue(PatternMode.LANE_LENGTH)
        .visible(autoPattern::get)
        .build()
    );

    private final Setting<Double> laneLength = sgPattern.add(new DoubleSetting.Builder()
        .name("lane-length")
        .description("Blocks to fly along each lane before turning around.")
        .defaultValue(1000)
        .min(50)
        .sliderRange(50, 5000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.LANE_LENGTH)
        .build()
    );

    private final Setting<Integer> boundMinX = sgPattern.add(new IntSetting.Builder()
        .name("bound-min-x")
        .description("Western boundary (minimum X). Lane turns around here when flying West.")
        .defaultValue(-500)
        .sliderRange(-10000, 10000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Integer> boundMaxX = sgPattern.add(new IntSetting.Builder()
        .name("bound-max-x")
        .description("Eastern boundary (maximum X). Lane turns around here when flying East.")
        .defaultValue(500)
        .sliderRange(-10000, 10000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Integer> boundMinZ = sgPattern.add(new IntSetting.Builder()
        .name("bound-min-z")
        .description("Northern boundary (minimum Z). Lane turns around here when flying North.")
        .defaultValue(-500)
        .sliderRange(-10000, 10000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Integer> boundMaxZ = sgPattern.add(new IntSetting.Builder()
        .name("bound-max-z")
        .description("Southern boundary (maximum Z). Lane turns around here when flying South.")
        .defaultValue(500)
        .sliderRange(-10000, 10000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Double> laneSpacing = sgPattern.add(new DoubleSetting.Builder()
        .name("lane-spacing")
        .description("Perpendicular distance between adjacent lanes in blocks.")
        .defaultValue(90)
        .min(10)
        .sliderRange(10, 500)
        .visible(autoPattern::get)
        .build()
    );

    private final Setting<SweepDir> sweepDirection = sgPattern.add(new EnumSetting.Builder<SweepDir>()
        .name("sweep-direction")
        .description("Which side to shift when starting a new lane (relative to the initial lane direction).")
        .defaultValue(SweepDir.LEFT)
        .visible(autoPattern::get)
        .build()
    );

    // ── Logout ───────────────────────────────────────────────────────────────

    private final Setting<Boolean> autoLogout = sgLogout.add(new BoolSetting.Builder()
        .name("auto-logout")
        .description("Disconnects from the server if you run out of rockets or have no elytra to swap to.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> logoutNoRockets = sgLogout.add(new BoolSetting.Builder()
        .name("logout-no-rockets")
        .description("Disconnect if you run out of firework rockets while boosting.")
        .defaultValue(true)
        .visible(autoLogout::get)
        .build()
    );

    private final Setting<Boolean> logoutNoElytra = sgLogout.add(new BoolSetting.Builder()
        .name("logout-no-elytra")
        .description("Disconnect if your elytra is about to break and there is no spare one to swap to.")
        .defaultValue(true)
        .visible(autoLogout::get)
        .build()
    );

    private final Setting<Integer> elytraDurabilityWarning = sgLogout.add(new IntSetting.Builder()
        .name("elytra-durability-warning")
        .description("Treat the equipped elytra as needing a swap once its remaining durability drops to this value.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 50)
        .visible(() -> autoLogout.get() && logoutNoElytra.get())
        .build()
    );

    // ── State ─────────────────────────────────────────────────────────────────

    private enum FlightPhase  { CRUISING, BOOSTING }
    private enum PatternPhase { FLYING, SHIFTING }

    private FlightPhase  flightPhase;
    private PatternPhase patternPhase;
    private int    rocketTimer;
    private boolean laneFlipped;         // true when running back on a return lane
    private double  laneStartX, laneStartZ;
    private double  shiftStartX, shiftStartZ;

    public ElytraMapper() {
        super(RobsAddon.CATEGORY, "elytra-mapper",
            "Automates elytra flight for aerial terrain mapping: manages altitude, direction lock, rocket boosts, and lawnmower lane patterns.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        flightPhase  = FlightPhase.CRUISING;
        patternPhase = PatternPhase.FLYING;
        rocketTimer  = 0;
        laneFlipped  = false;
        laneStartX   = mc.player.getX();
        laneStartZ   = mc.player.getZ();
    }

    @Override
    public String getInfoString() {
        if (mc.player == null || !mc.player.isFallFlying()) return "idle";
        return String.format("Y:%.0f  %s", mc.player.getY(),
            flightPhase == FlightPhase.BOOSTING ? "BOOST" : "CRUISE");
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || !mc.player.isFallFlying()) return;

        if (autoLogout.get() && logoutNoElytra.get() && !hasElytraToSwapTo()) {
            disconnect("No elytra left to swap to.");
            return;
        }

        double y = mc.player.getY();

        // Altitude state machine
        if (flightPhase == FlightPhase.CRUISING && y <= boostAltitude.get()) {
            flightPhase = FlightPhase.BOOSTING;
            rocketTimer = 0;
        } else if (flightPhase == FlightPhase.BOOSTING && y >= cruiseAltitude.get()) {
            flightPhase = FlightPhase.CRUISING;
        }

        // Pitch
        mc.player.setXRot(flightPhase == FlightPhase.BOOSTING
            ? boostPitch.get().floatValue()
            : cruisePitch.get().floatValue());

        // Rockets
        if (flightPhase == FlightPhase.BOOSTING && autoRocket.get()) {
            if (rocketTimer <= 0) {
                if (fireRocket()) {
                    rocketTimer = rocketInterval.get() * 20;
                } else if (autoLogout.get() && logoutNoRockets.get()) {
                    disconnect("Out of firework rockets.");
                    return;
                }
            } else {
                rocketTimer--;
            }
        }

        // Yaw / pattern
        if (autoPattern.get()) {
            tickPattern();
        } else if (lockYaw.get()) {
            applyYaw(currentLaneYaw());
        }
    }

    // ── Pattern logic ─────────────────────────────────────────────────────────

    private void tickPattern() {
        double x = mc.player.getX();
        double z = mc.player.getZ();

        if (patternPhase == PatternPhase.FLYING) {
            if (lockYaw.get()) applyYaw(currentLaneYaw());
            if (hasReachedLaneEnd(x, z)) {
                patternPhase = PatternPhase.SHIFTING;
                shiftStartX  = x;
                shiftStartZ  = z;
            }
        } else { // SHIFTING
            CardinalDir sweep = sweepCardinal();
            if (lockYaw.get()) applyYaw(dirToYaw(sweep));
            if (shiftProgress(x, z, sweep) >= laneSpacing.get()) {
                laneFlipped  = !laneFlipped;
                laneStartX   = x;
                laneStartZ   = z;
                patternPhase = PatternPhase.FLYING;
            }
        }
    }

    private boolean hasReachedLaneEnd(double x, double z) {
        if (patternMode.get() == PatternMode.LANE_LENGTH) {
            return laneProgress(x, z) >= laneLength.get();
        }
        CardinalDir dir = laneFlipped ? opposite(laneDirection.get()) : laneDirection.get();
        return switch (dir) {
            case EAST  -> x >= boundMaxX.get();
            case WEST  -> x <= boundMinX.get();
            case SOUTH -> z >= boundMaxZ.get();
            case NORTH -> z <= boundMinZ.get();
        };
    }

    private double laneProgress(double x, double z) {
        CardinalDir dir = laneFlipped ? opposite(laneDirection.get()) : laneDirection.get();
        return switch (dir) {
            case EAST  -> x - laneStartX;
            case WEST  -> laneStartX - x;
            case SOUTH -> z - laneStartZ;
            case NORTH -> laneStartZ - z;
        };
    }

    private double shiftProgress(double x, double z, CardinalDir dir) {
        return switch (dir) {
            case EAST  -> x - shiftStartX;
            case WEST  -> shiftStartX - x;
            case SOUTH -> z - shiftStartZ;
            case NORTH -> shiftStartZ - z;
        };
    }

    private CardinalDir sweepCardinal() {
        boolean left = sweepDirection.get() == SweepDir.LEFT;
        return switch (laneDirection.get()) {
            case NORTH -> left ? CardinalDir.WEST  : CardinalDir.EAST;
            case SOUTH -> left ? CardinalDir.EAST  : CardinalDir.WEST;
            case EAST  -> left ? CardinalDir.NORTH : CardinalDir.SOUTH;
            case WEST  -> left ? CardinalDir.SOUTH : CardinalDir.NORTH;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float currentLaneYaw() {
        return dirToYaw(laneFlipped ? opposite(laneDirection.get()) : laneDirection.get());
    }

    private float dirToYaw(CardinalDir dir) {
        return switch (dir) {
            case SOUTH -> 0f;
            case WEST  -> 90f;
            case NORTH -> 180f;
            case EAST  -> -90f;
        };
    }

    private CardinalDir opposite(CardinalDir dir) {
        return switch (dir) {
            case NORTH -> CardinalDir.SOUTH;
            case SOUTH -> CardinalDir.NORTH;
            case EAST  -> CardinalDir.WEST;
            case WEST  -> CardinalDir.EAST;
        };
    }

    private void applyYaw(float yaw) {
        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
    }

    private boolean fireRocket() {
        if (mc.player == null) return false;
        FindItemResult rocket = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rocket.found()) return false;
        InvUtils.swap(rocket.slot(), true);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        InvUtils.swapBack();
        return true;
    }

    private boolean hasElytraToSwapTo() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.has(DataComponents.GLIDER) && remainingDurability(chest) > elytraDurabilityWarning.get()) return true;

        for (ItemStack stack : mc.player.getInventory().getNonEquipmentItems()) {
            if (stack.has(DataComponents.GLIDER) && remainingDurability(stack) > elytraDurabilityWarning.get()) return true;
        }

        return false;
    }

    private int remainingDurability(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    private void disconnect(String reason) {
        MutableComponent text = Component.literal("[" + title + "] " + reason);

        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect.isActive()) {
            text.append(" AutoReconnect was disabled so it wouldn't fly straight back into the same problem.");
            autoReconnect.toggle();
        }

        // Toggle off so re-logging in doesn't immediately re-trigger this same disconnect.
        toggle();

        mc.getConnection().getConnection().disconnect(text);
    }

    public enum CardinalDir { NORTH, SOUTH, EAST, WEST }
    public enum SweepDir    { LEFT, RIGHT }
    public enum PatternMode { LANE_LENGTH, BOUNDING_BOX }
}
