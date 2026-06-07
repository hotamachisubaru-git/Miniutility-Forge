package org.hotamachisubaru.miniutility.forge.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ForgeLuckPermsUtil {
    private static final String LUCKPERMS_MOD_ID = "luckperms";

    public String safePrefix(ServerPlayer player) {
        String prefix = getMeta(player, "getPrefix");
        return prefix == null ? "" : prefix;
    }

    public String safeSuffix(ServerPlayer player) {
        String suffix = getMeta(player, "getSuffix");
        return suffix == null ? "" : suffix;
    }

    private String getMeta(ServerPlayer player, String methodName) {
        if (player == null || !ModList.get().isLoaded(LUCKPERMS_MOD_ID)) {
            return null;
        }

        try {
            Object luckPerms = luckPermsApi();
            Object user = user(luckPerms, player.getUUID());
            if (user == null) {
                return null;
            }

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            Object value = metaData.getClass().getMethod(methodName).invoke(metaData);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static Object luckPermsApi() throws ReflectiveOperationException {
        Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
        Method get = provider.getMethod("get");
        return get.invoke(null);
    }

    private static Object user(Object luckPerms, UUID uniqueId) throws ReflectiveOperationException {
        Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
        return userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uniqueId);
    }
}
