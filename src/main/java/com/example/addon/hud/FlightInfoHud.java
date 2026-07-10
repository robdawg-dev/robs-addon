package com.example.addon.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class FlightInfoHud extends HudElement {
    public static final HudElementInfo<FlightInfoHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "flight-info",
        "Displays current pitch and yaw.",
        FlightInfoHud::new
    );

    private static final Minecraft mc = Minecraft.getInstance();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> labelColor = sgGeneral.add(new ColorSetting.Builder()
        .name("label-color")
        .description("Color of the label text.")
        .defaultValue(new SettingColor(180, 180, 180))
        .build()
    );

    private final Setting<SettingColor> valueColor = sgGeneral.add(new ColorSetting.Builder()
        .name("value-color")
        .description("Color of the value text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Boolean> showCardinal = sgGeneral.add(new BoolSetting.Builder()
        .name("show-cardinal")
        .description("Show approximate cardinal direction next to the yaw value.")
        .defaultValue(true)
        .build()
    );

    public FlightInfoHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        float pitch, yaw;
        if (mc.player != null && !isInEditor()) {
            pitch = mc.player.getXRot();
            yaw   = Mth.wrapDegrees(mc.player.getYRot());
        } else {
            pitch = -3.0f;
            yaw   = 180.0f;
        }

        String pitchVal = String.format("%.1f°", pitch);
        String yawVal   = String.format("%.1f°", yaw);
        if (showCardinal.get()) yawVal += " " + toCardinal(yaw);

        double scale = Hud.get().getTextScale();
        boolean sh   = shadow.get();
        double x     = this.x;

        x = renderer.text("Pitch: ", x, y, labelColor.get(), sh, scale);
        x = renderer.text(pitchVal,  x, y, valueColor.get(), sh, scale);
        x = renderer.text("  Yaw: ", x, y, labelColor.get(), sh, scale);
        x = renderer.text(yawVal,    x, y, valueColor.get(), sh, scale);

        setSize(x - this.x, renderer.textHeight(sh, scale));
    }

    // Minecraft yaw: 0=South, 90=West, 180=North, 270=East
    private String toCardinal(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        if (y < 22.5 || y >= 337.5) return "S";
        if (y < 67.5)  return "SW";
        if (y < 112.5) return "W";
        if (y < 157.5) return "NW";
        if (y < 202.5) return "N";
        if (y < 247.5) return "NE";
        if (y < 292.5) return "E";
        return "SE";
    }
}
