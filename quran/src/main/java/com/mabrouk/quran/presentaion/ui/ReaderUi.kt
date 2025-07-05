package com.mabrouk.quran.presentaion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mabrouk.quran.R
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.presentaion.viewmodels.SurahViewModel

@Composable
fun ReadersUi(surahViewModel: SurahViewModel){
    val readers = surahViewModel.reader.collectAsStateWithLifecycle().value
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    LaunchedEffect (readers){
        selectedIndex = readers.indexOfFirst { it.isSelected }
    }

    LazyColumn (modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){
       item { Spacer(modifier = Modifier.size(5.dp)) }
        items(readers.size){
            ReaderItemView(readers[it], isSelected = it == selectedIndex){
                selectedIndex = if (selectedIndex == it) -1 else it
                surahViewModel.updateReader(context,readers[it])
                readers.map {  reader->
                    reader.isSelected = reader.readerId == readers[it].readerId
                }
            }
        }
        item { Spacer(modifier = Modifier.size(5.dp)) }
    }
}

@Composable
fun ReaderItemView(reader: QuranReader,isSelected:Boolean,onItemSelected:()->Unit){
    Card (
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(5.dp),
        shape = RoundedCornerShape(2.dp)
    ){

        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp).clickable {
                onItemSelected()
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                text = reader.name
            )

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = "check",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

        }
    }
}