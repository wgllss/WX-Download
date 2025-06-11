package com.wx.download

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.wx.download.ui.theme.WXDownLoadTheme
import com.wx.download.utils.WLog
import com.wx.progress.ProgressButton
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {

    val viewModel by viewModels<WXDownLoadViewModel>()

    private val REQUEST_CODE_STORAGE = 100
    private val REQUEST_CODE_MANAGE_STORAGE = 101

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
        checkPermission()
    }

    private fun checkPermission(): Boolean {
        XXPermissions.with(this).permission(Permission.MANAGE_EXTERNAL_STORAGE).request { permissions, allGranted ->
            if (!allGranted) {
                Toast.makeText(this@MainActivity, "部分权限成功", Toast.LENGTH_LONG).show()
                return@request
            }
        }
        return false
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun ProgressButtonSample(viewModel: WXDownLoadViewModel, innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val datas by viewModel.datas.observeAsState(emptyList())
    val textMeasurer = rememberTextMeasurer()
    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        itemsIndexed(datas, key = { i, item ->
            item.progress
        }) { i, item ->
            ProgressButton(
                Modifier
                    .padding(10.dp, 10.dp, 10.dp, 0.dp)
                    .fillMaxWidth()
                    .height(50.dp), textMeasurer, TextStyle(color = Color.Black, fontSize = 16.sp), item
            ) {
                viewModel.onClick(it, i)
            }
        }
    }
}
