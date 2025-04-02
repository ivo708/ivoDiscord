package com.ivodiscord;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ClientExecPayload implements CustomPayload {
    // ID del CustomPayload
    public static final CustomPayload.Id<ClientExecPayload> ID =new CustomPayload.Id<>(Identifier.of("ivodiscord", "client_exec"));

    // Constructor para inicializar desde un PacketByteBuf
    public ClientExecPayload(PacketByteBuf buf) {
    }

    // Constructor para inicializar desde un BlockPos
    public ClientExecPayload() {
		super();
	}

    @Override
    public CustomPayload.Id<ClientExecPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static class Codec implements PacketCodec<RegistryByteBuf, ClientExecPayload> {
        @Override
        public void encode(RegistryByteBuf buf, ClientExecPayload payload) {
            payload.write(buf);
        }

		@Override
		public ClientExecPayload decode(RegistryByteBuf buf) {
			
            return new ClientExecPayload(buf);
		}
    }

    // Método estático para registrar el payload en PayloadTypeRegistry
    public static void register() {
        // Registra el payload con el codec
        PayloadTypeRegistry.playS2C().register(ID, new Codec());
    }
}