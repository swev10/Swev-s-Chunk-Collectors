package com.swevmc.utils;

import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;

import java.util.Locale;

public final class BukkitTypes {

    private BukkitTypes() {
    }

    public static Sound sound(String name, Sound fallback) {
        NamespacedKey key = key(name);
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        return sound != null ? sound : fallback;
    }

    public static Particle particle(String name, Particle fallback) {
        NamespacedKey key = key(name);
        Particle particle = key == null ? null : Registry.PARTICLE_TYPE.get(key);
        return particle != null ? particle : fallback;
    }

    private static NamespacedKey key(String name) {
        String value = name.toLowerCase(Locale.ROOT);
        return value.contains(":") ? NamespacedKey.fromString(value) : NamespacedKey.minecraft(value);
    }
}
