package org.spotube.spotube.core.audioplayer

class FakeAudioPlayerQueueRepository : QueueStateRepository {
    private var persistedState: PersistedQueueState? = null

    override suspend fun getPersistedState(): PersistedQueueState? = persistedState

    override suspend fun saveState(state: PersistedQueueState) {
        persistedState = state
    }

    override suspend fun clearState() {
        persistedState = null
    }

    fun setState(state: PersistedQueueState?) {
        persistedState = state
    }
}
