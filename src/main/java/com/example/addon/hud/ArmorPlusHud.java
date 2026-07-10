package com.example.addon.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ArmorPlusHud extends HudElement {
    private static final Color GREEN  = new Color(85, 255, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color RED    = new Color(255, 85, 85);

    public static final HudElementInfo<ArmorPlusHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "armor+",
        "Displays armor with durability text positioned below each icon so it never overlaps.",
        ArmorPlusHud::new
    );

    private final SettingGroup sgGeneral    = settings.getDefaultGroup();
    private final SettingGroup sgDurability = settings.createGroup("Durability");
    private final SettingGroup sgScale      = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // ── General ───────────────────────────────────────────────────────────────

    private final Setting<Orientation> orientation = sgGeneral.add(new EnumSetting.Builder<Orientation>()
        .name("orientation")
        .description("Layout direction.")
        .defaultValue(Orientation.Horizontal)
        .build()
    );

    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("flip-order")
        .description("Flips the order of armor items (helmet first vs boots first).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("show-empty")
        .description("Shows a barrier icon for empty armor slots.")
        .defaultValue(false)
        .build()
    );

    // ── Durability ────────────────────────────────────────────────────────────

    private final Setting<DurabilityMode> durabilityMode = sgDurability.add(new EnumSetting.Builder<DurabilityMode>()
        .name("durability")
        .description("How to display durability.")
        .defaultValue(DurabilityMode.Percentage)
        .build()
    );

    private final Setting<Boolean> colorCode = sgDurability.add(new BoolSetting.Builder()
        .name("color-code")
        .description("Colors the text green → yellow → red based on remaining durability.")
        .defaultValue(true)
        .visible(this::isTextMode)
        .build()
    );

    private final Setting<Integer> highThreshold = sgDurability.add(new IntSetting.Builder()
        .name("high-threshold")
        .description("Durability % at or above which the text is green.")
        .defaultValue(50).min(1).max(99).sliderRange(1, 99)
        .visible(() -> colorCode.get() && isTextMode())
        .build()
    );

    private final Setting<Integer> lowThreshold = sgDurability.add(new IntSetting.Builder()
        .name("low-threshold")
        .description("Durability % below which the text is red.")
        .defaultValue(20).min(1).max(99).sliderRange(1, 99)
        .visible(() -> colorCode.get() && isTextMode())
        .build()
    );

    private final Setting<SettingColor> textColor = sgDurability.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Fixed durability text color (used when color-code is off).")
        .defaultValue(new SettingColor())
        .visible(() -> !colorCode.get() && isTextMode())
        .build()
    );

    private final Setting<Double> textScale = sgDurability.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Scale multiplier for durability text. Reduce if '100%' overflows the icon width.")
        .defaultValue(0.8).min(0.25).max(2).sliderRange(0.25, 2)
        .visible(this::isTextMode)
        .build()
    );

    private final Setting<Boolean> shadow = sgDurability.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
        .visible(this::isTextMode)
        .build()
    );

    // ── Scale ─────────────────────────────────────────────────────────────────

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Item icon scale.")
        .defaultValue(2).min(0.5).sliderRange(0.5, 4)
        .build()
    );

    // ── Background ────────────────────────────────────────────────────────────

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    public ArmorPlusHud() {
        super(INFO);
    }

    private boolean isTextMode() {
        return durabilityMode.get() == DurabilityMode.Percentage
            || durabilityMode.get() == DurabilityMode.Total;
    }

    @Override
    public void render(HudRenderer renderer) {
        ItemStack[] armor = buildArmor();

        double iScale   = scale.get();
        boolean showTxt = isTextMode();
        double txtScale = Hud.get().getTextScale() * textScale.get();
        // textH is 0 if no text so the element is exactly icon-height when durability = None or Bar
        double textH    = showTxt ? renderer.textHeight(shadow.get(), txtScale) : 0;
        double cellW    = 16 * iScale + 2;
        double cellH    = 16 * iScale + (showTxt ? 2 + textH : 2);

        if (orientation.get() == Orientation.Horizontal) {
            setSize(cellW * 4, cellH);
        } else {
            setSize(cellW, cellH * 4);
        }

        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());

        // Text renders before items so item sprites naturally draw over any accidental overlap.
        // Text is positioned strictly below each icon so there is no overlap.
        if (showTxt) {
            for (int i = 0; i < 4; i++) {
                ItemStack stack = armor[i];
                if (!stack.isDamageableItem()) continue;

                double itemX = orientation.get() == Orientation.Horizontal ? x + i * cellW : x;
                double itemY = orientation.get() == Orientation.Horizontal ? y : y + i * cellH;

                String txt = switch (durabilityMode.get()) {
                    case Percentage -> Math.round((1f - (float) stack.getDamageValue() / stack.getMaxDamage()) * 100) + "%";
                    case Total      -> String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
                    default         -> "";
                };

                double txtW = renderer.textWidth(txt, shadow.get(), txtScale);
                double txtX = itemX + 8 * iScale - txtW / 2;   // centered under the icon
                double txtY = itemY + 16 * iScale + 2;          // 2px below the icon bottom

                renderer.text(txt, txtX, txtY, durabilityColor(stack), shadow.get(), txtScale);
            }
        }

        // Items must render inside post() for correct layering with Minecraft's render pipeline.
        renderer.post(() -> {
            for (int i = 0; i < 4; i++) {
                double itemX = orientation.get() == Orientation.Horizontal ? x + i * cellW : x;
                double itemY = orientation.get() == Orientation.Horizontal ? y : y + i * cellH;
                boolean bar  = durabilityMode.get() == DurabilityMode.Bar && armor[i].isDamageableItem();
                renderer.item(armor[i], (int) itemX, (int) itemY, (float) iScale, bar);
            }
        });
    }

    private Color durabilityColor(ItemStack stack) {
        if (!colorCode.get()) return textColor.get();
        float pct = (1f - (float) stack.getDamageValue() / stack.getMaxDamage()) * 100f;
        if (pct >= highThreshold.get()) return GREEN;
        if (pct >= lowThreshold.get())  return YELLOW;
        return RED;
    }

    private ItemStack[] buildArmor() {
        EquipmentSlot[] slots = flipOrder.get()
            ? new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}
            : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

        ItemStack[] result = new ItemStack[4];
        for (int i = 0; i < 4; i++) result[i] = getItem(slots[i]);
        return result;
    }

    private ItemStack getItem(EquipmentSlot slot) {
        if (isInEditor()) {
            return switch (slot) {
                case HEAD  -> DisplayItemUtils.toStack(Items.NETHERITE_HELMET);
                case CHEST -> DisplayItemUtils.toStack(Items.NETHERITE_CHESTPLATE);
                case LEGS  -> DisplayItemUtils.toStack(Items.NETHERITE_LEGGINGS);
                default    -> DisplayItemUtils.toStack(Items.NETHERITE_BOOTS);
            };
        }
        ItemStack stack = mc.player.getItemBySlot(slot);
        return stack.isEmpty() && showEmpty.get() ? DisplayItemUtils.toStack(Items.BARRIER) : stack;
    }

    public enum DurabilityMode { None, Bar, Percentage, Total }
    public enum Orientation    { Horizontal, Vertical }
}
