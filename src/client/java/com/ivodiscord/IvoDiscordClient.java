package com.ivodiscord;

import java.io.IOException;

import com.google.gson.JsonObject;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;

public class IvoDiscordClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {        

        ClientPlayNetworking.registerGlobalReceiver(ClientExecPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> handleAuth(payload));
        });
    }
    
    private void handleAuth(ClientExecPayload payload) {
        try {
            // Iniciar el servidor OAuth
        	IvoDiscord.LOGGER.info("Obteniendo autorizacion");
            OAuthServer server = new OAuthServer();

            // Suscribirse al futuro para obtener el JSON del usuario
            server.getUserFuture().thenAccept(user -> {
                if (user != null) {                	
                	sendUserBack(user);       
                	
                } else {
                	IvoDiscord.LOGGER.info("No se pudo obtener el usuario.");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendUserBack(JsonObject user) {
        MinecraftClient.getInstance().execute(() -> {
            IvoDiscord.LOGGER.info("Devolviendo respuesta...");
            System.out.println("JSON recibido de Discord: " + user.toString());

            if (user == null || !user.has("id") || !user.has("username")) {
                IvoDiscord.LOGGER.error("Error: JSON inválido o incompleto");
                return;
            }

            String user_id = user.get("id").getAsString();
            String user_name = user.get("username").getAsString();
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            String minecraft_uuid = player.getUuidAsString();
            String minecraft_name = player.getName().getLiteralString();

            ClientResponsePayload payload = new ClientResponsePayload(user_id, user_name, minecraft_uuid, minecraft_name);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            payload.write(buf);
            ClientPlayNetworking.send(payload);
        });
    }


    
}
