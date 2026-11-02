package com.vitregram.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.vitregram.tv.navigation.TvNavGraph
import com.vitregram.tv.ui.theme.VitreGramTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep TV display active during media playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            VitreGramTheme {
                val navController = rememberNavController()
                TvNavGraph(navController = navController)
            }
        }
    }
}
