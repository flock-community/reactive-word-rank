package com.github.flockcommunity.reactivestreaming.middleware.wordcloud.service

import com.github.flockcommunity.reactivestreaming.middleware.wordcloud.domain.Word
import com.github.flockcommunity.reactivestreaming.middleware.wordcloud.domain.WordCloud
import com.github.flockcommunity.reactivestreaming.middleware.wordcloud.repository.WordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

private const val WORDCLOUD_ID: Long = 1

@Service
internal class SimpleWordService(private val repo: WordRepository) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val wordCounter = mutableMapOf<String, Long>()

    fun getWords(): Flux<Word> = repo.getWords()

    fun getWordsWithRetry(): Flux<Word> = getWords()
            .doOnError { log.info("Issue retrieving words. Starting over ... ", it) }
            .retry()

    fun getWordDistribution(): Flux<WordCloud> {
        val wordCounter = mutableMapOf<String, Long>()
        return getWords()
                .map {
                    wordCounter.updateWith(it.word)
                    WordCloud(WORDCLOUD_ID, wordCounter)
                }
                .doOnError { log.info("Issue updating wordDistribution. Stopping", it) }
                .onErrorStop()
    }

    fun getWordDistributionWithRetry(): Flux<WordCloud> = getWordsWithRetry()
            .onErrorReturn(Word(-1,"error"))
            .map {
                wordCounter.updateWith(it.word)
                WordCloud(WORDCLOUD_ID, wordCounter)
            }
            .doOnError { log.info("Issue updating wordDistribution. Trying again", it) }

    private fun MutableMap<String, Long>.updateWith(word: String): MutableMap<String, Long> {
        val currentCount = this[word] ?: 0
        this[word] = currentCount + 1
        return this
    }
}

