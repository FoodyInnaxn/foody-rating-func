package com.function.foody.ratingfunc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

/**
 * Azure Functions with Timer trigger.
 */
public class TimerTriggerRating {
    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("TimerTriggerRating")
    public void run(
        @TimerTrigger(name = "timerInfo", schedule = "0 */5 * * * *") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("Recipe Rating Aggregation Function executed at: " + timerInfo);

               try {
            // Get all recipes
            URL recipesUrl = new URL("http://20.67.11.153/api/recipe/view");
            HttpURLConnection recipesConnection = (HttpURLConnection) recipesUrl.openConnection();
            recipesConnection.setRequestMethod("GET");
            recipesConnection.setRequestProperty("Content-Type", "application/json");

            if (recipesConnection.getResponseCode() == 200 || recipesConnection.getResponseCode() == 201) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(recipesConnection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray recipes = new JSONArray(response.toString());

                //Aggregate ratings and number saved for each recipe
                for (int i = 0; i < recipes.length(); i++) {
                    JSONObject recipe = recipes.getJSONObject(i);
                    String recipeId = recipe.getString("id");
                    double rating = recipe.getDouble("rating");
                    int numberSaved = recipe.getInt("numberSaved");

                    // Calculate new average rating using numberSaved
                    double newRating = (rating * numberSaved + numberSaved) / (numberSaved + 1);

                    // Update the recipe with the new average rating 
                    // recipe/" + recipeId + "/rating?rating=" + newRating
                    URL updateRecipeUrl = new URL("http://20.67.11.153/api/recipe/view/" + recipeId + "/rating?rating=" + newRating);
                    HttpURLConnection updateRecipeConnection = (HttpURLConnection) updateRecipeUrl.openConnection();
                    updateRecipeConnection.setRequestMethod("PUT");
                    updateRecipeConnection.setRequestProperty("Content-Type", "application/json");
                    updateRecipeConnection.setDoOutput(true);

                    JSONObject updateBody = new JSONObject();
                    updateBody.put("rating", newRating);

                    try (OutputStream os = updateRecipeConnection.getOutputStream()) {
                        byte[] input = updateBody.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    if (updateRecipeConnection.getResponseCode() != 204) {
                        context.getLogger().severe("Failed to update recipe: " + recipeId);
                    }

                    updateRecipeConnection.disconnect();
                }
            } else {
                context.getLogger().severe("Failed to get recipes");
            }

            recipesConnection.disconnect();
        } catch (Exception e) {
            context.getLogger().severe("Exception in Recipe Rating Aggregation Function: " + e.getMessage());
        }
    }
}
