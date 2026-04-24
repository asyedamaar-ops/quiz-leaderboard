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
        Set<String> processed = new HashSet<>();
        Map<String, Integer> scores = new LinkedHashMap<>();
        System.out.println("Starting poll sequence...");
        for (int poll = 0; poll <= 9; poll++) {
            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("Poll " + poll + " -> " + res.body());
            JsonObject body = gson.fromJson(res.body(), JsonObject.class);
            JsonArray events = body.getAsJsonArray("events");
            for (JsonElement el : events) {
                JsonObject event = el.getAsJsonObject();
                String roundId = event.get("roundId").getAsString();
                String participant = event.get("participant").getAsString();
                int score = event.get("score").getAsInt();
                String key = roundId + "|" + participant;
                if (processed.contains(key)) {
                    System.out.println("  [SKIP] duplicate -> " + key);
                    continue;
                }
                processed.add(key);
                scores.merge(participant, score, Integer::sum);
                System.out.println("  [ADD]  " + participant + " +" + score);
            }
            if (poll < 9) {
                System.out.println("  waiting 5 seconds...");
                Thread.sleep(5000);
            }
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        System.out.println("\n--- Final Leaderboard ---");
        JsonArray leaderboardJson = new JsonArray();
        for (Map.Entry<String, Integer> entry : sorted) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
            JsonObject row = new JsonObject();
            row.addProperty("participant", entry.getKey());
            row.addProperty("totalScore", entry.getValue());
            leaderboardJson.add(row);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("regNo", REG_NO);
        payload.add("leaderboard", leaderboardJson);
        System.out.println("\nSubmitting: " + payload);
        HttpRequest submitReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        HttpResponse<String> submitRes = client.send(submitReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nResult: " + submitRes.body());
    }
}
