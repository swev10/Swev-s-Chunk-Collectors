package com.swevmc.managers;

import com.swevmc.SwevsChunkCollector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public final class EconomyPriceManager {

    private final SwevsChunkCollector plugin;
    private Plugin pricingPlugin;
    private PricingSource pricingSource;

    public EconomyPriceManager(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        reload();
    }

    public double getItemPrice(Material material, UUID ownerUuid) {
        double price;
        try {
            price = switch (pricingSource) {
                case SHOP_GUI_PLUS -> getShopGuiPlusPrice(material, ownerUuid);
                case ECONOMY_SHOP_GUI -> getEconomyShopGuiPrice(material);
                case CUSTOM -> plugin.getConfigManager().getCustomPrice(material);
            };
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to read the sell price for " + material, exception);
            price = plugin.getConfigManager().getFallbackPrice();
        }

        if (!Double.isFinite(price) || price < 0) {
            price = plugin.getConfigManager().getFallbackPrice();
        }
        return price * plugin.getConfigManager().getPriceMultiplier();
    }

    private double getShopGuiPlusPrice(Material material, UUID ownerUuid) throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
        ItemStack item = new ItemStack(material);

        try {
            Method method = apiClass.getMethod("getItemStackPriceSell", ItemStack.class);
            return numberValue(method.invoke(null, item));
        } catch (NoSuchMethodException exception) {
            Player player = Bukkit.getPlayer(ownerUuid);
            if (player == null) {
                return plugin.getConfigManager().getFallbackPrice();
            }
            Method method = apiClass.getMethod("getItemStackPriceSell", Player.class, ItemStack.class);
            return numberValue(method.invoke(null, player, item));
        }
    }

    private double getEconomyShopGuiPrice(Material material) throws ReflectiveOperationException {
        Class<?> hookClass = Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook");
        Method method = hookClass.getMethod("getItemSellPrice", ItemStack.class);
        Object result = method.invoke(null, new ItemStack(material));
        return result == null ? plugin.getConfigManager().getFallbackPrice() : numberValue(result);
    }

    private double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Pricing API returned a non-numeric value");
    }

    public boolean isEconomyPluginAvailable() {
        return pricingSource == PricingSource.CUSTOM || pricingPlugin != null && pricingPlugin.isEnabled();
    }

    public String getEconomyPluginName() {
        return pricingSource.name();
    }

    public void reload() {
        String configuredSource = plugin.getConfigManager().getEconomyPlugin().toUpperCase(Locale.ROOT);
        pricingSource = switch (configuredSource) {
            case "SHOPGUIPLUS" -> PricingSource.SHOP_GUI_PLUS;
            case "ECONOMYSHOPGUI", "ECONOMYSHOPGUIPREMIUM" -> PricingSource.ECONOMY_SHOP_GUI;
            default -> PricingSource.CUSTOM;
        };

        pricingPlugin = switch (pricingSource) {
            case SHOP_GUI_PLUS -> Bukkit.getPluginManager().getPlugin("ShopGUIPlus");
            case ECONOMY_SHOP_GUI -> findEconomyShopGui();
            case CUSTOM -> null;
        };

        if (pricingSource != PricingSource.CUSTOM && !isEconomyPluginAvailable()) {
            plugin.getLogger().warning(configuredSource + " is unavailable; item prices will use configured fallbacks");
        }
    }

    public void reloadPrices() {
        reload();
    }

    private Plugin findEconomyShopGui() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EconomyShopGUI");
        return plugin != null ? plugin : Bukkit.getPluginManager().getPlugin("EconomyShopGUI-Premium");
    }

    private enum PricingSource {
        SHOP_GUI_PLUS,
        ECONOMY_SHOP_GUI,
        CUSTOM
    }
}
