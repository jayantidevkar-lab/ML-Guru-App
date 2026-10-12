package com.jayanti.mlguru;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Local Offline AI Brain for ML Guru.
 * Loads 100 detailed Machine Learning Q&A entries from assets/ml_knowledge.json.
 * Uses keyword scoring to return accurate answers.
 * Returns "I can only answer Machine Learning questions." if no ML match is found.
 */
public class MLAnswerEngine {

    private static final String TAG = "MLAnswerEngine";
    public static final String NON_ML_RESPONSE = "I can only answer Machine Learning questions.";

    public static class KnowledgeEntry {
        public String question;
        public List<String> keywords = new ArrayList<>();
        public String answer;
    }

    private final List<KnowledgeEntry> knowledgeList = new ArrayList<>();

    public MLAnswerEngine(Context context) {
        loadKnowledge(context);
    }

    public MLAnswerEngine(String jsonString) {
        parseJson(jsonString);
    }

    private static void logDebug(String msg) {
        try {
            Log.d(TAG, msg);
        } catch (Throwable ignored) {
            System.out.println(TAG + ": " + msg);
        }
    }

    private static void logError(String msg, Throwable t) {
        try {
            Log.e(TAG, msg, t);
        } catch (Throwable ignored) {
            System.err.println(TAG + ": " + msg + (t != null ? " (" + t.getMessage() + ")" : ""));
        }
    }

    /**
     * Loads knowledge base from assets/ml_knowledge.json.
     * Wrapped in try-catch so it never crashes if asset loading fails.
     */
    public synchronized void loadKnowledge(Context context) {
        if (!knowledgeList.isEmpty()) {
            return;
        }
        try {
            if (context == null || context.getAssets() == null) {
                logError("Context or Assets is null", null);
                return;
            }
            InputStream is = context.getAssets().open("ml_knowledge.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int read = is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, 0, read, StandardCharsets.UTF_8);
            parseJson(jsonStr);
        } catch (Exception e) {
            logError("Failed to load ml_knowledge.json from assets", e);
            // Don't crash, knowledgeList remains empty list
        }
    }

    public synchronized void parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(jsonStr);
            knowledgeList.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                KnowledgeEntry entry = new KnowledgeEntry();
                entry.question = obj.optString("question", "");
                entry.answer = obj.optString("answer", "");

                JSONArray kwArr = obj.optJSONArray("keywords");
                if (kwArr != null) {
                    for (int k = 0; k < kwArr.length(); k++) {
                        entry.keywords.add(kwArr.getString(k).trim().toLowerCase());
                    }
                }
                knowledgeList.add(entry);
            }
            logDebug("Loaded " + knowledgeList.size() + " ML knowledge entries.");
        } catch (Exception e) {
            logError("Failed to parse ml_knowledge.json", e);
        }
    }

    /**
     * Returns answer by checking query against keyword array with scoring.
     * If query contains any keyword from keywords array, score++.
     * Returns highest score answer.
     * If no ML keyword matches, returns "I can only answer Machine Learning questions."
     */
    public String getAnswer(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "Please ask a question related to Machine Learning.";
        }

        String lowerQuery = query.toLowerCase().trim();

        if (knowledgeList.isEmpty()) {
            return NON_ML_RESPONSE;
        }

        KnowledgeEntry bestEntry = null;
        int maxScore = 0;

        for (KnowledgeEntry entry : knowledgeList) {
            int score = 0;
            for (String kw : entry.keywords) {
                if (kw == null || kw.isEmpty()) continue;

                // Check if query contains keyword
                if (lowerQuery.contains(kw)) {
                    score++;
                    // Longer, more specific keyword matches get higher weight
                    if (kw.length() > 6) {
                        score += 2;
                    }
                    if (lowerQuery.equals(kw)) {
                        score += 5;
                    }
                } else if (kw.contains(lowerQuery) && lowerQuery.length() > 4) {
                    score++;
                }
            }

            // Also check question title match
            String lowerQuestion = entry.question != null ? entry.question.toLowerCase() : "";
            if (!lowerQuestion.isEmpty() && (lowerQuery.contains(lowerQuestion) || lowerQuestion.contains(lowerQuery))) {
                score += 3;
            }

            if (score > maxScore) {
                maxScore = score;
                bestEntry = entry;
            }
        }

        // If score is 0 or no ML word matched, return refusal
        if (maxScore == 0 || bestEntry == null) {
            return NON_ML_RESPONSE;
        }

        return bestEntry.answer;
    }

    public int getKnowledgeCount() {
        return knowledgeList.size();
    }
}
