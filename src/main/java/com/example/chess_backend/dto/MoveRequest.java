package com.example.chess_backend.dto;

public class MoveRequest {

    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private String promotionType; // QUEEN, ROOK, BISHOP, KNIGHT - null = auto Queen

    public MoveRequest() {}

    public int getFromRow() { return fromRow; }
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }

    public int getFromCol() { return fromCol; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }

    public int getToRow() { return toRow; }
    public void setToRow(int toRow) { this.toRow = toRow; }

    public int getToCol() { return toCol; }
    public void setToCol(int toCol) { this.toCol = toCol; }

    public String getPromotionType() { return promotionType; }
    public void setPromotionType(String promotionType) { this.promotionType = promotionType; }
}
