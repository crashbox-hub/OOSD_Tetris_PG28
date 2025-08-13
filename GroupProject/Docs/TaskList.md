Board Class
    - Create grid storage
        Add int[][] cells = new int(20)(10).
    - Add getters/setters
        Methods to get and set cell values.
    - Add boundary check
        inBounds(r,c) returns true if inside grid.
    - Add empty check
        empty(r,c) returns true if cell is 0.
    - Add clear full rows method
        Detect and remove any full rows, shift above rows down, return number cleared.

Tetromino Enum
    - Define shapes
        Add 7 tetromino constants: I,O,T,S,Z,J,L.
    - Add rotation matrices
        Store each tetromino’s rotation patterns in a 2D/3D array.
    - Add helper methods
        shape(rot), rotationCount(), and colorId().

PieceState Record
    - Define record
        Holds: Tetromino type, int rot, int row, int col.

GameView – Base Setup
    - Create canvas
        Size: 10 * TILE wide, 20 * TILE tall.
    - Draw empty grid
        Fill background and draw grid lines.
    - Add focus handling
        Call setFocusTraversable(true) and requestFocus().

Gameplay – Piece Management
    - Spawn method
        Random tetromino at row=0, col=3.
    - Collision check method
        Return false if piece is out of bounds or overlaps another block.
    - Movement method
        Move left/right/down or rotate if valid.

Gameplay – Loop & Gravity
    - Add AnimationTimer
        Runs game logic every frame (~60fps).
    - Gravity drop
        Drop active piece every set interval. 
    - Lock piece to board
        When piece can’t move down, copy it to grid, then call clearFullRows().

Gameplay – Extra Features
    - Pause toggle
        Press P to pause/unpause; overlay “Paused”.
    - Exit confirmation
        Show confirmation dialog with Yes/No options.

High Scores
    - ListView with dummy data
        Show top 10 scores (fake values).
    - Back button
        Returns to previous screen.