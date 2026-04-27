package com.ragask.ticketing.knowledge;

import java.util.Random;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    public static final int DIMENSION = 1536;

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModel = embeddingModelProvider.getIfAvailable();
    }

    public float[] embed(String text) {
        float[] online = tryOnlineEmbedding(text);
        if (online != null) {
            return normalize(online);
        }
        Random random = new Random(text == null ? 0 : text.hashCode());
        float[] vector = new float[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) {
            float value = (float) (random.nextDouble() * 2 - 1);
            vector[i] = value;
        }
        return normalize(vector);
    }

    public boolean onlineAvailable() {
        return embeddingModel != null;
    }

    private float[] tryOnlineEmbedding(String text) {
        if (embeddingModel == null) {
            return null;
        }
        try {
            return embeddingModel.embed(text == null ? "" : text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private float[] normalize(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        double length = Math.sqrt(norm);
        if (length == 0) {
            return vector;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / length);
        }
        return vector;
    }

    public String toPgVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}
