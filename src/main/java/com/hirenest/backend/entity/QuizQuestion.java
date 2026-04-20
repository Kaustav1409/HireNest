package com.hirenest.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Domain / category (e.g. java, python, sql). */
    private String skill;
    /** Optional: Beginner, Intermediate, Advanced. */
    private String difficulty;
    @Column(length = 1000)
    private String question;
    /** Pipe-separated options (same as before). */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String optionsCsv;
    private Integer correctIndex;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getOptionsCsv() { return optionsCsv; }
    public void setOptionsCsv(String optionsCsv) { this.optionsCsv = optionsCsv; }
    public Integer getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(Integer correctIndex) { this.correctIndex = correctIndex; }
}

