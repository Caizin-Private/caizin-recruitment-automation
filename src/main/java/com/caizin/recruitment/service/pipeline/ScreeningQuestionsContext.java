package com.caizin.recruitment.service.pipeline;

import com.caizin.recruitment.dto.ScreeningQuestionDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScreeningQuestionsContext {

    private List<ScreeningQuestionDto> questions = new ArrayList<>();

    public void setQuestions(List<ScreeningQuestionDto> questions) {
        this.questions = questions != null ? questions : new ArrayList<>();
    }

    public List<ScreeningQuestionDto> getQuestions() {
        return questions;
    }

    public boolean hasQuestions() {
        return questions != null && !questions.isEmpty();
    }

    public void clear() {
        this.questions = new ArrayList<>();
    }
}