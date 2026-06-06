package com.example.chess_backend.domain;



public record Position(int row, int col) {

    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}
