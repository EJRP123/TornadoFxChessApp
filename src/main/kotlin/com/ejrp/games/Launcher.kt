package com.ejrp.games

class Launcher {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MyApp().main(args)
        }
    }

}

//module Chess.main {
//    requires javafx.controls;
//    requires kotlin.stdlib;
//    requires ChessEngine;
//    requires tornadofx;
//    opens com.ejrp.games to javafx.controls;
//    exports com.ejrp.games;
//}