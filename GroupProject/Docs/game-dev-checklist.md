Tetris Game Development Checklist

Setup
    - Project runs from GroupProject folder with mvn clean javafx:run
    - JavaFX window opens with no runtime errors

Core Game Logic
    - Board class:
        20×10 grid (int[][])
        Methods for get/set, inBounds, empty
        clearFullRows() removes single & multiple rows, shifts down, keeps colours
    - Tetromino enum:
        All 7 shapes (I,O,T,S,Z,J,L) with rotation matrices
        Methods for shape(rot), rotationCount(), colorId()
    - PieceState record:
        Holds Tetromino type, rotation, row, column

Gameplay Features
    - Spawn: new random tetromino at top; if blocked → Game Over state
    - Movement:
        LEFT/RIGHT: move piece if no collision
        UP: rotate piece (no rotation if collision)
        DOWN: soft drop by one row 
    - Collision rules:
        No going through walls, floor, or stacked blocks
    - Gravity:
        Piece drops automatically on timer
        Locks if cannot move down
    - Locking:
        Writes active piece to board grid with correct colour id
        Calls clearFullRows() before spawning next piece
    - Row clearing:
        Detects full rows
        Handles multiple clears at once
        Maintains correct colour arrangement
    - Pause:
        Press P to toggle pause state
        Shows “Paused” overlay when paused
    - Exit confirm:
        Yes = quit program
        No = return to main menu/game

UI Integration
    - GameView class:
        Extends Pane/Canvas
        Draws board grid and active piece
        Handles key inputs
        Uses AnimationTimer for game loop
    - Focus handling: requestFocus() ensures key input works
    - High Scores view: dummy list of 10 scores + Back button

Marking Criteria Code Features
    - Uses JavaFX for UI
    - Uses enhanced for loop (e.g., in clearFullRows)
    - Uses enhanced switch (e.g., key input handling)
    - Defines & uses an interface (e.g., Updatable)
    - Defines & uses an abstract class (e.g., AbstractScreen)
    - Uses a record (e.g., PieceState)