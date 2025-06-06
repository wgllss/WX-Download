package com.wx.download

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.wx.download.ui.theme.WXDownLoadTheme
import com.wx.progress.ProgressButton

class MainActivity : ComponentActivity() {

    val viewModel by viewModels<WXDownLoadViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WXDownLoadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProgressButtonSample(
                        viewModel, innerPadding
                    )
                }
            }
        }
        viewModel.add()
    }
}

@Composable
fun ProgressButtonSample(viewModel: WXDownLoadViewModel, innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val datas by viewModel.datas.observeAsState()

    val progress by viewModel.progress.observeAsState(0f)
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        datas?.let {
            ProgressButton(
                Modifier
                    .padding(50.dp, 50.dp, 50.dp, 0.dp)
                    .fillMaxWidth()
                    .height(50.dp), textMeasurer, TextStyle(color = Color.Black, fontSize = 16.sp), it, progress
            ) {
                viewModel.onClick(it)
            }
        }
    }
}
