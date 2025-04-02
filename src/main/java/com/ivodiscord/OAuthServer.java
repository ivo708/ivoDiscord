package com.ivodiscord;

import fi.iki.elonen.NanoHTTPD;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class OAuthServer extends NanoHTTPD {

    // Atributo para almacenar el resultado del usuario autenticado
    private CompletableFuture<JsonObject> UserFuture = new CompletableFuture<>();
    
    public CompletableFuture<JsonObject> getUserFuture() {
        return UserFuture;
    }
    
    public OAuthServer() throws IOException {
        super(7080);
        start(SOCKET_READ_TIMEOUT, false);
        System.out.println("Servidor de OAuth iniciado en http://localhost:7080");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, List<String>> params = session.getParameters();

        if (uri.equals("/callback") && params.containsKey("code")) {
            String code = params.get("code").get(0);
            exchangeCodeForToken(code)
                .thenCompose(this::obtenerUsuarioDiscord)
                .thenAccept(user -> {
                    UserFuture.complete(user);
                    stop();
                    System.out.println("Servidor de OAuth detenido después del callback.");
                });

            return newFixedLengthResponse(
            	    "<!DOCTYPE html>\n" +
            	    "<html lang=\"es\">\n" +
            	    "  <head>\n" +
            	    "    <meta charset=\"UTF-8\">\n" +
            	    "    <title>Autenticaci\u00F3n Completada</title>\n" +
            	    "    <style>\n" +
            	    "      body { \n" +
            	    "        background-color: #7289da; \n" +
            	    "        margin: 0; \n" +
            	    "        padding: 0; \n" +
            	    "        font-family: Arial, sans-serif; \n" +
            	    "        display: flex; \n" +
            	    "        align-items: center; \n" +
            	    "        justify-content: center; \n" +
            	    "        height: 100vh; \n" +
            	    "        color: #fff; \n" +
            	    "      }\n" +
            	    "      .container { \n" +
            	    "        text-align: center; \n" +
            	    "        background: rgba(0, 0, 0, 0.5); \n" +
            	    "        padding: 30px; \n" +
            	    "        border-radius: 10px; \n" +
            	    "        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3); \n" +
            	    "      }\n" +
            	    "      .container img { \n" +
            	    "        width: 100px; \n" +
            	    "        margin-bottom: 20px; \n" +
            	    "      }\n" +
            	    "      h1 { \n" +
            	    "        margin: 0; \n" +
            	    "        font-size: 2em; \n" +
            	    "      }\n" +
            	    "      p { \n" +
            	    "        font-size: 1.2em; \n" +
            	    "        margin-top: 10px; \n" +
            	    "      }\n" +
            	    "      .imagen-redondeada { \n" +
            	    "        width: 300px; \n" +
            	    "        height: auto; \n" +
            	    "        border-radius: 15px; \n" +
            	    "        display: block; \n" +
            	    "        margin: 20px auto; \n" +
            	    "      }\n" +
            	    "    </style>\n" +
            	    "  </head>\n" +
            	    "  <body>\n" +
            	    "    <div class=\"container\">\n" +
            	    "      <img src=\"https://cdn-icons-png.flaticon.com/512/2111/2111370.png\" alt=\"Discord Icon\">\n" +
            	    "      <h1>Autenticaci\u00F3n completada</h1>\n" +
            	    "      <p>Vuelve a tu juego, el proceso se completar\u00E1 autom\u00E1ticamente.</p>\n" +
            	    "      <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAP8AAAEFBAMAAADeWCwyAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAtUExURXvF2FlRFDAtJninpNPDIPrrKYl2H9f/v53QxayaHhgWGOQ8Kc8iHvr/USEiGg0if50AAAAJcEhZcwAADsIAAA7CARUoSoAAAAYnSURBVHja7Zy/bxxFFMcNSIh2IUEKBbKH2EmuQChXEFERtASZKrHughwJiVhhBa6iCGkLKCJorgwojrSpHGMcZUH+A7iOMnKbkgaJJsX9Dbx935m3s+u9YNJ4Ub7f4nS78+N9tph5b97M7gJFURRF9VovJUniVO8Mh+8RgAAEOAbrJ9I0HavW/jvDq1m2AfosywhAgP8fwCvD4XnnlmE9z/Nv0vQj5xaP3F5sXtcH+FjaPw8DAQjwIgFMJhP5fW0yuW0X8vtDDCAaKcBZK+9ocju+JTbPFcXWePyZ0IOhuwkBCNADgLh13bvoZedWZPTAIcIbVrIw0Uuu5a717nuRgtMYg5Bc11agGogABHjRASbzhKnAlWUpDKtFcb8sd2BQsAqTXMQLuAtoLLdeNwZ4Q+t1iMpDrUwAAvQZwNcWAOnt5Hh8BUs1W63Vwl1h/FHNOgCJN7yH1RkCQnT5vVxPp9ODgwPp+OdqtUcAAhBgrunzGK7S2lzbuoz72PLnf0Z6orckek2lvlS8a6u5uOMKQKyD4XFZbhOAAL0EGKr1pSR5E35Pfm/EAV6H9ZihGpbSZFOH2YXDTzYEA55vkQAEIEAXgPipB0lySoeyV9t6B8OTVuFVdYVnux9QTBCAAP0EkGiuVD2KrY+fV+saJiYdDE7XfDsEIAAB/DbU0CLQMpJnOIIdqYWMyFK83QytOc2FtAEwFRCAAP0AgF9aybKvJfqzYWgMxREY4lbiRpO4zC/VOhhQf5sABCDAAvalvMSgzQb7s9lfyHXMDUqPAFAlVUpNNbYBpO8BAQjQCwAXknpeAlRbh5KwTpuDMdcbyhj8VDqbzWbyKx1/FQPIyPyAAAQgwKF5wBg2Wwwuzhui1jOnBin/JM+/Ozj4A43B0MgbEoAAvQFoW0cH63piolLMMMCt6XQqvw/z/Fv8Qy2c/bsC14gdqV+n099RCIeIkZogQCQAAQgQeUNsStXWIXFlF5Pm2T0PgOMPot/wT0oakwAcqrcOYbZZtcfC1tWAAAQgQAWA5eDpf484B2WkA9XjuPeL4bhTtTaM61bl9kwp+rMyAhCgHwClJs5XUC/tzAuOwnG9Rh6xElydtP3SKruwI9XxWGuuuSlAAAIQQINSnFK/XzZH7DPXftB6dzq/depwbI/lMybNeYAABOgDAKK1An4RtS/Fw6pbeAWxUtKS09TjVqt+vTQzh6hnywlAAAKMwysaaRp2zVB1GUHkXAAJP9+STia6/5Xn+S3nzuivw63lVn2nb33Up0JSnRS2CUCAXgBAI2Mo7BUlG5a1a0QHqY7BAVxbxzBMdJ9ry5q4sDXW8LFi8kMCEIAAC42hbhPCasCoXrMygIoRcWuh6zT//mEW6UYMuKxH3DNYLzq3ny8TgADHD4CRNmqNtBijcKGrTVu91RmGrLn3fAkGE3WVj4riJ6fZ+GLecSgCEIAAl6tTtWnnUmxkncIt1ul63FpCUNohzAjXrbzozKPgAe8QgADHD5CEpN1uOm8dBsDNThuN87Sn4EChRv2OLgsdo4sEIAABFqz2XhZy9W21e0v0m0gZ3gppAXgl8SSBuLZhvQwvlnxBAAL0BWADeezYbB0mopv9mQoXsH4XXZl24T1jrKoVwsK6S5TDMAEIQIAAAAZ/Ki/TCNXXho36pDswnB6PSFGIW0/L8hdLosyC6vPxqZ51OoGI1qwSgAA9ACj0e0M1QBkOBV3Fas0MNN73KNX7nZRy0Ijenc3+BkB3/Xqw4xZG8h0CEIAA/iCT36QyJyYj+hriVACgN58kcTobPIX3i80s41RtpnnBexrK7qAEa7gHCFXzPMfZ8nMEIEAvAOAApd0b+DpKh8xABVCE9w1TS42bGodYr5n1MgSAG/j4eB50q0pQEIAABEAQ6r+j17K8bxForSzKo4wOzwNeWTwJACDR41Fn8qZuEoAAxw/ggg98KJfwfrXptnVULsLZ8VEaMn5tALs1KMNur9Mx+D4BCECALgDUqwBsKgCAxJ1lzDAow4tNMOO/dmSFdYYEvey0ANqTAAEI0CcAXwoA8Vtvq/VqaTbTBPmeZRfQteUwvCkA7CJMxK09Oxeb6o4VAQhAgE6A/OY/pQas3I3a0SMAAAAASUVORK5CYII=\" class=\"imagen-redondeada\">"+
            	    "  </div>"+
            	    "</body>"+
            	    "</html>"
            	    );



        }

        return newFixedLengthResponse("<html><body><h1>Servidor OAuth activo</h1></body></html>");
    }

    private CompletableFuture<String> exchangeCodeForToken(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String clientId = "1355938934575730829";
                String clientSecret = "2WyjD6xa2Q4zjGZcnHqPa5gsxYA_QU9X";
                String redirectUri = "http://localhost:7080/callback";

                String urlParameters = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                        "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                        "&grant_type=authorization_code" +
                        "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

                byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                URI uri = new URI("https://discord.com/api/oauth2/token");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
                conn.setRequestProperty("User-Agent", "MinecraftMod/1.0");

                try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                    dos.write(postData);
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Gson gson = new Gson();
                JsonObject tokenResponse = gson.fromJson(response.toString(), JsonObject.class);
                return tokenResponse.has("access_token") ? tokenResponse.get("access_token").getAsString() : null;
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private CompletableFuture<JsonObject> obtenerUsuarioDiscord(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (accessToken == null) {
                    return null;
                }

                URI uri = new URI("https://discord.com/api/users/@me");
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("User-Agent", "MinecraftMod/1.0");

                int responseCode = connection.getResponseCode();
                InputStream is = (responseCode == 200) ? connection.getInputStream() : connection.getErrorStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);

                return jsonResponse;
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

}
