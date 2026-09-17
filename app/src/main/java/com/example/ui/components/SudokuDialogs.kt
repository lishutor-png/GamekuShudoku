package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AssistanceSettings
import com.example.model.Difficulty
import com.example.ui.theme.LimeAccent
import com.example.viewmodel.SudokuUiState

@Composable
fun DifficultyDialog(
  currentDifficulty: Difficulty,
  onSelectDifficulty: (Difficulty) -> Unit,
  onDismiss: () -> Unit,
  isDark: Boolean
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Pilih Tingkat Kesulitan",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Tutup")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Difficulty.values().forEach { diff ->
          val isSelected = (diff == currentDifficulty)
          val borderColor = if (isSelected) {
            if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          }

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
              .clickable {
                onSelectDifficulty(diff)
                onDismiss()
              }
              .testTag("difficulty_${diff.name.lowercase()}"),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) {
                if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              }
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = diff.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                  ) {
                    Text(
                      text = "${diff.clueCount} angka",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = diff.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .background(if (isDark) LimeAccent else MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF142900) else Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {}
  )
}

@Composable
fun SettingsDialog(
  settings: AssistanceSettings,
  onUpdateSettings: (AssistanceSettings) -> Unit,
  onAutoFillNotes: () -> Unit,
  onClearNotes: () -> Unit,
  onDismiss: () -> Unit,
  isDark: Boolean
) {
  val switchColors = SwitchDefaults.colors(
    checkedThumbColor = if (isDark) Color(0xFF142900) else Color.White,
    checkedTrackColor = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Pengaturan & Bantuan",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Tutup")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "FITUR BANTUAN PEMULA",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
        )

        SettingToggleRow(
          title = "Sorot Angka Sama",
          subtitle = "Tampilkan semua sel yang memiliki angka yang sama dengan yang dipilih",
          checked = settings.highlightSameNumber,
          onCheckedChange = { onUpdateSettings(settings.copy(highlightSameNumber = it)) },
          switchColors = switchColors
        )

        SettingToggleRow(
          title = "Sorot Baris & Kolom",
          subtitle = "Beri warna garis bidik pada baris, kolom, dan kotak 3x3 yang aktif",
          checked = settings.highlightRowColBox,
          onCheckedChange = { onUpdateSettings(settings.copy(highlightRowColBox = it)) },
          switchColors = switchColors
        )

        SettingToggleRow(
          title = "Peringatan Duplikat",
          subtitle = "Tandai angka yang bentrok dalam baris, kolom, atau blok yang sama",
          checked = settings.highlightDuplicates,
          onCheckedChange = { onUpdateSettings(settings.copy(highlightDuplicates = it)) },
          switchColors = switchColors
        )

        SettingToggleRow(
          title = "Hapus Otomatis Ragu-ragu",
          subtitle = "Hapus catatan angka saat angka tersebut dikonfirmasi di baris/kolom yang sama",
          checked = settings.autoRemoveNotes,
          onCheckedChange = { onUpdateSettings(settings.copy(autoRemoveNotes = it)) },
          switchColors = switchColors
        )

        HorizontalDivider()

        Text(
          text = "TANTANGAN",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
        )

        SettingToggleRow(
          title = "Batasi 3 Kesalahan",
          subtitle = "Game over jika melakukan 3 kesalahan (Nonaktifkan untuk mode santai pemula)",
          checked = settings.mistakeLimitEnabled,
          onCheckedChange = { onUpdateSettings(settings.copy(mistakeLimitEnabled = it)) },
          switchColors = switchColors
        )

        HorizontalDivider()

        Text(
          text = "AKSI BANTUAN CEPAT",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
        )

        OutlinedButton(
          onClick = {
            onAutoFillNotes()
            onDismiss()
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("button_auto_fill_notes"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Isi Otomatis Semua Ragu-ragu")
        }

        OutlinedButton(
          onClick = {
            onClearNotes()
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Bersihkan Semua Catatan Ragu-ragu")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text(
          "Selesai",
          color = if (isDark) Color(0xFF142900) else Color.White,
          fontWeight = FontWeight.Bold
        )
      }
    }
  )
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  switchColors: SwitchColors
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = switchColors
    )
  }
}

@Composable
fun VictoryDialog(
  uiState: SudokuUiState,
  onPlayAgain: () -> Unit,
  onChangeDifficulty: () -> Unit,
  onDismiss: () -> Unit
) {
  val isDark = uiState.isDarkTheme

  AlertDialog(
    onDismissRequest = onDismiss,
    title = null,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Cool Sudoku Victory Badge
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
              width = 2.dp,
              color = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(20.dp)
            )
        ) {
          Image(
            painter = painterResource(id = R.drawable.sudoku_cool_logo),
            contentDescription = "Logo ShuDoku",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }

        Text(
          text = "Selamat! Kamu Menang!",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Kamu berhasil menyelesaikan teka-teki ShuDoku ini dengan sangat baik.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        // Summary Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            VictoryStatRow(label = "Tingkat Kesulitan", value = uiState.difficulty.displayName)
            VictoryStatRow(label = "Waktu Penyelesaian", value = formatTime(uiState.timerSeconds))
            VictoryStatRow(label = "Jumlah Kesalahan", value = "${uiState.mistakesCount}")
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onPlayAgain,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("button_play_again"),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDark) LimeAccent else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          "Main Lagi",
          color = if (isDark) Color(0xFF142900) else Color.White,
          fontWeight = FontWeight.Bold
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onChangeDifficulty,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Ganti Tingkat Kesulitan")
      }
    }
  )
}

@Composable
private fun VictoryStatRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun StatsDialog(
  uiState: SudokuUiState,
  onDismiss: () -> Unit
) {
  val stats = uiState.stats

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Statistik Permainan",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Tutup")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Dimainkan",
            value = stats.gamesPlayed.toString(),
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "Kemenangan",
            value = stats.gamesWon.toString(),
            modifier = Modifier.weight(1f)
          )
        }

        Text(
          text = "Waktu Terbaik per Kesulitan",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Difficulty.values().forEach { diff ->
              val time = stats.bestTimes[diff]
              val formatted = if (time != null) formatTime(time) else "--:--"
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(text = diff.displayName, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = formatted,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Tutup")
      }
    }
  )
}

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Panduan Bermain ShuDoku",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Tutup")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        RuleItem(
          number = "1",
          title = "Aturan Dasar 9x9",
          desc = "Isi setiap baris, setiap kolom, dan setiap kotak 3x3 dengan angka 1 sampai 9 tanpa ada angka yang berulang."
        )

        RuleItem(
          number = "2",
          title = "Mudah Mengisi Angka",
          desc = "Kamu bisa ketuk kotak lalu pilih angka pada tombol 1-9 di bawah. Angka yang sudah terisi 9 kali akan otomatis diberi tanda centang."
        )

        RuleItem(
          number = "3",
          title = "Opsi Ragu-ragu (Catatan Pensil)",
          desc = "Jika kamu belum yakin, aktifkan tombol 'Ragu-ragu'. Angka yang kamu ketuk akan ditulis sebagai catatan kecil di dalam kotak untuk membantumu berfikir."
        )

        RuleItem(
          number = "4",
          title = "Fitur Bantuan Pemula",
          desc = "Gunakan tombol 'Bantuan' untuk mendapatkan jawaban pada kotak yang kamu pilih. Kamu juga bisa mengaktifkan sorotan baris, kolom, dan angka yang sama pada menu Pengaturan."
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Mengerti, Ayo Main!")
      }
    }
  )
}

@Composable
private fun RuleItem(number: String, title: String, desc: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(26.dp)
        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = number,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
