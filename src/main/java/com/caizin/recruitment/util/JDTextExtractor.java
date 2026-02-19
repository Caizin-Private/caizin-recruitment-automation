package com.caizin.recruitment.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JDTextExtractor {

    private final ResumeTextExtractor textExtractor;

    // Cache JD text per jobId
    private final ConcurrentHashMap<String, String> jdCache =
            new ConcurrentHashMap<>();

    public JDTextExtractor(ResumeTextExtractor textExtractor) {
        this.textExtractor = textExtractor;
    }

    /**
     * Get JD text based on jobId
     * Example: jd/JOB123.pdf
     */
    public String getJDText(String jobOpeningId) {

        // return cached version if exists
        if (jdCache.containsKey(jobOpeningId)) {
            return jdCache.get(jobOpeningId);
        }

        try {

            String jdPath = "jd/" + jobOpeningId + ".pdf";

            File jdFile =
                    new ClassPathResource(jdPath).getFile();

            String jdText =
                    textExtractor.extractText(jdFile);

            if (jdText == null || jdText.isBlank()) {

                throw new RuntimeException(
                        "JD text empty for jobId: " + jobOpeningId
                );
            }

            jdCache.put(jobOpeningId, jdText);

            return jdText;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to load JD for jobId: " + jobOpeningId,
                    e
            );
        }
    }

    /**
     * Force reload JD for specific jobId
     */
    public void reloadJD(String jobId) {

        jdCache.remove(jobId);
    }

    /**
     * Clear entire cache
     */
    public void clearCache() {

        jdCache.clear();
    }
}