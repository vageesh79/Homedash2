package com.ftpix.homedash.plugin.plex;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugin.plex.model.PlexSession;
import com.ftpix.homedash.plugins.Plugin;
import com.google.common.io.Files;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlexPlugin extends Plugin {
    private static final String SETTINGS_TOKEN = "token";
    private static final String SETTINGS_URL = "url";
    private static final String PLEX_SESSIONS_URL = "%sstatus/sessions?X-Plex-Token=%s",
            PLEX_ART_URL = "%s%s?X-Plex-Token=%s",
            PLEX_HEADER_ACCEPT = "Accept",
            PLEX_HEADER_ACCEPT_VALUE = "application/json";
    private static final String IMAGE_PATH = "images/";
    private static final int THUMB_SIZE = 500;
    private String url = "";
    private String token = "";

    @Override
    public String getId() {
        return "plex";
    }

    @Override
    public String getDisplayName() {
        return "Plex";
    }

    @Override
    public String getDescription() {
        return "Display what is currently playing on a plex server";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {

        url = settings.get(SETTINGS_URL);
        token = settings.get(SETTINGS_TOKEN);

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

    }


    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1", "3x2", "3x3", "4x4", ModuleLayout.KIOSK};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        return null;
    }

    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {

        switch (size) {
            default:
                return Optional.ofNullable(getPlexSessions())
                        .map(PlexSession::getMediaContainer)
                        .get();
        }
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_SECOND * 5;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        String url = settings.get(SETTINGS_URL);
        String token = settings.get(SETTINGS_TOKEN);

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        String toCall = String.format(PLEX_SESSIONS_URL, url, token);

        logger().info("Testing setting with url:[{}]", toCall);

        try {
            GetRequest get = Unirest.get(toCall)
                    .header(PLEX_HEADER_ACCEPT, PLEX_HEADER_ACCEPT_VALUE);

            PlexSession sessions = PlexResultParser.parseJson(get.asJson().getBody());
            return null;
        } catch (UnirestException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("Connection failed", "Cound't connect to server: " + e.getMessage());
            return errors;
        }

    }

    @Override
    public ModuleExposedData exposeData() {
        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return null;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }

    //PLUGIN METHODS

    public PlexSession getPlexSessions() throws UnirestException {
        String toCall = String.format(PLEX_SESSIONS_URL, url, token);

        GetRequest get = Unirest.get(toCall).header(PLEX_HEADER_ACCEPT, PLEX_HEADER_ACCEPT_VALUE);
        JsonNode response = get.asJson().getBody();
        PlexSession sessions = PlexResultParser.parseJson(response);

        downloadSessionPictures(sessions);

        return sessions;
    }

    /**
     * Thi will download the pictures available to download
     *
     * @param sessions
     */
    private void downloadSessionPictures(PlexSession sessions) {
        Optional.ofNullable(sessions.getMediaContainer())
                .map(c -> c.size)
                .filter(i -> i > 0)
                .ifPresent(size -> {
                    ExecutorService exec = Executors.newFixedThreadPool(size * 3);

                    try {
                        Optional.ofNullable(sessions.getMediaContainer())
                                .map(c -> c.videos)
                                .orElse(Collections.emptyList())
                                .forEach(video -> {

                                    List<Callable<Void>> tasks = new ArrayList<>();

                                    tasks.add(() -> {
                                        video.grandparentThumb = Optional.ofNullable(video.grandparentThumb)
                                                .map(this::downloadPicture)
                                                .orElse("");
                                        return null;
                                    });


                                    tasks.add(() -> {
                                        video.grandparentArt = Optional.ofNullable(video.grandparentArt)
                                                .map(this::downloadPicture)
                                                .orElse("");
                                        return null;
                                    });
                                    tasks.add(() -> {
                                        video.parentThumb = Optional.ofNullable(video.parentThumb)
                                                .map(this::downloadPicture)
                                                .orElse("");
                                        return null;
                                    });
                                    tasks.add(() -> {
                                        video.parentArt = Optional.ofNullable(video.parentArt)
                                                .map(this::downloadPicture)
                                                .orElse("");
                                        return null;
                                    });
                                    tasks.add(() -> {
                                        video.thumb = Optional.ofNullable(video.thumb)
                                                .map(this::downloadPicture)
                                                .orElse("");
                                        return null;
                                    });
                                    tasks.add(() -> {
                                        video.art = Optional.ofNullable(video.art)
                                                .map(this::downloadPicture)
                                                .orElse("");
                                        return null;
                                    });

                                    try {
                                        exec.invokeAll(tasks);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    } catch (RuntimeException e) {
                        logger().error("Couldn't run in exec", e);
                    } finally {
                        exec.shutdown();
                    }

                });
    }


    private String downloadPicture(String plexPath) {
        try {
            Path filePath = getImagePath().resolve(DigestUtils.md5Hex(plexPath) + ".jpg");


            if (!java.nio.file.Files.exists(filePath)) {
                String pictureUrl = String.format(PLEX_ART_URL, url.substring(0, url.length() - 1), plexPath, token);


                logger().info("Download from: {}", pictureUrl);

                FileUtils.copyURLToFile(new URL(pictureUrl), filePath.toFile());

                // resizing the picture.
                BufferedImage image = ImageIO.read(filePath.toFile());
                BufferedImage resize = Scalr.resize(image, THUMB_SIZE);

                ImageIO.write(resize, Files.getFileExtension(filePath.toFile().getName()), filePath.toFile());


            }
            return "/" + filePath;
        } catch (Exception e) {
            logger().error("Couldn't get poster from path [{}]", plexPath, e);
            return "";
        }


    }


    private Path getImagePath() throws IOException {

        Path resolve = getCacheFolder().resolve(IMAGE_PATH);
        if (!java.nio.file.Files.exists(resolve)) {
            java.nio.file.Files.createDirectories(resolve);
        }
        return resolve;
    }
}
