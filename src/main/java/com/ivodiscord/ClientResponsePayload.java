package com.ivodiscord;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ClientResponsePayload implements CustomPayload {
	final String user_id;
	final String user_name;
	final String minecraft_uuid;
	final String minecraft_name;	
	
    // ID del CustomPayload
    public static final CustomPayload.Id<ClientResponsePayload> ID =new CustomPayload.Id<>(Identifier.of("ivodiscord", "client_response"));

    // Constructor para inicializar desde un PacketByteBuf
    public ClientResponsePayload(PacketByteBuf buf) {
    	this.user_id=buf.readString();
    	this.user_name=buf.readString();
    	this.minecraft_uuid=buf.readString();
    	this.minecraft_name=buf.readString();

    }

    public ClientResponsePayload(String user_id,String user_name,String minecraft_uuid,String minecraft_name) {
		super();
    	this.user_id=user_id;
    	this.user_name=user_name;
    	this.minecraft_uuid=minecraft_uuid;
    	this.minecraft_name=minecraft_name;
	}

    @Override
    public CustomPayload.Id<ClientResponsePayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    	buf.writeString(user_id);
    	buf.writeString(user_name);
    	buf.writeString(minecraft_uuid);
    	buf.writeString(minecraft_name);

    }

    public String getUser_id() {
		return user_id;
	}

	public String getUser_name() {
		return user_name;
	}

	public String getMinecraft_uuid() {
		return minecraft_uuid;
	}

	public String getMinecraft_name() {
		return minecraft_name;
	}

	public static class Codec implements PacketCodec<RegistryByteBuf, ClientResponsePayload> {
        @Override
        public void encode(RegistryByteBuf buf, ClientResponsePayload payload) {
            payload.write(buf);
        }

		@Override
		public ClientResponsePayload decode(RegistryByteBuf buf) {			
            return new ClientResponsePayload(buf);
		}
    }

    // Método estático para registrar el payload en PayloadTypeRegistry
    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, new Codec());
    }
}