package com.fibermc.essentialcommands.mixin;

import com.fibermc.essentialcommands.playerdata.PlayerDataManager;

import com.mojang.authlib.properties.Property;

import com.mojang.authlib.properties.PropertyMap;

import net.minecraft.network.PacketByteBuf;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import static com.fibermc.essentialcommands.EssentialCommands.BACKING_CONFIG;

@Mixin(PlayerListS2CPacket.Action.class)
public class PlayerListS2CPacketActionMixin {

    @Mutable
    @Final
    @Shadow
    PlayerListS2CPacket.Action.Writer writer;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    public void ctor(String string, int i, PlayerListS2CPacket.Action.Reader reader, PlayerListS2CPacket.Action.Writer argWriter, CallbackInfo ci) {
        if (!"ADD_PLAYER".equals(string)) {
            return;
        }

        var vanillaWriter = writer;
        writer = (buf, entry) -> {
            // Need to use the backing config, since this lambda captures the field value.
            if (BACKING_CONFIG.NICKNAME_ABOVE_HEAD.getValue()) {
                var id = entry.profileId();
                var displayName = PlayerDataManager.getInstance().getByUuid(id).getPlayer().getDisplayName();
                var displayNameString = displayName.asTruncatedString(16);
                buf.writeString(displayNameString, 16);
                writePropertyMap(buf, entry.profile().getProperties());
            } else {
                vanillaWriter.write(buf, entry);
            }
        };

    }

    @Unique
    private void writePropertyMap(PacketByteBuf buf, PropertyMap propertyMap) {
        buf.writeCollection(propertyMap.values(), this::writeProperty);
    }

    @Unique
    private void writeProperty(PacketByteBuf buf, Property property) {
        buf.writeString(property.name());
        buf.writeString(property.value());
        if (property.hasSignature()) {
            buf.writeBoolean(true);
            buf.writeString(property.signature());
        } else {
            buf.writeBoolean(false);
        }
    }
}
