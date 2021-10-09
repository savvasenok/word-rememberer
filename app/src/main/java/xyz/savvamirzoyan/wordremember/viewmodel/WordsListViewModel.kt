package xyz.savvamirzoyan.wordremember.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.savvamirzoyan.wordremember.contract.repository.IWordsListRepository
import xyz.savvamirzoyan.wordremember.contract.viewmodel.IViewModelDeleteSwipedItem
import xyz.savvamirzoyan.wordremember.data.database.model.AdjectiveWord
import xyz.savvamirzoyan.wordremember.data.database.model.NounWord
import xyz.savvamirzoyan.wordremember.data.database.model.VerbWordWithVerbForms
import xyz.savvamirzoyan.wordremember.data.entity.WordsListItem

class WordsListViewModel(
    private val repository: IWordsListRepository
) : ViewModel(), IViewModelDeleteSwipedItem {

    private val searchQueryFlow by lazy { MutableStateFlow("") }
    private val _wordsListFlow by lazy { MutableStateFlow(listOf<WordsListItem>()) }
    private val _showUndoSnackbarFlow by lazy { Channel<WordsListEvent>() }

    val wordsListFlow by lazy { _wordsListFlow.asStateFlow() }
    val showUndoSnackbarFlow by lazy { _showUndoSnackbarFlow.receiveAsFlow() }

    init {
        Timber.i("WordsListViewModel created")

        viewModelScope.launch(Dispatchers.IO) {
            receiveWordsFlow()
                .collect {
                    _wordsListFlow.emit(it)
                }
        }
    }

    private fun receiveWordsFlow() = combine(
        repository.wordsList,
        repository.verbsList,
        repository.adjectivesList,
        searchQueryFlow
    ) { nouns, verbs, adjectives, searchQuery ->
        (nouns
            .map { it.toWordsListItem() } +
                verbs.map { it.toWordsListItem() } +
                adjectives.map { it.toWordsListItem() })
            .sortedBy { it.word }
            .filter {
                it.word.search(searchQuery)
            }
    }

    private fun NounWord.toWordsListItem(): WordsListItem.WordsListItemNoun {
        return WordsListItem.WordsListItemNoun(
            id,
            word ?: plural ?: "",
            translation,
            gender
        )
    }

    private fun VerbWordWithVerbForms.toWordsListItem(): WordsListItem.WordsListItemVerb {
        return WordsListItem.WordsListItemVerb(
            verb.verbId, // only verbId, because verbForms id is the same
            verb.word,
            verb.translation,
            verbForm.prateritumSieSie ?: "",
            verbForm.perfekt ?: "",
        )
    }

    private fun AdjectiveWord.toWordsListItem(): WordsListItem.WordsListItemAdjective {
        return WordsListItem.WordsListItemAdjective(
            id,
            word,
            translation,
            komparativ,
            superlativ
        )
    }

    private fun String.search(query: String): Boolean {
        return this.lowercase().contains(query.lowercase())
    }

    fun performSearch(search: String) {
        Timber.i("performSearch(search:\"$search\")")

        viewModelScope.launch { searchQueryFlow.emit(search) }
    }

    fun returnNounToDB(nounWord: NounWord) {
        Timber.i("returnNounToDB(nounWord:$nounWord)")

        viewModelScope.launch(Dispatchers.IO) {
            repository.addNoun(nounWord)
        }
    }

    fun returnAdjectiveToDB(adjectiveWord: AdjectiveWord) {
        Timber.i("returnAdjectiveToDB(adjectiveWord:$adjectiveWord)")

        viewModelScope.launch(Dispatchers.IO) {
            repository.addAdjective(adjectiveWord)
        }
    }

    fun returnVerbToDB(verb: VerbWordWithVerbForms) {
        Timber.i("returnVerbToDB(verb:$verb)")

        viewModelScope.launch(Dispatchers.IO) {
            repository.addVerb(verb)
        }
    }

    override fun deleteWord(wordsListItem: WordsListItem) {
        Timber.i("deleteWord(wordsListItem:$wordsListItem)")

        viewModelScope.launch(Dispatchers.IO) {
            when (wordsListItem) {
                is WordsListItem.WordsListItemAdjective -> {
                    repository.getAdjective(wordsListItem.id)?.let {
                        repository.deleteAdjective(it.id)
                        _showUndoSnackbarFlow.send(WordsListEvent.ReturnBackAdjective(it))
                    }
                }
                is WordsListItem.WordsListItemNoun -> {
                    repository.getNoun(wordsListItem.id)?.let {
                        repository.deleteNoun(it.id)
                        _showUndoSnackbarFlow.send(WordsListEvent.ReturnBackNoun(it))
                    }
                }
                is WordsListItem.WordsListItemVerb -> {
                    repository.getVerb(wordsListItem.id)?.let {
                        repository.deleteVerb(it.verb.verbId)
                        repository.deleteVerbForm(it.verb.verbId)
                        _showUndoSnackbarFlow.send(WordsListEvent.ReturnBackVerb(it))
                    }
                }
            }

        }
    }

    sealed class WordsListEvent {
        data class ReturnBackNoun(val nounWord: NounWord) : WordsListEvent()
        data class ReturnBackVerb(val verb: VerbWordWithVerbForms) : WordsListEvent()
        data class ReturnBackAdjective(val adjectiveWord: AdjectiveWord) : WordsListEvent()
    }
}