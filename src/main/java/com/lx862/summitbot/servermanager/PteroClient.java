package com.lx862.summitbot.servermanager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.summitbot.Config;
import com.lx862.summitbot.util.JsonUtil;
import okhttp3.*;

import java.io.IOException;
import java.io.StringReader;

public class PteroClient {
    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final boolean enabled;
    private final String serverId;

    public PteroClient(Config.Pterodactyl config) {
        this.okHttpClient = new OkHttpClient();
        this.gson = new Gson();
        this.baseUrl = config.url();
        this.apiKey = config.apiKey();
        this.serverId = config.serverId();
        this.enabled = config.enabled();
    }

    public boolean isUsable() {
        return this.enabled && this.apiKey != null && this.baseUrl != null && this.serverId != null;
    }

    public String getData(String endpoint, String extras) throws IOException {
        String finalURL = (this.baseUrl + endpoint + extras).replace("{SVR_ID}", this.serverId);
        Request request = new Request.Builder()
                .url(finalURL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try(Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public JsonElement getDataJson(String endpoint, String extras) throws IOException {
        String finalURL = (this.baseUrl + endpoint + extras).replace("{SVR_ID}", this.serverId);
        Request request = new Request.Builder()
                .url(finalURL)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try(Response response = okHttpClient.newCall(request).execute()) {
            return gson.fromJson(new StringReader(response.body().string()), JsonElement.class);
        }
    }

    public JsonElement postData(String type, RequestBody requestBody, String url, boolean includeBaseURL, String extras) throws IOException {
        String finalURL;
        if (includeBaseURL) {
            finalURL = (this.baseUrl + url + extras).replace("{SVR_ID}", this.serverId);
        } else {
            finalURL = (url + extras).replace("{SVR_ID}", this.serverId);
        }

        Request request = new Request.Builder()
                .url(finalURL)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", type)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        try(Response response = okHttpClient.newCall(request).execute()) {
            return gson.fromJson(new StringReader(response.body().string()), JsonElement.class);
        }
    }

    public JsonElement sendCommand(String command) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("command", command)
                .build();

        return postData("application/json", requestBody, Endpoints.Server.SEND_COMMAND, true, "");
    }

    public JsonElement getResources() throws IOException {
        return getDataJson(Endpoints.Server.GET_RESOURCES, "");
    }

    public JsonElement getServer() throws IOException {
        return getDataJson(Endpoints.Server.GET_SERVER, "");
    }

    public JsonElement getPlayers() throws IOException {
        return getDataJson(Endpoints.Server.GET_PLAYERS, "");
    }

    public JsonElement uploadFile(String destinationPath, String filename, byte[] data) throws IOException {
        JsonObject uploadUrlResponse = getUploadURL().getAsJsonObject();
        String uploadUrl = JsonUtil.getString("attributes.url", uploadUrlResponse) + "&directory=";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("files", filename, RequestBody.create(data))
                .build();

        return postData("multipart/form-data", requestBody, uploadUrl, false, destinationPath);
    }

    public JsonElement getUploadURL() throws IOException {
        return getDataJson(Endpoints.Server.File.GET_UPLOAD_URL, "");
    }

    public JsonElement listFiles(String dir) throws IOException {
        return getDataJson(Endpoints.Server.File.LIST_FILES, dir);
    }

    public JsonElement getBackups() throws IOException {
        return getDataJson(Endpoints.Server.Backup.GET_BACKUPS, "");
    }

    public JsonElement getBackup(String uuid) throws IOException {
        return getDataJson(Endpoints.Server.Backup.GET_BACKUPS, String.format("/%s/download", uuid));
    }

    static class Endpoints {
        static class Account {
            public static final String DETAILS = "/api/client/account/";
            public static final String TWOFA = "/api/client/account/two-factor/";
            public static final String GET_API_KEYS = "/api/client/account/api-keys/";
            public static final String CREATE_API_KEYS = "/api/client/account/api-keys/";
        }
        static class Server {
            public static final String GET_SERVERS = "/api/client/";
            public static final String GET_SERVER = "/api/client/servers/{SVR_ID}/";
            public static final String GET_PLAYERS = "/api/client/servers/{SVR_ID}/players";
            public static final String GET_RESOURCES = "/api/client/servers/{SVR_ID}/resources";
            public static final String SEND_COMMAND = "/api/client/servers/{SVR_ID}/command";
            public static final String SEND_POWER = "/api/client/servers/{SVR_ID}/power";

            static class Backup {
                public static final String GET_BACKUPS = "/api/client/servers/{SVR_ID}/backups";
            }

            static class File {
                public static final String LIST_FILES = "/api/client/servers/{SVR_ID}/files/list?directory=";
                public static final String DELETE_FILES = "/api/client/servers/{SVR_ID}/files/delete";
                public static final String VIEW_FILES = "/api/client/servers/{SVR_ID}/files/contents?file=";
                public static final String GET_UPLOAD_URL = "/api/client/servers/{SVR_ID}/files/upload";
            }
        }
    }
}
