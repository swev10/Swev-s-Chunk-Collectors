package com.swevmc.utils;

import net.md_5.bungee.api.ChatColor;

public class ColorUtils {
    
    public static String translateColors(String text) {
        if (text == null) return null;
        
        text = text.replace("&", "§");
        text = translateHexColors(text);
        
        return text;
    }
    
    private static String translateHexColors(String text) {
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("#[A-Fa-f0-9]{6}");
        java.util.regex.Matcher matcher = hexPattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group();
            try {
                ChatColor color = ChatColor.of(hexColor);
                matcher.appendReplacement(result, color.toString());
            } catch (Exception e) {
                matcher.appendReplacement(result, hexColor);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public static String stripColors(String text) {
        if (text == null) return null;
        
        text = text.replaceAll("§[0-9a-fk-or]", "");
        
        text = text.replaceAll("#[A-Fa-f0-9]{6}", "");
        
        return text;
    }
}
