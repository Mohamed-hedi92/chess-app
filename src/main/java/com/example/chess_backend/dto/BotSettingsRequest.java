package com.example.chess_backend.dto;

public class BotSettingsRequest {

    private Integer difficulty; // 1 = Leicht, 2 = Mittel, 3 = Schwer
    private Boolean enabled;    // Bot ein/aus

    public BotSettingsRequest() {}

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
