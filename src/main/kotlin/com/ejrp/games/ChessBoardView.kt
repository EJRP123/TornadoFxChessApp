package com.ejrp.games

import com.ejrp.chess.board.*
import com.ejrp.chess.piece.*
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.RowConstraints
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.stage.StageStyle
import tornadofx.*

class ChessView : View("Chess") {

    private val board = find<ChessBoardView>(mapOf(
        ChessBoardView::color1 to Color.rgb(235, 235, 208),
        ChessBoardView::color2 to Color.rgb(119, 148, 85),
        ChessBoardView::size to 512.0))

    private val pieceInformation = find<PieceInformation>()

    override val root: Parent = borderpane {
        primaryStage.isAlwaysOnTop = true
        setPrefSize(1000.0,512.0)
        style {
            backgroundColor.add(Color.BLACK)
        }
        left = board.root
        right = pieceInformation.root
    }
}

class ChessBoardView : View(), ChessBoard {

    val color1 : Color by param()
    val color2 : Color by param()
    val size : Double by param()

    private val board = gridpane {
        setMinSize(64.0,64.0)
        setPrefSize(size,size)
        val rowCount = 8
        val rc = RowConstraints()
        rc.percentHeight = 100.0 / rowCount
        for (i in 0 until rowCount) {
            addRow(i)
            rowConstraints.add(rc)
        }

        val columnCount = 8
        val cc = ColumnConstraints()
        cc.percentWidth = 100.0 / columnCount
        for (i in 0 until columnCount) {
            addColumn(i)
            columnConstraints.add(cc)
        }

        shortcut("Ctrl+Z") {
            removeColoredSquare()
            try { internalBoard.undoLastMove(); }
            catch (e : NoSuchElementException) { println("You are back to that beginning!") }
        }
    }

    private val images = arrayOfNulls<ImageView?>(64)
    override val internalBoard = InternalBoard(STARTING_BOARD_FEN_STRING,this)
    override val root: GridPane = board

    private val coloredSquare : MutableList<Rectangle> = ArrayList()

    init {
        drawBoard(color1, color2, size / 8)
        drawPieces(internalBoard.getAllPieces().toTypedArray())
    }

    private fun drawBoard(color1: Color, color2: Color, squareSize: Double) {
        var changeColor = false
        for (i in 0..63) {
            if (i % 8 == 0) changeColor = !changeColor
            val color = if (i % 2 == 0)
                if (changeColor) color1 else color2
            else
                if (changeColor) color2 else color1
            val square = getSquare(squareSize, color)
            val label = Label((i).toString())
            square.add(label)
            square.onMouseClicked = EventHandler {
                var white = false
                var black = false
                internalBoard.getAllPieces().filterIsInstance<King>()
                    .forEach { king ->
                        if (king.getColor() == PieceColor.WHITE) white = king.isIndexLegal(i) else black = king.isIndexLegal(i)
                    }
                find<PieceInformation>().isSquareSafe(white, black)
            }
            board.add(square, i % 8, i / 8)
        }
    }

    private fun getSquare(size: Double, color: Color) : Pane {
        val p = Pane()
        p.setMinSize(size,size)
        p.setPrefSize(size,size)
        p.style { backgroundColor.add(color) }
        return p
    }

    private fun getImage(size: Double, piece: ChessPiece) : ImageView {
        return formatImage(ImageView(getImagePath("${piece.getName().lowercase().replace(" ","_")}.png")),size,piece)
    }

    private fun getImagePath(imageName : String) = "/Images/Chess/$imageName"

    private fun formatImage(imageView : ImageView, size : Double, piece: ChessPiece) : ImageView {
        imageView.isPreserveRatio = true
        imageView.minHeight(8.0)
        imageView.minWidth(8.0)
        imageView.prefHeight(size)
        imageView.prefHeight(size)
        imageView.fitHeight = size
        imageView.fitWidth = size
        imageView.onMouseClicked = EventHandler {
            removeColoredSquare()
            val moves = piece.getLegalMoves()
            for (move in moves) colorSquare(move, getColorForMoves(move))
            find<PieceInformation>().createPieceInfo(piece)
        }
        return imageView
    }

    private fun getColorForMoves(move: Move) : Color {
        return when (move) {
            is Neutral, is DoublePawn -> Color.AQUA
            is Capture, is EnPassant -> Color.RED
            is Promotion -> Color.GREEN
            is Castling -> Color.DARKBLUE
        }
    }

    private fun removeColoredSquare() {
        board.children.removeIf { node -> coloredSquare.contains(node) }
        coloredSquare.clear()
    }

    private fun colorSquare(move : Move, color : Color) {
        val realColor = Color(color.red, color.green, color.blue, 0.5)
        val rect = Rectangle()
        rect.width = size / 8
        rect.height = size / 8
        rect.fill = realColor
        rect.setOnMouseClicked {
            if (move is Promotion) {
                val piece = internalBoard.getPiecesAt(move.squareToMoveFrom) as Pawn
                val promotionPrompt = find<PromotionPrompt>(mapOf(PromotionPrompt::pieceColor to piece.getColor()))
                promotionPrompt.openModal(stageStyle = StageStyle.UTILITY, block = true)
                val char = promotionPrompt.pieceChar
                try { internalBoard.executePromotionMoveForPawn(piece,char, move.squareToMoveTo) }
                catch (e : NotRightTurnException ) { println("It is not your turn to go!"); return@setOnMouseClicked }
                catch (_: IndexOutOfBoundsException) {  }
            } else {
                try { internalBoard.makeMove(move) }
                catch (e: NotRightTurnException) { println("It is not your turn to go!"); return@setOnMouseClicked }
            }
            find<PieceInformation>().createPieceInfo(internalBoard.getPiecesAt(move.squareToMoveTo)!!)
        }
        board.add(rect,move.squareToMoveTo % 8, move.squareToMoveTo / 8)
        coloredSquare.add(rect)
    }

    override fun drawPieces(pieces: Array<ChessPiece?>) {
        for (i in 0..63) removePieceAt(i)
        for (piece in pieces) {
            if (piece == null) continue
            val image = getImage(size / 8, piece)
            images[piece.getBoardPosition()] = image
            board.add(image, piece.getBoardPosition() % 8, piece.getBoardPosition() / 8)
        }
    }

    override fun addPiece(squareToAddPiece: Int, piece : ChessPiece) {
        removeColoredSquare()
        images[squareToAddPiece] = getImage(size / 8, piece)
        board.add(images[squareToAddPiece],squareToAddPiece % 8, squareToAddPiece / 8)
    }

    override fun removePieceAt(position: Int) {
        board.children.remove(images[position])
        images[position] = null
    }

    override fun draw() { find<GameEndView>(mapOf(GameEndView::winner to null, GameEndView::internalBoard to internalBoard)).openModal() }

    override fun winner(winner: PieceColor) { find<GameEndView>(mapOf(GameEndView::winner to winner, GameEndView::internalBoard to internalBoard)).openModal() }
}

class PieceInformation : View() {

    private val textColor = Color(0.0, 1.0, 0.0, 1.0)

    private val info = vbox {
        setPrefSize(488.0,512.0)
        style {
            fontSize = Dimension(2.0,Dimension.LinearUnits.em)
            backgroundColor.add(
                LinearGradient(
                    0.0, 0.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE,
                    Stop(0.0, Color(0.49, 0.91, 0.93, 0.5)),
                    Stop(1.0, Color(0.29, 0.29, 0.89, 0.5))
                ))
            textAlignment = TextAlignment.CENTER
            textFill = Color(0.7, 1.0, 0.0, 1.0)
        }
        alignment = Pos.CENTER
        label {
            text = "Debug info!"
            this.textFill = textColor
            isWrapText = true
        }
    }

    override val root: Parent = info

    fun createPieceInfo(currentPiece: ChessPiece) {
        info.children.clear()
        info.children.add(createLabel("${currentPiece.getName()} debug information"))
        info.children.add(createLabel(
            "Board Position: ${currentPiece.getBoardPosition()} or ${positionToAlgebraic(currentPiece.getBoardPosition())}\n" +
                    "Fen char: ${currentPiece.getFenChar()}\n" +
                    "Pinned: ${currentPiece.whichDirectionIsThePinned()}\n" +
                    when (currentPiece) {
                        is King -> "Can castle on the king side: ${currentPiece.canCastleOnTheKingSide()}\n" +
                                "Can Castle on the Queen side: ${currentPiece.canCastleOnTheQueenSide()}\n" +
                                "Has been checked before: ${currentPiece.hasBeenChecked()}\n" +
                                "Has moved: ${currentPiece.hasMoved()}\n"
                        is Pawn -> "Has done a two move: ${currentPiece.hasJustMadeATwoMove()}\nIs Not Pinned By King To Do En Passant: ${currentPiece.isNotPinnedByKingToDoEnPassant()}"
                        is Rook -> "Has Moved: ${currentPiece.hasMoved()}"
                        else -> ""
                    }))
        if (currentPiece is King) {
            info.children.add(
                MenuButton("Attacking Pieces",null, *currentPiece.getPiecesThatAreAttackingTheKing().map { chessPiece -> MenuItem(chessPiece.toString()) }.toTypedArray()))
            info.children.add(
                MenuButton("Protecting Pieces",null, *currentPiece.getPiecesThatAreProtectingTheKing().map { chessPiece -> MenuItem(chessPiece.toString()) }.toTypedArray()))
            info.children.add(
                MenuButton("Attacking Pieces",null, *currentPiece.getPiecesThatAreCheckingTheKing().map { chessPiece -> MenuItem(chessPiece.toString()) }.toTypedArray()))
        }
        info.children.add(MenuButton("View King Attacked",null, *currentPiece.getKingAttacked().map { move -> MenuItem(move.toString()) }.toTypedArray()))

        info.children.add(getFenButton(currentPiece))
    }

    private fun getFenButton(chessPiece: ChessPiece) : Parent {
        val button = Button("Copy Fen")
        button.onMouseClicked = EventHandler {
            val clipboard: Clipboard = Clipboard.getSystemClipboard()
            val content = ClipboardContent()
            content.putString(chessPiece.internalBoard.getFenString())
            println(clipboard.setContent(content))

        }
        //return createTextField("Fen: ${chessPiece.internalBoard.getFenString()}")
        return button
    }

    fun isSquareSafe(whiteKing : Boolean, blackKing : Boolean) {
        info.children.clear()
        info.children.add(createLabel("Is square safe for white king: $whiteKing"))
        info.children.add(createLabel("Is square safe for black king: $blackKing"))
    }

    private fun createLabel(text : String) : Label {
        val label = Label(text)
        label.isWrapText = true
        label.textFill = textColor
        label.textAlignment = TextAlignment.CENTER
        return label
    }

    private fun createTextField(text: String) : TextField {
        val tF = TextField(text)
        tF.isEditable = false
        tF.font = Font.font(14.0)
        return tF
    }
}

class PromotionPrompt : Fragment("Promotion Chooser") {
    var pieceChar = 'T'
    val pieceColor : PieceColor by param()
    override val root: Parent = vbox {
        setMinSize(200.0,250.0)
        style {
            backgroundColor.add(LinearGradient(
                0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE,
                Stop(0.0, Color(0.83, 0.85, 0.87, 1.0)),
                Stop(1.0, Color(0.24, 0.33, 0.41, 1.0))
            ))
        }
        alignment = Pos.CENTER

        spacing = 20.0

        button {
            text = "Queen"
            onMouseClicked = EventHandler { pieceChar = if (pieceColor == PieceColor.BLACK) 'q' else 'Q'; close() }
        }
        button {
            text = "Rook"
            onMouseClicked = EventHandler { pieceChar = if (pieceColor == PieceColor.BLACK) 'r' else 'R'; close() }
        }
        button {
            text = "Bishop"
            onMouseClicked = EventHandler { pieceChar = if (pieceColor == PieceColor.BLACK) 'b' else 'B'; close() }
        }
        button {
            text = "Knight"
            onMouseClicked = EventHandler { pieceChar = if (pieceColor == PieceColor.BLACK) 'n' else 'N'; close() }
        }
    }
}

class GameEndView : View("Game has ended!") {

    val winner : PieceColor? by param()
    val internalBoard : InternalBoard by param()

    private val textColor = Color(0.0, 1.0, 0.0, 1.0)

    override val root: Parent = vbox {
        setMinSize(200.0,250.0)
        style {
            backgroundColor.add(LinearGradient(
                0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE,
                Stop(0.0, Color(0.83, 0.85, 0.87, 1.0)),
                Stop(1.0, Color(0.24, 0.33, 0.41, 1.0))
            ))
        }
        label {
            text = if (winner != null) "${winner!!.colorName} has won!" else "It is a draw!"
            textFill = textColor
            textAlignment = TextAlignment.CENTER
        }
        button {
            text = "Start again?"
            onMouseClicked = EventHandler { internalBoard.setPosition(STARTING_BOARD_FEN_STRING); close() }
        }
    }

}

