package com.example.addon;

import com.example.addon.hud.ArmorPlusHud;
import com.example.addon.hud.FlightInfoHud;
import com.example.addon.modules.ElytraMapper;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class RobsAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Rob's Addon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Rob's Addon");

        Modules.get().add(new ElytraMapper());
        Hud.get().register(FlightInfoHud.INFO);
        Hud.get().register(ArmorPlusHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
