package com.ivodiscord;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Command {


	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
			dispatcher.register(CommandManager.literal("discordlink")
					.executes(context -> {
						ServerPlayerEntity player;
						player = context.getSource().getPlayer();
						commandRun(player);
						return 1;
					})
			);
		});
		
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
			dispatcher.register(CommandManager.literal("discordlink")
					.then(CommandManager.literal("reload")
							.executes(context -> {
									IvoDiscord.loadIgnoredList();
									IvoDiscord.loadConfig();
								return 1;
							})
					)
			);
		});	
	}

	public static void commandRun(ServerPlayerEntity player) {
		
        MutableText text = Text.literal("\nPara enlazar Discord haz click ")
                .setStyle(Style.EMPTY.withColor(Formatting.BLUE));
        
        
        MutableText clickableText = Text.literal("aqui")
                .setStyle(Style.EMPTY.withColor(Formatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, IvoDiscord.config.get("OAUTH_URL").getAsString())));
        
        text.append(clickableText);
        text.append(Text.literal("\n"));
        
        player.sendMessage(text, false);
        
        ClientExecPayload payload = new ClientExecPayload();

        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player,payload);
        
        
	}

}
