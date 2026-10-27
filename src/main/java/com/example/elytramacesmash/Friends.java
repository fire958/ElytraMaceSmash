package com.example.elytramacesmash;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Friends {
    private static final Set<String> NAMES = new LinkedHashSet<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("elytramacesmash_friends.txt");

    public static void load() {
        NAMES.clear();
        try {
            if (Files.exists(FILE)) {
                for (String l : Files.readAllLines(FILE)) if (!l.isBlank()) NAMES.add(l.trim().toLowerCase());
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        try { Files.write(FILE, NAMES); } catch (Exception ignored) {}
    }

    public static boolean isFriend(String name) { return NAMES.contains(name.toLowerCase()); }
    public static void add(String name) { if (!name.isBlank()) { NAMES.add(name.trim().toLowerCase()); save(); } }
    public static void remove(String name) { NAMES.remove(name.toLowerCase()); save(); }
    public static List<String> all() { return new ArrayList<>(NAMES); }
}
