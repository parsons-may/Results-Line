package com.resultsline.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Loads the synthetic patient records and labeled caller questions from the classpath. */
public final class SyntheticDataLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private SyntheticDataLoader() {
    }

    public static List<PatientRecord> loadPatients() {
        return read("/data/patients.json", new TypeReference<List<PatientRecord>>() { });
    }

    public static List<CallerQuestion> loadQuestions() {
        return read("/data/questions.json", new TypeReference<List<CallerQuestion>>() { });
    }

    private static <T> T read(String resource, TypeReference<T> type) {
        try (InputStream in = SyntheticDataLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
    }
}
