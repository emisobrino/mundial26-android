package com.mundial26.app.data.repository

import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.news.NewsRssSource
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.repository.NewsRepository

class DefaultNewsRepository(private val rss: NewsRssSource) : NewsRepository {
    private var cache: List<NewsItem> = emptyList()
    private var at: Long = 0
    override suspend fun news(): List<NewsItem> {
        if (cache.isEmpty() || System.currentTimeMillis() - at > 3 * 60 * 60 * 1000L) {
            val fresh = rss.headlines()
            if (fresh.isNotEmpty()) { cache = fresh; at = System.currentTimeMillis() }
        }
        return cache.ifEmpty { MockData.news }
    }
    override suspend fun item(id: String): NewsItem? = news().firstOrNull { it.id == id }
    override suspend fun refresh() { cache = emptyList(); at = 0 }
}
