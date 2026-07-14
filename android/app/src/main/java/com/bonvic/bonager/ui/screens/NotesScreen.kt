package com.bonvic.bonager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bonvic.bonager.data.AppSnapshot
import com.bonvic.bonager.data.Dates
import com.bonvic.bonager.ui.BonagerViewModel
import com.bonvic.bonager.ui.components.BonagerCard
import com.bonvic.bonager.ui.components.BonagerField
import com.bonvic.bonager.ui.components.CompactButton
import com.bonvic.bonager.ui.components.DeleteButton
import com.bonvic.bonager.ui.components.EmptyState
import com.bonvic.bonager.ui.components.Eyebrow
import com.bonvic.bonager.ui.components.ListCopy
import com.bonvic.bonager.ui.components.Page
import com.bonvic.bonager.ui.components.PrimaryButton
import com.bonvic.bonager.ui.components.SecondaryButton
import com.bonvic.bonager.ui.components.SectionTitle
import com.bonvic.bonager.ui.theme.BonagerColors

@Composable
fun NotesScreen(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var journalBody by rememberSaveable { mutableStateOf("") }
    var journalMood by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var noteTitle by rememberSaveable { mutableStateOf("") }
    var noteBody by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(snapshot.journal?.updatedAt) {
        journalBody = snapshot.journal?.body.orEmpty()
        journalMood = snapshot.journal?.mood.orEmpty()
    }

    fun resetNote() {
        editingId = null
        noteTitle = ""
        noteBody = ""
    }

    Page("Notes", "Capture working notes and keep a daily thought record.") {
        BonagerCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Eyebrow(Dates.toDateKey())
                SectionTitle("Daily journal")
            }
            BonagerField("Mood", journalMood, { journalMood = it }, placeholder = "Focused, tired, hopeful...")
            BonagerField("Thoughts", journalBody, { journalBody = it }, placeholder = "What is on your mind today?", multiline = true)
            PrimaryButton("Save journal", { viewModel.saveJournal(journalBody, journalMood) }, Modifier.fillMaxWidth())
        }

        BonagerCard {
            SectionTitle(if (editingId == null) "New note" else "Edit note")
            BonagerField("Title", noteTitle, { noteTitle = it })
            BonagerField("Note", noteBody, { noteBody = it }, multiline = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(if (editingId == null) "Add note" else "Update note", {
                    viewModel.saveNote(editingId, noteTitle, noteBody)
                    resetNote()
                }, Modifier.weight(1f))
                if (editingId != null) SecondaryButton("Cancel", ::resetNote, Modifier.weight(1f))
            }
        }

        BonagerCard {
            SectionTitle("Saved notes")
            if (snapshot.notes.isEmpty()) {
                EmptyState("No notes yet", "Write a thought, meeting note, client idea, or anything you need to keep.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.notes.forEach { note ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ListCopy(note.title, note.body, Modifier.weight(1f).clickable {
                                editingId = note.id
                                noteTitle = note.title
                                noteBody = note.body
                            })
                            CompactButton("Edit", {
                                editingId = note.id
                                noteTitle = note.title
                                noteBody = note.body
                            })
                            DeleteButton { viewModel.deleteNote(note.id) }
                        }
                    }
                }
            }
        }
    }
}
