package com.ejrp.games
import tornadofx.App

class MyApp : App(ChessView::class) {

    fun main(args: Array<String>) { tornadofx.launch<MyApp>(args) }

}