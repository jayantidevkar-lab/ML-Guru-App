package com.jayanti.mlguru;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.jayanti.mlguru.adapter.ChatAdapter;
import com.jayanti.mlguru.database.ChatDatabase;
import com.jayanti.mlguru.database.ChatHistory;
import com.jayanti.mlguru.model.ChatMessage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Crash-Proof MainActivity for ML Guru.
 * Pure Java implementation.
 * Features intelligent image text extraction that filters out status bar, battery,
 * time, and app UI, and guarantees ML Guru answers for all image queries.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerViewChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageView btnAttachImage;
    private ImageView btnAttachDoc;
    private ImageView btnHistory;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private MLAnswerEngine answerEngine;
    private ChatDatabase chatDatabase;
    private final Gson gson = new Gson();

    private long currentSessionId = -1;
    private String currentSessionTitle = null;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String[]> docPickerLauncher;
    private ActivityResultLauncher<Intent> historyLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            // 1. findViewById for all UI components
            initViews();

            // 2. Initialize RecyclerView BEFORE engine
            setupRecyclerView();

            // 3. Room DB initialization with allowMainThreadQueries() and fallbackToDestructiveMigration()
            initDatabase();

            // 4. Initialize MLAnswerEngine
            initEngine();

            // 5. Setup Activity Result Launchers
            setupLaunchers();

            // 6. Setup Click Listeners in a separate method
            setupClickListeners();

            // 7. Load previous chat history if available
            loadExistingChat();

        } catch (Exception e) {
            Log.e(TAG, "Critical error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        try {
            recyclerViewChat = findViewById(R.id.recyclerViewChat);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);
            btnAttachImage = findViewById(R.id.btnAttachImage);
            btnAttachDoc = findViewById(R.id.btnAttachDoc);
            btnHistory = findViewById(R.id.btnHistory);
        } catch (Exception e) {
            Log.e(TAG, "Error finding views", e);
            Toast.makeText(this, "Error binding views: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        try {
            if (recyclerViewChat != null) {
                LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                layoutManager.setStackFromEnd(true);
                recyclerViewChat.setLayoutManager(layoutManager);

                chatAdapter = new ChatAdapter(this, messageList);
                recyclerViewChat.setAdapter(chatAdapter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            Toast.makeText(this, "Error setting up chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initDatabase() {
        try {
            chatDatabase = Room.databaseBuilder(
                            getApplicationContext(),
                            ChatDatabase.class,
                            "ml_db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Room DB", e);
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initEngine() {
        try {
            answerEngine = new MLAnswerEngine(this);
            Log.d(TAG, "MLAnswerEngine initialized with " + answerEngine.getKnowledgeCount() + " topics.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MLAnswerEngine", e);
            Toast.makeText(this, "Failed to load ML brain: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLaunchers() {
        try {
            imagePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            handleImageSelection(uri);
                        }
                    }
            );

            docPickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            handleDocumentSelection(uri);
                        }
                    }
            );

            historyLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            long chatId = result.getData().getLongExtra(HistoryActivity.EXTRA_LOAD_CHAT_ID, -1);
                            if (chatId != -1) {
                                loadChatSession(chatId);
                            }
                        }
                    }
            );

            permissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            try {
                                imagePickerLauncher.launch("image/*");
                            } catch (Exception e) {
                                Log.e(TAG, "Error launching image picker", e);
                            }
                        } else {
                            Toast.makeText(this, "Permission required to access images", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error setting up launchers", e);
        }
    }

    /**
     * Setup click listeners in a separate method as required.
     */
    private void setupClickListeners() {
        try {
            // Send Button Click
            if (btnSend != null) {
                btnSend.setOnClickListener(v -> {
                    try {
                        if (etMessage != null) {
                            String query = etMessage.getText().toString().trim();
                            if (!query.isEmpty()) {
                                sendMessage(query, null);
                                etMessage.setText("");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending message", e);
                        Toast.makeText(this, "Error sending: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Attach Image Click
            if (btnAttachImage != null) {
                btnAttachImage.setOnClickListener(v -> checkAndPickImage());
            }

            // Attach Document Click
            if (btnAttachDoc != null) {
                btnAttachDoc.setOnClickListener(v -> pickDocument());
            }

            // History Button Click
            if (btnHistory != null) {
                btnHistory.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                        historyLauncher.launch(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening history", e);
                        Toast.makeText(this, "Error opening history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupClickListeners", e);
            Toast.makeText(this, "Error setting up listeners: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndPickImage() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error picking image", e);
            Toast.makeText(this, "Image picker error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickDocument() {
        try {
            docPickerLauncher.launch(new String[]{
                    "application/pdf",
                    "text/plain",
                    "*/*"
            });
        } catch (Exception e) {
            Log.e(TAG, "Error picking doc", e);
            Toast.makeText(this, "Document picker error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String userText, String imageUri) {
        try {
            long now = System.currentTimeMillis();

            // 1. Add User Message
            ChatMessage userMsg = new ChatMessage(ChatMessage.SENDER_USER, userText, now, imageUri);
            messageList.add(userMsg);
            if (chatDatabase != null) {
                try {
                    chatDatabase.chatDao().insertMessage(userMsg);
                } catch (Exception ignored) {}
            }
            if (chatAdapter != null) {
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            }
            if (recyclerViewChat != null) {
                recyclerViewChat.scrollToPosition(messageList.size() - 1);
            }

            // If an image or document is attached, the OCR/file reader will process it
            // and post the bot response asynchronously. Do not post an immediate refusal.
            if (imageUri != null || (userText != null && userText.startsWith("Attached Document:"))) {
                return;
            }

            if (currentSessionTitle == null) {
                currentSessionTitle = userText.length() > 30 ? userText.substring(0, 30) + "…" : userText;
            }

            // 2. Query MLAnswerEngine for text message
            String botAnswer;
            if (answerEngine != null) {
                botAnswer = answerEngine.getAnswer(userText);
            } else {
                botAnswer = "I can only answer Machine Learning questions.";
            }

            // 3. Add Bot Message
            ChatMessage botMsg = new ChatMessage(ChatMessage.SENDER_BOT, botAnswer, System.currentTimeMillis());
            messageList.add(botMsg);
            if (chatDatabase != null) {
                try {
                    chatDatabase.chatDao().insertMessage(botMsg);
                } catch (Exception ignored) {}
            }
            if (chatAdapter != null) {
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            }
            if (recyclerViewChat != null) {
                recyclerViewChat.scrollToPosition(messageList.size() - 1);
            }

            // Save conversation session to history
            saveSessionHistory();

        } catch (Exception e) {
            Log.e(TAG, "Error in sendMessage", e);
            Toast.makeText(this, "Error processing query: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles image selection: extracts main content while ignoring status bar,
     * battery, time, and app UI, and guarantees an ML Guru answer.
     */
    private void handleImageSelection(Uri imageUri) {
        try {
            Toast.makeText(this, R.string.processing_image, Toast.LENGTH_SHORT).show();
            sendMessage("Image Attached", imageUri.toString());

            InputImage image = InputImage.fromFilePath(this, imageUri);
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        try {
                            String mainContent = extractMainContent(visionText, imageWidth, imageHeight);
                            if (mainContent != null && !mainContent.trim().isEmpty()) {
                                String botAnswer = null;
                                if (answerEngine != null) {
                                    botAnswer = answerEngine.getAnswer(mainContent);
                                }

                                // For image questions, always answer as ML Guru, never refuse!
                                if (botAnswer == null || botAnswer.equals(MLAnswerEngine.NON_ML_RESPONSE)) {
                                    botAnswer = generateMLGuruImageResponse(mainContent);
                                }

                                addBotMessage("Extracted: \"" + mainContent.trim() + "\"\n\n" + botAnswer);
                            } else {
                                addBotMessage(generateMLGuruNoTextResponse());
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error processing OCR success callback", ex);
                            addBotMessage(generateMLGuruImageResponse("Attached Image"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR Recognition error: ", e);
                        addBotMessage(generateMLGuruImageResponse("Attached Image"));
                    });

        } catch (Exception e) {
            Log.e(TAG, "Failed to load image for OCR", e);
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Filters out status bar (time, battery, carrier) and app UI ("Ask anything about ML...", "ML Guru")
     * to extract only the main content from the image.
     */
    private String extractMainContent(Text visionText, int imageWidth, int imageHeight) {
        if (visionText == null) return "";

        StringBuilder sb = new StringBuilder();
        List<Text.TextBlock> blocks = visionText.getTextBlocks();
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        for (Text.TextBlock block : blocks) {
            if (block == null) continue;
            for (Text.Line line : block.getLines()) {
                if (line == null) continue;
                String text = line.getText();
                if (text == null || text.trim().isEmpty()) continue;
                String trimmed = text.trim();

                // Spatial filtering using bounding boxes when available
                Rect box = line.getBoundingBox();
                if (box != null && imageHeight > 0) {
                    // Ignore top ~6.5% of the image (Status Bar region) if it matches status bar items
                    if (box.bottom < imageHeight * 0.065 && isStatusBarText(trimmed)) {
                        continue;
                    }
                    // Ignore bottom ~8% of the image (Chat bar / navigation) if it matches UI items
                    if (box.top > imageHeight * 0.92 && isAppUiText(trimmed)) {
                        continue;
                    }
                }

                // Content-based filtering regardless of position
                if (isStatusBarText(trimmed)) {
                    continue;
                }

                if (isAppUiText(trimmed)) {
                    continue;
                }

                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(trimmed);
            }
        }

        String result = sb.toString().trim();

        // Fallback: if spatial/line filtering was too strict, parse raw text with regex filters
        if (result.isEmpty()) {
            String raw = visionText.getText();
            if (raw != null) {
                String[] lines = raw.split("\n");
                StringBuilder fallbackSb = new StringBuilder();
                for (String l : lines) {
                    String t = l.trim();
                    if (!isStatusBarText(t) && !isAppUiText(t)) {
                        if (fallbackSb.length() > 0) fallbackSb.append(" ");
                        fallbackSb.append(t);
                    }
                }
                result = fallbackSb.toString().trim();
            }
        }

        return result;
    }

    /**
     * Detects status bar text such as time (10:30, 9:41 AM), battery (85%), network icons (5G, LTE, Wi-Fi), carrier names.
     */
    private boolean isStatusBarText(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return true;

        // Time format: 10:30, 9:41 AM, 23:59, 12:00 PM
        if (trimmed.matches("(?i)^[0-2]?\\d[:.][0-5]\\d(\\s*[ap]m)?$")) {
            return true;
        }

        // Battery percentage: 100%, 85%, 5%
        if (trimmed.matches("^\\d{1,3}\\s*%$")) {
            return true;
        }

        // Network, Carrier, Wi-Fi, and System indicators
        if (trimmed.matches("(?i)^(5g|4g\\+?|3g|lte|volte|vowifi|wi-?fi|jio|airtel|vi|bsnl|verizon|at&t|sprint|t-mobile|vodafone)$")) {
            return true;
        }

        // Speed readings: 0.0 KB/s, 1.2 MB/s
        if (trimmed.matches("(?i)^[0-9.]+\\s*(k|m)?b/s$")) {
            return true;
        }

        // Composite status bar line: e.g. "10:30 5G 85%" or "9:41 LTE"
        String lower = trimmed.toLowerCase();
        String withoutTokens = lower
                .replaceAll("[0-2]?\\d[:.][0-5]\\d(\\s*[ap]m)?", "")
                .replaceAll("\\d{1,3}\\s*%", "")
                .replaceAll("5g|4g\\+?|3g|lte|volte|vowifi|wi-?fi|jio|airtel|vi|bsnl|verizon|at&t|sprint", "")
                .replaceAll("[0-9.]+\\s*(k|m)?b/s", "")
                .replaceAll("[|·•\\s/\\\\-]", "");
        if (withoutTokens.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Detects App UI elements like "Ask anything about ML...", "ML Guru", "Send", etc.
     */
    private boolean isAppUiText(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        if (lower.isEmpty()) return true;

        if (lower.contains("ask anything about ml") ||
            lower.contains("ask anything about machine learning") ||
            lower.contains("ask anything") ||
            lower.equals("ml guru") ||
            lower.equals("send") ||
            lower.equals("attach") ||
            lower.equals("attach image") ||
            lower.equals("attach document") ||
            lower.equals("image attached") ||
            lower.equals("attached document") ||
            lower.equals("conversation history") ||
            lower.equals("history") ||
            lower.equals("try asking:") ||
            lower.equals("try asking") ||
            lower.matches("(?i)^type (a )?message.*$")) {
            return true;
        }
        return false;
    }

    /**
     * Always answers as ML Guru for image questions rather than refusing.
     */
    private String generateMLGuruImageResponse(String extractedContent) {
        if (extractedContent == null || extractedContent.trim().isEmpty()) {
            return generateMLGuruNoTextResponse();
        }

        return "Hello! As your ML Guru, I analyzed the main content from your image:\n\n"
                + "\"" + extractedContent.trim() + "\"\n\n"
                + "From a Machine Learning and Data Science perspective, understanding real-world visual content involves Computer Vision and Natural Language Processing (NLP). In machine learning pipelines, images are processed using Convolutional Neural Networks (CNNs) and Vision Transformers (ViT) to extract features, while Optical Character Recognition (OCR) translates visual glyphs into semantic text tokens. If you would like to explore how Machine Learning algorithms (such as classification, clustering, regression, neural networks, or deep learning pipelines) apply to this concept, feel free to ask me to explain!";
    }

    private String generateMLGuruNoTextResponse() {
        return "Hello! As your ML Guru, I inspected the attached image. Although no readable text was detected by the OCR engine, in Machine Learning visual analysis is performed using deep architectures such as Convolutional Neural Networks (CNNs), ResNet, and Vision Transformers (ViT) that classify images, detect objects, and segment visual regions directly from pixel tensors. Feel free to ask any question about Machine Learning or upload an image with clear text!";
    }

    private void handleDocumentSelection(Uri docUri) {
        try {
            Toast.makeText(this, R.string.processing_doc, Toast.LENGTH_SHORT).show();
            String fileName = getFileName(docUri);
            sendMessage("Attached Document: " + fileName, null);

            String mimeType = getContentResolver().getType(docUri);
            String extractedText = "";

            if (mimeType != null && mimeType.contains("pdf")) {
                extractedText = extractTextFromPdf(docUri);
            } else {
                extractedText = readTextFromStream(docUri);
            }

            String query = (extractedText != null && !extractedText.trim().isEmpty())
                    ? extractedText
                    : cleanFileName(fileName);

            String botAnswer = answerEngine != null
                    ? answerEngine.getAnswer(query)
                    : "I can only answer Machine Learning questions.";

            addBotMessage(botAnswer);

        } catch (Exception e) {
            Log.e(TAG, "Error reading document", e);
            Toast.makeText(this, "Error reading document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return "Machine Learning";
        int lastDot = fileName.lastIndexOf('.');
        String name = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        return name.replace('_', ' ').replace('-', ' ').trim();
    }

    private String extractTextFromPdf(Uri pdfUri) {
        StringBuilder sb = new StringBuilder();
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(pdfUri, "r");
            if (pfd != null) {
                PdfRenderer renderer = new PdfRenderer(pfd);
                int pagesToRender = Math.min(renderer.getPageCount(), 2);
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                for (int i = 0; i < pagesToRender; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();

                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                    try {
                        com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> task = recognizer.process(inputImage);
                        com.google.mlkit.vision.text.Text result = com.google.android.gms.tasks.Tasks.await(task);
                        if (result != null) {
                            sb.append(result.getText()).append("\n");
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "PDF OCR error on page " + i, ex);
                    }
                    bitmap.recycle();
                }
                renderer.close();
                pfd.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "PDF extraction error", e);
        }
        return sb.toString();
    }

    private String readTextFromStream(Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 100) {
                sb.append(line).append("\n");
                count++;
            }
        } catch (Exception e) {
            Log.e(TAG, "Stream read error", e);
        }
        return sb.toString();
    }

    private String getFileName(Uri uri) {
        String result = "document";
        try {
            if ("content".equals(uri.getScheme())) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index >= 0) {
                            result = cursor.getString(index);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private void addBotMessage(String text) {
        try {
            ChatMessage botMsg = new ChatMessage(ChatMessage.SENDER_BOT, text, System.currentTimeMillis());
            messageList.add(botMsg);
            if (chatDatabase != null) {
                chatDatabase.chatDao().insertMessage(botMsg);
            }
            if (chatAdapter != null) {
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            }
            if (recyclerViewChat != null) {
                recyclerViewChat.scrollToPosition(messageList.size() - 1);
            }
            saveSessionHistory();
        } catch (Exception e) {
            Log.e(TAG, "Error adding bot message", e);
        }
    }

    private void loadExistingChat() {
        try {
            if (chatDatabase != null) {
                List<ChatMessage> existing = chatDatabase.chatDao().getAllMessages();
                if (existing != null && !existing.isEmpty()) {
                    messageList.clear();
                    messageList.addAll(existing);
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                    if (recyclerViewChat != null) {
                        recyclerViewChat.scrollToPosition(messageList.size() - 1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading existing messages", e);
        }
    }

    private void saveSessionHistory() {
        if (chatDatabase == null || messageList.isEmpty()) return;
        try {
            String json = gson.toJson(messageList);
            String title = currentSessionTitle != null ? currentSessionTitle : "ML Chat Session";
            if (currentSessionId == -1) {
                ChatHistory history = new ChatHistory(title, System.currentTimeMillis(), json);
                currentSessionId = chatDatabase.chatDao().insertChat(history);
            } else {
                ChatHistory history = chatDatabase.chatDao().getChatById(currentSessionId);
                if (history != null) {
                    history.setTitle(title);
                    history.setTime(System.currentTimeMillis());
                    history.setChatJson(json);
                    chatDatabase.chatDao().updateChat(history);
                } else {
                    ChatHistory newHist = new ChatHistory(title, System.currentTimeMillis(), json);
                    currentSessionId = chatDatabase.chatDao().insertChat(newHist);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving session history", e);
        }
    }

    private void loadChatSession(long chatId) {
        if (chatDatabase == null) return;
        try {
            ChatHistory history = chatDatabase.chatDao().getChatById(chatId);
            if (history != null && history.getChatJson() != null) {
                Type type = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
                List<ChatMessage> loaded = gson.fromJson(history.getChatJson(), type);
                if (loaded != null && !loaded.isEmpty()) {
                    messageList.clear();
                    messageList.addAll(loaded);
                    currentSessionId = history.getId();
                    currentSessionTitle = history.getTitle();
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                    if (recyclerViewChat != null) {
                        recyclerViewChat.scrollToPosition(messageList.size() - 1);
                    }
                    Toast.makeText(this, "Loaded: " + history.getTitle(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading chat session", e);
            Toast.makeText(this, "Failed to load chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
