package com.mabrouk.quran.presentaion.ui

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.work.WorkInfo
import com.mabrouk.core.R
import com.mabrouk.core.theme.WhiteColor
import com.mabrouk.core.utils.RequestMultiplePermissions
import com.mabrouk.core.utils.isTablet
import com.mabrouk.quran.domain.models.Verse
import com.mabrouk.quran.presentaion.states.SurahStates
import com.mabrouk.quran.presentaion.states.VersesDownloadStates
import com.mabrouk.quran.presentaion.utils.LAST_ID
import com.mabrouk.quran.presentaion.viewmodels.SurahViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahScreen(surahViewModel: SurahViewModel,name:String,id:Int){
    val surah = surahViewModel.versesList.collectAsStateWithLifecycle().value
    val downloads = surahViewModel.states.collectAsStateWithLifecycle().value
    var showOptionsMenu = remember { mutableStateOf(false) }
    var showLoading by rememberSaveable { mutableIntStateOf(-1) }
    val currentAyaIndex = surahViewModel.currentAyaIndex.collectAsStateWithLifecycle().value
    var isGrented by remember { mutableStateOf(false) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(surahViewModel.isPlaying.collectAsStateWithLifecycle().value) {
        isPlaying = surahViewModel.isPlaying.value
    }
    val offset = remember { mutableStateOf(DpOffset.Zero) }
    var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }

    val txt = buildAnnotatedString {
        surah.forEachIndexed { index, it ->
            val isClicked = index == currentAyaIndex
            withStyle(style = SpanStyle(
                fontFamily = FontFamily(Font(R.font.quran)),
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                letterSpacing = 0.sp,
                color = if (isClicked&&isPlaying) Color.Blue else  MaterialTheme.colorScheme.onSurface
            )){
                pushStringAnnotation("aya",it.textMadani?:"")
                append(it.textMadani)
                pop()
            }
            append(" ")
            append("[${index+1}]")
            append(" ")
        }
        append("\n")
    }





    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    } else{
        listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }



    RequestMultiplePermissions(
        permissions = permissions,
        onResult = {
            isGrented = it
        },
        alwaysRequest = true
    )

    Scaffold (modifier = Modifier.fillMaxSize(), topBar = {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet()) 70.dp else 50.dp),
            contentAlignment = Alignment.Center
        ){
            Image(
                painter = painterResource(R.drawable.surah_header),
                contentDescription = "",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Text(
                stringResource(com.mabrouk.quran.R.string.sura_txt, name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

        }
    }, floatingActionButton = {
        var isExpended by remember { mutableStateOf(false) }
        Column (horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)){

            AnimatedVisibility(isExpended) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (currentAyaIndex < 0){
                            surahViewModel.setCurrentAyaIndex(0)
                        }

                        if (surahViewModel.player?.isPlaying==true){
                            surahViewModel.player?.pause()
                            isPlaying = false
                        }else{
                            surahViewModel.player?.play()
                            isPlaying = true
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(55.dp),
                    containerColor = MaterialTheme.colorScheme.onPrimary
                ){
                    Icon(
                        painter = painterResource(if (isPlaying) com.mabrouk.quran.R.drawable.pause else com.mabrouk.quran.R.drawable.play_arrow),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "",
                    )
                }
            }

            AnimatedVisibility(isExpended) {
                ExtendedFloatingActionButton(
                    onClick = {
                        isExpended = false
                        showBottomSheet = true
                        scope.launch {
                            sheetState.expand()
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp),
                    containerColor = MaterialTheme.colorScheme.onPrimary
                ){
                    Icon(
                        painter = painterResource(com.mabrouk.quran.R.drawable.reader_24),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "",
                    )
                }
            }

            var rotationState by remember { mutableFloatStateOf(0f) }

            val rotation by animateFloatAsState(
                targetValue = rotationState,
                animationSpec = tween(durationMillis = 500),
                label = "rotation"
            )

            ExtendedFloatingActionButton(
                onClick = {
                    isExpended = !isExpended
                    rotationState += if (isExpended) 360f else -360f
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(70.dp)
                    .rotate(rotation),
                containerColor = MaterialTheme.colorScheme.onPrimary
            ){
                Icon(
                    painter = painterResource(com.mabrouk.quran.R.drawable.accessibility),
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "",
                )
            }
        }
    }
      ){

        DisposableEffect(Unit) {
            onDispose {
                surahViewModel.player?.pause()
                surahViewModel.player?.clearMediaItems()
            }
        }

        Box (modifier = Modifier
            .padding(it)
            .fillMaxWidth()){

            if (currentAyaIndex>=0) AyaOptionsUi(showOptionsMenu,surah[currentAyaIndex])

            if (txt.isBlank()){
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(100.dp)
                        .align(Alignment.Center)
                )
            } else {

                val verses = surah.chunked(min(7, surah.size - 1))

                LaunchedEffect (surahViewModel.currentReader.collectAsStateWithLifecycle().value , isGrented){
                    if (surahViewModel.player?.isPlaying==true){
                        surahViewModel.setCurrentAyaIndex(0)
                    }

                    if (isGrented) {
                        surahViewModel.setCurrentAyaIndex(0)
                        surahViewModel.player?.clearMediaItems()
                        surahViewModel.downloadVerseAudio(name, verses[0], verses,0,id)
                    }

                }

                val density =LocalDensity.current
                ClickableText(
                    onClick = { offest ->
                        val layoutResult = textLayoutResultState ?: return@ClickableText

                        txt.getStringAnnotations("aya", offest, offest).forEach { annotation ->
                            val index = surah.indexOfFirst { it.textMadani == annotation.item }
                            if (index != -1) {
                                Log.d("efefefef",offest.toString())
                                val boundingBox = layoutResult.getBoundingBox(offest)
                                val clickPosInPixels = boundingBox.bottomCenter
                                with(density) {
                                    offset.value = DpOffset(
                                        x = clickPosInPixels.x.toDp(),
                                        y = clickPosInPixels.y.toDp() + 8.dp
                                    )
                                }
                                showOptionsMenu.value = true
                                //clickOffset = DpOffset(x = 0.dp, y = 0.dp)
                                surahViewModel.setCurrentAyaIndex(index)

//                                surahViewModel.setCurrentAyaIndex(index)
//                                surahViewModel.player?.seekTo(index,0L)
//                                surahViewModel.player?.play()
//                                isPlaying = true

                            }
                        }
                    },
                    text = txt,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 35.sp,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                    onTextLayout = { textLayoutResult ->
                        textLayoutResultState = textLayoutResult
                    }
                )

                when (downloads) {
                    VersesDownloadStates.IDLE -> {

                    }

                    VersesDownloadStates.RUNNING, VersesDownloadStates.SUCCESS -> {
                        ++showLoading
                    }
                }
            }

            if (showLoading == 0){
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(100.dp)
                        .align(Alignment.Center)
                )
            }

        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = {
                showBottomSheet = false
            },sheetState = sheetState  , containerColor = MaterialTheme.colorScheme.primary) {
                ReadersUi(surahViewModel)
            }
        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyaOptionsUi(showMenu:MutableState<Boolean>,verse: Verse){
    AnimatedVisibility(visible = showMenu.value) {
        AlertDialog(
            onDismissRequest = {
                showMenu.value = false
            },
            icon = {

            },

            title = {
                Text(verse.textMadani.toString())
            },
            text = {
                Column {
                    Text("ufurhfuhrf")
                    Text("wdwdwdwdwd")
                }
            },
            confirmButton = {},
            dismissButton = {

            }
        )
    }
}