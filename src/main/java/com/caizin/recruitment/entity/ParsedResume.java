package com.caizin.recruitment.entity;

import java.util.List;

public record ParsedResume(

        String fullName,
        String email,
        List<String> skills,
        double yearsOfExperience,
        List<String> projects,
        int wordCount

) {}