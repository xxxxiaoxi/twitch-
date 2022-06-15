package com.example.demo.service;

import com.example.demo.entity.db.Item;
import com.example.demo.entity.db.ItemType;
import com.example.demo.entity.response.Game;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service
public class GameService {
    private static final String TOKEN = "Bearer cg9gtizp80cqfzix2pdhnq0p20evwk";
    private static final String CLIENT_ID = "27nbg6l03x9yoq7urh4mx08b0701d5";
    private static final String TOP_GAME_URL = "https://api.twitch.tv/helix/games/top?first=%s";
    private static final String GAME_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/games?name=%s";
    private static final int DEFAULT_GAME_LIMIT = 20;

    private static final String STREAM_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/streams?game_id=%s&first=%s";
    private static final String VIDEO_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/videos?game_id=%s&first=%s";
    private static final String CLIP_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/clips?game_id=%s&first=%s";
    private static final String TWITCH_BASE_URL = "https://www.twitch.tv/";
    private static final int DEFAULT_SEARCH_LIMIT = 20;


    private String buildGameURL(String url, String gameName, int limit) {
        if (gameName.equals("")) {
            return String.format(url, limit);
        } else {
            try {
                // Encode special characters in URL, e.g. Rick Sun -> Rick%20Sun
                gameName = URLEncoder.encode(gameName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return String.format(url, gameName);
        }
    }

    private String buildSearchURL(String url, String gameId, int limit) {
        try {
            gameId = URLEncoder.encode(gameId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return String.format(url, gameId, limit);
    }


//先给twitch发一个request 然后twitch response 我们再parse他的response
    private String searchTwitch(String url) throws TwitchException {

        CloseableHttpClient httpclient = HttpClients.createDefault();

        // Define the response handler to parse and return HTTP response body returned from Twitch
        ResponseHandler<String> responseHandler = response -> {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                System.out.println("Response status: " + response.getStatusLine().getReasonPhrase());
                throw new TwitchException("Failed to get result from Twitch API");
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new TwitchException("Failed to get result from Twitch API");
            }
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));
            return obj.getJSONArray("data").toString();
        };

        try {
            // Define the HTTP request, TOKEN and CLIENT_ID are used for user authentication on Twitch backend
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", TOKEN);
            request.setHeader("Client-Id", CLIENT_ID);
            return httpclient.execute(request, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to get result from Twitch API");
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //將twitch返回的游戏数据转化为list
    private List<Game> getGameList(String data){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Game[].class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse game data from Twitch API");
        }
    }


    // Integrate search() and getGameList() together, returns the top x popular games from Twitch.
    public List<Game> topGames(int limit){
        if (limit <= 0) {
            limit = DEFAULT_GAME_LIMIT;
        }
        String url = buildGameURL(TOP_GAME_URL, "", limit);
        String data = searchTwitch(url);
        return getGameList(data);
    }

    // Integrate search() and getGameList() together, returns the dedicated game based on the game name.
    public Game searchGame(String gameName){
        String url = buildGameURL(GAME_SEARCH_URL_TEMPLATE, gameName, 0);
        String data = searchTwitch(url);
        List<Game> gameList = getGameList(data);
        if (gameList.size() != 0) {
            return gameList.get(0);
        }
        return null;
    }

    //将twitch返回的json数据转化为list
    private List<Item> getItemList(String data) throws TwitchException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Item[].class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to parse item data from Twitch API");
        }
    }
    //没给streamURL自己手动拼接
    private List<Item> searchStreams(String gameId, int limit) throws TwitchException {
        String url = buildSearchURL(STREAM_SEARCH_URL_TEMPLATE, gameId, limit);
        String data = searchTwitch(url);
        List<Item> streams = getItemList(data);
        for (Item item : streams) {
            item.setUrl(TWITCH_BASE_URL + item.getBroadcasterName());
            item.setItemType(ItemType.STREAM);
        }
        return streams;
    }

    private List<Item> searchClips(String gameId, int limit) throws TwitchException {
        String url = buildSearchURL(CLIP_SEARCH_URL_TEMPLATE, gameId, limit);
        String data = searchTwitch(url);
        List<Item> clips = getItemList(data);
        for (Item item : clips) {
            item.setItemType(ItemType.CLIP);
        }
        return clips;
    }

    private List<Item> searchVideos(String gameId, int limit) throws TwitchException {
        String url = buildSearchURL(VIDEO_SEARCH_URL_TEMPLATE, gameId, limit);
        String data = searchTwitch(url);
        List<Item> videos = getItemList(data);
        for (Item item : videos) {
            item.setItemType(ItemType.VIDEO);
        }
        return videos;
    }

    //将不同的类型对应到不同的方法来得到相应的链接
    public List<Item> searchByType(String gameId, ItemType type, int limit) throws TwitchException {
        List<Item> items = Collections.emptyList();

        switch (type) {
            case STREAM:
                items = searchStreams(gameId, limit);
                break;
            case VIDEO:
                items = searchVideos(gameId, limit);
                break;
            case CLIP:
                items = searchClips(gameId, limit);
                break;
        }
        // Update gameId for all items. GameId is used by recommendation function
        //因为twitch有些api不返回gameid，所以最好自己手动设置一下
        for (Item item : items) {
            item.setGameId(gameId);
        }
        return items;
    }

    //key:video or streams or clips  value:对应内容的list
    public Map<String, List<Item>> searchItems(String gameId) throws TwitchException {
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), searchByType(gameId, type, DEFAULT_SEARCH_LIMIT));
        }
        return itemMap;
    }
}
