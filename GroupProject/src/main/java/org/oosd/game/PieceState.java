package org.oosd.game;

/**
 * Represents the current state of a piece on the board:
 * - Which piece type
 * - Its rotation
 * - Its position (row and column)
 */
public record PieceState(Tetromino type, int rot, int row, int col) { }
