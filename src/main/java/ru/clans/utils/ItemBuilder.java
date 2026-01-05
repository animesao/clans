package ru.clans.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        meta.setDisplayName(ColorUtil.colorize(name));
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ColorUtil.colorize(line));
        }
        meta.setLore(coloredLore);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ColorUtil.colorize(line));
        }
        meta.setLore(coloredLore);
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(ColorUtil.colorize(line));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addGlow() {
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder addFlag(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideFlags() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, 
                          ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_POTION_EFFECTS);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder setSkullOwner(OfflinePlayer player) {
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwningPlayer(player);
        }
        return this;
    }

    public ItemBuilder setSkullTexture(String base64) {
        if (!(meta instanceof SkullMeta)) return this;
        
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("textures", base64));
            
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        try {
            meta.getClass().getMethod("setCustomModelData", Integer.class).invoke(meta, data);
        } catch (Exception ignored) {
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFiller(Material material) {
        return new ItemBuilder(material)
                .setName(" ")
                .hideFlags()
                .build();
    }

    public static ItemStack createFiller() {
        return createFiller(Material.GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack createGradientFiller(int position) {
        Material[] gradient = {
            Material.BLACK_STAINED_GLASS_PANE,
            Material.GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE
        };
        int index = Math.min(position % gradient.length, gradient.length - 1);
        return new ItemBuilder(gradient[index])
                .setName(" ")
                .hideFlags()
                .build();
    }

    public static ItemStack createCustomHead(String base64, String name, String... lore) {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullTexture(base64)
                .setName(name)
                .setLore(lore)
                .build();
    }
}
