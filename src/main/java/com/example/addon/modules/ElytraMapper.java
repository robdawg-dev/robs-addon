package com.example.addon.modules;

import com.example.addon.RobsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ElytraMapper extends Module {

    private final SettingGroup sgAltitude   = settings.createGroup("Altitude");
    private final SettingGroup sgDirection  = settings.createGroup("Direction");
    private final SettingGroup sgRockets    = settings.createGroup("Rockets");
    private final SettingGroup sgPattern    = settings.createGroup("Pattern");
    private final SettingGroup sgEquipment  = settings.createGroup("Equipment");

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

    private final Setting<Boolean> autoRestockRockets = sgRockets.add(new BoolSetting.Builder()
        .name("auto-restock-rockets")
        .description("Move rockets from inventory to hotbar when the hotbar supply runs low.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minRocketCount = sgRockets.add(new IntSetting.Builder()
        .name("min-rocket-count")
        .description("Restock hotbar rockets from inventory when total hotbar rocket count drops below this.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 64)
        .visible(autoRestockRockets::get)
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
        .min(-30000000)
        .max(30000000)
        .sliderRange(-30000000, 30000000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Integer> boundMaxX = sgPattern.add(new IntSetting.Builder()
        .name("bound-max-x")
        .description("Eastern boundary (maximum X). Lane turns around here when flying East.")
        .defaultValue(500)
        .min(-30000000)
        .max(30000000)
        .sliderRange(-30000000, 30000000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Integer> boundMinZ = sgPattern.add(new IntSetting.Builder()
        .name("bound-min-z")
        .description("Northern boundary (minimum Z). Lane turns around here when flying North.")
        .defaultValue(-500)
        .min(-30000000)
        .max(30000000)
        .sliderRange(-30000000, 30000000)
        .visible(() -> autoPattern.get() && patternMode.get() == PatternMode.BOUNDING_BOX)
        .build()
    );

    private final Setting<Integer> boundMaxZ = sgPattern.add(new IntSetting.Builder()
        .name("bound-max-z")
        .description("Southern boundary (maximum Z). Lane turns around here when flying South.")
        .defaultValue(500)
        .min(-30000000)
        .max(30000000)
        .sliderRange(-30000000, 30000000)
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

    // ── Equipment ─────────────────────────────────────────────────────────────

    private final Setting<Boolean> autoSwapElytra = sgEquipment.add(new BoolSetting.Builder()
        .name("auto-swap-elytra")
        .description("Automatically equip a spare elytra when the current one's durability falls below the threshold.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minElytraDurability = sgEquipment.add(new IntSetting.Builder()
        .name("min-elytra-durability")
        .description("Durability percentage at which to swap to a spare elytra from hotbar or inventory.")
        .defaultValue(10)
        .min(1)
        .max(99)
        .sliderRange(1, 50)
        .visible(autoSwapElytra::get)
        .build()
    );

    // ── State ─────────────────────────────────────────────────────────────────

    private enum FlightPhase  { CRUISING, BOOSTING }
    private enum PatternPhase { FLYING, SHIFTING }

    private FlightPhase  flightPhase;
    private PatternPhase patternPhase;
    private int     rocketTimer;
    private int     equipCheckTimer;
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
        flightPhase      = FlightPhase.CRUISING;
        patternPhase     = PatternPhase.FLYING;
        rocketTimer      = 0;
        equipCheckTimer  = 0;
        laneFlipped      = false;
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
                fireRocket();
                rocketTimer = rocketInterval.get() * 20;
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

        // Equipment checks (elytra durability + rocket restock) — once per second
        if (--equipCheckTimer <= 0) {
            equipCheckTimer = 20;
            checkEquipment();
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
            boolean doneShifting = shiftProgress(x, z, sweep) >= laneSpacing.get();
            if (!doneShifting && patternMode.get() == PatternMode.BOUNDING_BOX)
                doneShifting = atBound(x, z, sweep);
            if (doneShifting) {
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
        // Use velocity lookahead (3 ticks) so fast elytra flight doesn't overshoot the boundary
        Vec3 vel = mc.player.getDeltaMovement();
        double px = x + vel.x * 3;
        double pz = z + vel.z * 3;
        return switch (dir) {
            case EAST  -> px >= boundMaxX.get();
            case WEST  -> px <= boundMinX.get();
            case SOUTH -> pz >= boundMaxZ.get();
            case NORTH -> pz <= boundMinZ.get();
        };
    }

    private boolean atBound(double x, double z, CardinalDir dir) {
        Vec3 vel = mc.player.getDeltaMovement();
        double px = x + vel.x * 3;
        double pz = z + vel.z * 3;
        return switch (dir) {
            case EAST  -> px >= boundMaxX.get();
            case WEST  -> px <= boundMinX.get();
            case SOUTH -> pz >= boundMaxZ.get();
            case NORTH -> pz <= boundMinZ.get();
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

    // ── Equipment management ──────────────────────────────────────────────────

    private void checkEquipment() {
        if (autoSwapElytra.get())      checkElytra();
        if (autoRestockRockets.get())  checkRockets();
    }

    private void checkElytra() {
        ItemStack current = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (current.getItem() != Items.ELYTRA || current.getMaxDamage() == 0) return;
        float currentPct = (1f - (float) current.getDamageValue() / current.getMaxDamage()) * 100f;
        if (currentPct > minElytraDurability.get()) return;

        // Search all inventory slots — InvUtils.move handles index→container-ID conversion
        // and cursor cleanup automatically. toArmor(2) = chest slot. (0=feet,1=legs,2=chest,3=head)
        for (int i = 0; i < 36; i++) {
            if (isSpareElytra(mc.player.getInventory().getItem(i), currentPct)) {
                InvUtils.move().from(i).toArmor(2);
                return;
            }
        }
    }

    private boolean isSpareElytra(ItemStack stack, float worseThanPct) {
        if (stack.getItem() != Items.ELYTRA || stack.getMaxDamage() == 0) return false;
        float pct = (1f - (float) stack.getDamageValue() / stack.getMaxDamage()) * 100f;
        return pct > worseThanPct;
    }

    private void checkRockets() {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.getItem() == Items.FIREWORK_ROCKET) total += s.getCount();
        }
        if (total >= minRocketCount.get()) return;

        // Target: prefer a hotbar slot with an existing partial stack; fall back to an empty slot
        int target = -1;
        for (int i = 0; i < 9 && target == -1; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.FIREWORK_ROCKET) target = i;
        }
        for (int i = 0; i < 9 && target == -1; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) target = i;
        }
        if (target == -1) return;

        // Move inventory stack to target hotbar slot — InvUtils deposits any cursor overflow back
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.FIREWORK_ROCKET) {
                InvUtils.move().from(i).to(target);
                return;
            }
        }
    }

    private void fireRocket() {
        if (mc.player == null) return;
        FindItemResult rocket = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rocket.found()) return;
        InvUtils.swap(rocket.slot(), true);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        InvUtils.swapBack();
    }

    public enum CardinalDir { NORTH, SOUTH, EAST, WEST }
    public enum SweepDir    { LEFT, RIGHT }
    public enum PatternMode { LANE_LENGTH, BOUNDING_BOX }
}
