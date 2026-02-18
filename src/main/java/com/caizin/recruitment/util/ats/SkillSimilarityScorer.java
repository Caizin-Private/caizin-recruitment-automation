package com.caizin.recruitment.util.ats;

import org.springframework.stereotype.Component;

@Component
public class SkillSimilarityScorer {

    private final CosineSimilarityCalculator cosine;

    public SkillSimilarityScorer(
            CosineSimilarityCalculator cosine){

        this.cosine = cosine;
    }

    public double score(
            String resumeText,
            String jdText){

        return cosine.calculate(
                resumeText, jdText)*100;
    }
}