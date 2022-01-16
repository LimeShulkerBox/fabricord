package com.limeshulkerbox.fabricord.minecraft.mixins;

import com.limeshulkerbox.fabricord.api.v1.API;
import com.limeshulkerbox.fabricord.minecraft.ServerInitializer;
import com.limeshulkerbox.fabricord.other.UUIDConverter;
import com.mojang.authlib.GameProfile;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import net.dv8tion.jda.api.entities.Activity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static com.limeshulkerbox.fabricord.minecraft.ServerInitializer.config;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    @Shadow
    public abstract boolean shouldDamagePlayer(PlayerEntity player);

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    private static Thread thread;

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At(value = "TAIL"))
    public void getMessage(Text message, MessageType type, UUID sender, CallbackInfo ci) throws IOException {
        thread = new Thread(() ->
        {
            try {
                if (!sender.equals(ServerInitializer.modUUID)) {
                    String key = "this.cannot.be.blank";
                    if (message instanceof TranslatableText translatableText) {
                        key = translatableText.getKey();
                    }
                    if (API.checkIfSomethingIsPresent(config.getKeysToSendToDiscord(), key) && sender.equals(getUuid())) {
                        if (!config.isOnlyWebhooks()) {
                            if (config.isChatEnabled()) {
                                API.sendMessageToDiscordChat(message.getString());
                                thread.interrupt();
                            }
                            if (config.isWebhooksEnabled()) {
                                String message1 = message.getString();
                                API.sendMessageToDiscordWebhook(message1.replaceFirst("<.+?> ", ""), UUIDConverter.getName(sender), "https://crafatar.com/avatars/" + sender + "?&overlay", config.getWebhookURL());
                                thread.interrupt();
                            }
                        } else {
                            if (config.isWebhooksEnabled()) {
                                String message1 = message.getString();
                                API.sendMessageToDiscordWebhook(message1.replaceFirst("<.+?> ", ""), UUIDConverter.getName(sender), "https://crafatar.com/avatars/" + sender + "?&overlay", config.getWebhookURL());
                                thread.interrupt();
                            } else if (config.isChatEnabled()) {
                                API.sendMessageToDiscordChat(message.getString());
                                thread.interrupt();
                            }
                        }
                    } else if (!key.equals("this.cannot.be.blank")) {
                        System.out.println("The key: " + key + " is not present in the config. This message is from ServerPlayerEntityMixin from Fabricord.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Fabricord get message");
        thread.start();
    }
}