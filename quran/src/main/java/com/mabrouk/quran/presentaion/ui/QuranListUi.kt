package com.mabrouk.quran.presentaion.ui

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mabrouk.core.theme.BlackColor
import com.mabrouk.core.theme.LightGreyColor
import com.mabrouk.core.theme.MuslimArchiveTheme
import com.mabrouk.core.utils.RequestMultiplePermissions
import com.mabrouk.quran.R
import com.mabrouk.quran.domain.models.JuzSurah
import com.mabrouk.quran.presentaion.states.DownloadStates
import com.mabrouk.quran.presentaion.states.QuranList
import com.mabrouk.quran.presentaion.states.QuranStates
import com.mabrouk.quran.presentaion.states.SurahContent
import com.mabrouk.quran.presentaion.viewmodels.QuranViewModel
import com.mabrouk.quran.presentaion.viewmodels.SurahViewModel


@Composable
fun QuranScreen(quranViewModel: QuranViewModel = hiltViewModel()){
    val navController = rememberNavController()

    NavHost(navController = navController , startDestination = QuranList){
        composable<QuranList> {
            QuranListUi(quranViewModel,navController=navController)
        }

        composable<SurahContent> {
            val surahContent : SurahContent = it.toRoute()
            val surahViewModel : SurahViewModel = hiltViewModel()
            LaunchedEffect (Unit){
                surahViewModel.getAllReader()
                surahViewModel.loadVerses(surahContent.id)
            }
            SurahScreen(surahViewModel,surahContent.name,surahContent.id)
        }

    }

}

@Composable
fun QuranListUi(quranViewModel: QuranViewModel ,navController: NavHostController) {

    LaunchedEffect (Unit){
        quranViewModel.loadData()
    }

     val list = quranViewModel.quranStates.collectAsStateWithLifecycle().value
     val showLoader = quranViewModel.loading.collectAsStateWithLifecycle().value

    Box(modifier = Modifier.fillMaxSize()){
        when (list) {
            QuranStates.ClearSearch -> {

            }

            is QuranStates.Error -> {

            }

            is QuranStates.LoadJuzSurahs -> {
                QuranListing(list.juzSurah,quranViewModel){
                    it.sura?.let {
                        navController.navigate(SurahContent(it.id,it.nameArabic?:""))
                    }
                }
            }

            QuranStates.IDLE, QuranStates.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            }

            is QuranStates.SearchResult -> {

            }
        }

        if (showLoader is DownloadStates.Loading){
            CircularProgressIndicator(
                color = BlackColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }

    }
}

@Composable
fun QuranListing(juzSurahs: List<JuzSurah>,quranViewModel: QuranViewModel,onClick:(JuzSurah)->Unit) {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(start = 5.dp, end = 5.dp)) {
        item {
            Spacer(modifier = Modifier.size(5.dp))
        }
        items(juzSurahs) {
            if (it.sura != null) {
                SurahItemView(it,quranViewModel,onClick)
            } else {
                JuzItemView(it,quranViewModel)
            }
        }
    }
}

@Composable
fun JuzItemView(juzSurah: JuzSurah,quranViewModel: QuranViewModel) {
    Card (
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(5.dp),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.juz_txt, juzSurah.juzNum),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )


                Icon(
                    painter = painterResource(com.mabrouk.core.R.drawable.download_for_offline_24),
                    contentDescription = "",
                    tint = LightGreyColor,
                    modifier = Modifier.clickable {
                        quranViewModel.downloadSurahs(juzSurah.verseIds)
                    }
                )
        }
    }
}

@Composable
fun SurahItemView(juzSurah: JuzSurah,quranViewModel: QuranViewModel,onClick:(JuzSurah)->Unit) {
    val context = LocalContext.current
    var isDownload by remember { mutableStateOf(juzSurah.sura?.isDownload ?: false) }
    val states = quranViewModel.loading.collectAsStateWithLifecycle().value
    val msg = stringResource(R.string.download_aya_msg,juzSurah.sura?.nameArabic?:"")

    if (states is DownloadStates.Success){
        juzSurah.sura?.id?.let {
            if (it in states.ids){
                isDownload = true
                quranViewModel.updateVerseDownload(true,it)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isDownload) {
                    Toast.makeText(
                        context,
                        msg, Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onClick(juzSurah)
                }
            }
            .padding(top = 15.dp, bottom = 15.dp, start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.sura_txt, juzSurah.sura?.nameArabic ?: ""),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            juzSurah.fromTo ?: "",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )

        if (!isDownload) {
                Icon(
                    painter = painterResource(com.mabrouk.core.R.drawable.download_for_offline_24),
                    contentDescription = "",
                    tint = LightGreyColor,
                    modifier = Modifier.clickable {
                        quranViewModel.downloadSurahs(listOf(juzSurah.sura?.id ?: 0))
                    }
                )
            }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranPreviewUi() {
    MuslimArchiveTheme {
    }
}

