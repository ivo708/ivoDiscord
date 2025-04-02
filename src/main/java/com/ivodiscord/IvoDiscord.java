package com.ivodiscord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.track.Track;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class IvoDiscord implements ModInitializer {
	
	public static final String MOD_ID = "ivodiscord";

    static JsonObject config = new JsonObject();
    private static List<String> IgnoredList;
    
    private static JDA jdaInstance;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		loadIgnoredList();
		loadConfig();
		Command.register();
		ClientExecPayload.register();	
		ClientResponsePayload.register();
	    ServerPlayNetworking.registerGlobalReceiver(ClientResponsePayload.ID, (payload, context) -> {
			LOGGER.info("Payload de respuesta recibido");
            context.player().getServer().execute(() -> handleResponse(payload, context.server()));
	    });
	    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
	        try {
	            String token = config.get("BOT_TOKEN").getAsString();
	            if (token == null || token.isEmpty()) {
	                throw new IllegalArgumentException("El BOT_TOKEN está vacío. Por favor, configura un token válido.");
	            }
	            jdaInstance = JDABuilder.createDefault(token).build().awaitReady();
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    } else {
	        LOGGER.info("El entorno es CLIENTE: No se inicializa JDA.");
	    }
    	try {
    	    Class.forName("com.mysql.cj.jdbc.Driver");
    	} catch (ClassNotFoundException e) {
    	    e.printStackTrace();
    	}
    	
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            updateUser(player.getName().getLiteralString(),player.getUuidAsString());
        });
	}
	public void run() {
		
		
	}
	
	
	public void handleResponse(ClientResponsePayload payload, MinecraftServer server) {
		LOGGER.info("Payload de respuesta recibido2");
	    String minecraft_name = payload.minecraft_name;
	    String minecraft_uuid = payload.minecraft_uuid;
	    String user_name = payload.user_name;
	    String user_id = payload.user_id;
	    
	    List<UserData> userList = getFromDB(minecraft_name, minecraft_uuid);
	    int totalFilas = userList.size();
	    
	    if (totalFilas == 0) {
	        String insertSQL = "INSERT INTO usuarios (mcuser, mcuuid, discuser, discid) VALUES (?, ?, ?, ?)";
	        try (Connection conn = DriverManager.getConnection(config.get("URL").getAsString(), config.get("USER").getAsString(), config.get("PASSWORD").getAsString());
	             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
	        	
	            pstmt.setString(1, minecraft_name);
	            pstmt.setString(2, minecraft_uuid);
	            pstmt.setString(3, user_name);
	            pstmt.setString(4, user_id);
	            
	            int filasInsertadas = pstmt.executeUpdate();
	            MutableText text;
	            if (filasInsertadas > 0) {
	                text = Text.literal("Usuario vinculado correctamente.")
	                           .setStyle(Style.EMPTY.withColor(Formatting.GREEN));
	                updateUser(minecraft_name, minecraft_uuid);
	            } else {
	                text = Text.literal("Error añadiendo tu usuario.")
	                           .setStyle(Style.EMPTY.withColor(Formatting.RED));
	            }
	            server.getPlayerManager().getPlayer(minecraft_name).sendMessage(text);
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    } else {
	        MutableText text = Text.literal("Ya hay un usuario enlazado.")
	                             .setStyle(Style.EMPTY.withColor(Formatting.RED));
	        server.getPlayerManager().getPlayer(minecraft_name).sendMessage(text);
	    }
	}

	
	
	public void updateUser(String playerName, String playerUuid) {
		if(!IgnoredList.contains(playerName)) {
			LuckPerms api = LuckPermsProvider.get();
	        try {
	            Guild guild = jdaInstance.getGuildById(config.get("GUILD_ID").getAsString());
	            if (guild != null) {
	            	List<UserData> result= getFromDB(playerName,playerUuid);
	            	if(result.size()>0) {
                        LOGGER.info("Buscando al usuario : " + result.get(0).getDiscuser() + " con ID: "+ result.get(0).getDiscId()+ "en el servidor: "+ guild.getName());
		                guild.retrieveMemberById(result.get(0).getDiscId()).queue(member -> {
			                if (member != null) {
			                    List<Role> roles = member.getRoles();
			                    User lpUser = api.getUserManager().getUser(playerName);
			                    if (lpUser != null) {	
				                    if(roles.stream().anyMatch(role ->
				                    role.getName().equalsIgnoreCase("⪩ sub tier 1 ⪨") ||
				                    role.getName().equalsIgnoreCase("༶༶⁺₊ sub tier 2 ₊⁺༶༶") ||
				                    role.getName().equalsIgnoreCase("⊹ .˳⁺⁎˚ ꒰ఎ sub tier 3 ໒꒱ ˚⁎⁺˳. ⊹")))
				                    {
				                    	Group group = api.getGroupManager().getGroup("suscriptor");
				                    	Track targetTrack = null;
				                    	if (group != null) {
				                    		for (Track track : api.getTrackManager().getLoadedTracks()) {
				                    		    for (String groupName : track.getGroups()) {
				                    		        if (groupName.equalsIgnoreCase(group.getName())) {
				                    		            targetTrack = track;
				                    		            break;
				                    		        }
				                    		    }
				                    		    if (targetTrack != null) {
				                    		        break;
				                    		    }
				                    		}
				                    		if (targetTrack == null) {
				                    		    LOGGER.warn("No se encontró ningún track que contenga al grupo " + group.getName());
				                    		    return;
				                    		}
				                    		Set<String> groupsInTrack = new HashSet<>(targetTrack.getGroups());
				                    		lpUser.data().clear(node -> node instanceof InheritanceNode && groupsInTrack.contains(((InheritanceNode) node).getGroupName()));
				                    		api.getUserManager().saveUser(lpUser);
				                    	    lpUser.data().add(InheritanceNode.builder(group).build());
				                    	    api.getUserManager().saveUser(lpUser);
				                    	    api.getMessagingService().ifPresent(messagingService -> messagingService.pushUserUpdate(lpUser));
				                    	    LOGGER.info(playerName + " agregado al grupo 'suscriptor'.");
				                    	} else {
				                    	    LOGGER.warn("El grupo 'suscriptor' no existe.");
				                    	}
				                    }
				                    else {
				                    	Group group = api.getGroupManager().getGroup("default");
				                    	Track targetTrack = null;
				                    	if (group != null) {
				                    		for (Track track : api.getTrackManager().getLoadedTracks()) {
				                    		    for (String groupName : track.getGroups()) {
				                    		        if (groupName.equalsIgnoreCase(group.getName())) {
				                    		            targetTrack = track;
				                    		            break;
				                    		        }
				                    		    }
				                    		    if (targetTrack != null) {
				                    		        break;
				                    		    }
				                    		}
				                    		if (targetTrack == null) {
				                    		    LOGGER.warn("No se encontró ningún track que contenga al grupo " + group.getName());
				                    		    return;
				                    		}
				                    		Set<String> groupsInTrack = new HashSet<>(targetTrack.getGroups());
				                    		lpUser.data().clear(node -> node instanceof InheritanceNode && groupsInTrack.contains(((InheritanceNode) node).getGroupName()));
				                    		api.getUserManager().saveUser(lpUser);
				                    	    lpUser.data().add(InheritanceNode.builder(group).build());
				                    	    api.getUserManager().saveUser(lpUser);
				                    	    api.getMessagingService().ifPresent(messagingService -> messagingService.pushUserUpdate(lpUser));
				                    	    LOGGER.info(playerName + " agregado al grupo 'default'.");
				                    	} else {
				                    	    LOGGER.warn("El grupo 'default' no existe.");
				                    	}
				                    }
			                    }
				                } else {
				                	LOGGER.info("No se encontró el miembro en el servidor.");
				                }
			                }, throwable -> {
			                	LOGGER.info("Execpción encontrando el miembro en el servidor.");
		                });		                
	            	}
	            } else {
	            	LOGGER.info("No se encontró el servidor.");
	            }
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
	}
	
	public List<UserData> getFromDB(String playerName, String playerUuid) {
	    List<UserData> users = new ArrayList<>();
	    String query = "SELECT * FROM usuarios WHERE mcuser = ? OR mcuuid = ?";
        try (Connection conn = DriverManager.getConnection(config.get("URL").getAsString(), config.get("USER").getAsString(), config.get("PASSWORD").getAsString());
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        
	        stmt.setString(1, playerName);
	        stmt.setString(2, playerUuid);
	        
	        try (ResultSet rs = stmt.executeQuery()) {
	            while (rs.next()) {
	                UserData data = new UserData();
	                data.setMcuser(rs.getString("mcuser"));
	                data.setMcuuid(rs.getString("mcuuid"));
	                data.setDiscuser(rs.getString("discuser"));
	                data.setDiscId(rs.getString("discid"));
	                users.add(data);
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return users;
	}
	
	
    public static void loadIgnoredList() {
        Path directory = Paths.get("config/ivodiscord");
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
            	LOGGER.info("Error al crear el directorio: " + e.getMessage());
                IgnoredList= new ArrayList<>();
            }
        }
        Path filePath = directory.resolve("ignored.json");
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
                Files.write(filePath, "[]".getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Archivo ignored.json creado con contenido inicial.");
            } catch (IOException e) {
            	LOGGER.info("Error al crear el archivo: " + e.getMessage());
                IgnoredList= new ArrayList<>();
            }
            IgnoredList= new ArrayList<>();
        }

        try {
            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(json, listType);
            IgnoredList= list != null ? list : new ArrayList<>();
        } catch (IOException e) {
        	LOGGER.info("Error al leer el archivo: " + e.getMessage());
            IgnoredList= new ArrayList<>();
        }
    }
    public static void loadConfig() {
        Path directory = Paths.get("config/ivodiscord");
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                LOGGER.info("Error al crear el directorio: " + e.getMessage());
                config = createDefaultConfig();
                return;
            }
        }
        
        Path filePath = directory.resolve("config.json");
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
                JsonObject defaultConfig = createDefaultConfig();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonContent = gson.toJson(defaultConfig);
                Files.write(filePath, jsonContent.getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Archivo config.json creado con contenido inicial vacío.");
            } catch (IOException e) {
                LOGGER.info("Error al crear el archivo: " + e.getMessage());
                config = createDefaultConfig();
                return;
            }
        }
        
        try {
            String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonElement loadedConfig = gson.fromJson(json, JsonElement.class);
            config = (loadedConfig != null) ? loadedConfig.getAsJsonObject() : createDefaultConfig();
        } catch (IOException e) {
            LOGGER.info("Error al leer el archivo: " + e.getMessage());
            config = createDefaultConfig();
        }
    }

    private static JsonObject createDefaultConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("URL", "");
        obj.addProperty("USER", "");
        obj.addProperty("PASSWORD", "");
        obj.addProperty("BOT_TOKEN", "");
        obj.addProperty("GUILD_ID", "");
        obj.addProperty("OAUTH_URL", "");
        return obj;
    }
	
}