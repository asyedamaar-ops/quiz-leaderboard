import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class QuizLeaderboard {

    static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    static final String REG_NO = "RA2311026010439";

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();

        // using a set to track which round+participant we already processed
        // this way duplicate entries across polls get ignored
        Set<String> processed = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        System.out.println("polling API...");

        for (int poll = 0; poll <= 9; poll++) {

            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("poll " + poll + " response: " + res.body());

            JsonObject body = gson.fromJson(res.body(), JsonObject.class);
            JsonArray events = body.getAsJsonArray("events");

            for (JsonElement el : events) {
                JsonObject event = el.getAsJsonObject();

                String roundId = event.get("roundId").getAsString();
                String participant = event.get("participant").getAsString();
                int score = event.get("score").getAsInt();

                // combining roundId and participant as a unique key
                String key = roundId + "|" + participant;

                if (processed.contains(key)) {
                    System.out.println("  skipping duplicate: " + key);
                    continue;
                }

                processed.add(key);

                // add score to existing or start from 0
                scores.put(participant, scores.getOrDefault(participant, 0) + score);
                System.out.println("  added " + participant + " score " + score);
            }

            // 5 second wait between polls as mentioned in the problem
            if (poll < 9) {
                System.out.println("waiting...");
                Thread.sleep(5000);
            }
        }

        // sort by total score high to low
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        System.out.println("\nleaderboard:");
        JsonArray leaderboardJson = new JsonArray();

        for (Map.Entry<String, Integer> entry : sorted) {
            System.out.println(entry.getKey() + " - " + entry.getValue());

            JsonObject row = new JsonObject();
            row.addProperty("participant", entry.getKey());
            row.addProperty("totalScore", entry.getValue());
            leaderboardJson.add(row);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("regNo", REG_NO);
        payload.add("leaderboard", leaderboardJson);

        System.out.println("\nsubmitting now...");

        HttpRequest submitReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> submitRes = client.send(submitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("result: " + submitRes.body());
    }
}
