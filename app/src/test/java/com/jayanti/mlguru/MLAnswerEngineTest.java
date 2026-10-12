package com.jayanti.mlguru;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MLAnswerEngineTest {

    private MLAnswerEngine engine;

    @Before
    public void setUp() throws Exception {
        File file = new File("src/main/assets/ml_knowledge.json");
        if (!file.exists()) {
            file = new File("app/src/main/assets/ml_knowledge.json");
        }
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        int read = fis.read(buffer);
        fis.close();
        String jsonStr = new String(buffer, 0, read, StandardCharsets.UTF_8);

        engine = new MLAnswerEngine(jsonStr);
    }

    @Test
    public void testKnowledgeCountIs100() {
        assertEquals("Knowledge base must contain exactly 100 entries", 100, engine.getKnowledgeCount());
    }

    @Test
    public void testTypesOfMachineLearning() {
        String answer = engine.getAnswer("types of machine learning");
        assertNotNull(answer);
        assertTrue("Answer must mention Supervised Learning", answer.contains("Supervised Learning"));
        assertTrue("Answer must mention Unsupervised Learning", answer.contains("Unsupervised Learning"));
        assertTrue("Answer must mention Reinforcement Learning", answer.contains("Reinforcement Learning"));
        assertTrue("Answer must mention 3 main types", answer.contains("3 main types"));
    }

    @Test
    public void testCNNQuestion() {
        String answer = engine.getAnswer("What is CNN?");
        assertNotNull(answer);
        assertTrue("Answer should contain Convolutional Neural Network", answer.contains("Convolutional Neural Network"));
    }

    @Test
    public void testOverfittingQuestion() {
        String answer = engine.getAnswer("Explain overfitting");
        assertNotNull(answer);
        assertTrue("Answer should explain overfitting", answer.toLowerCase().contains("overfitting"));
    }

    @Test
    public void testNonMLQuestionsReturnRefusal() {
        String cricket = engine.getAnswer("What is cricket?");
        assertEquals(MLAnswerEngine.NON_ML_RESPONSE, cricket);

        String cake = engine.getAnswer("Tell me how to bake a chocolate cake");
        assertEquals(MLAnswerEngine.NON_ML_RESPONSE, cake);

        String weather = engine.getAnswer("Will it rain tomorrow?");
        assertEquals(MLAnswerEngine.NON_ML_RESPONSE, weather);
    }
}
