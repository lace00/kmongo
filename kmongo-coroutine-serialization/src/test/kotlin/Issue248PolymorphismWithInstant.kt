/*
 * Copyright (C) 2016/2020 Litote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.litote.kmongo.issues

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Test
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.KMongoReactiveStreamsCoroutineBaseTest
import org.litote.kmongo.newId
import org.litote.kmongo.serialization.registerModule
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

enum class Action {
    UNMUTE
}

interface SchedulerJob {
    @Contextual
    @SerialName("_id")
    val id: Id<SchedulerJob>
    val action: Action

    @Contextual
    val startTime: Instant
    val duration: Long
    val targetUserId: String
    val guildId: String

    fun shouldBePersisted(): Boolean {
        return duration >= Duration.ofMinutes(15).toMillis()
    }

}

@Serializable
class UnmuteSchedulerJob(
    @Contextual @SerialName("_id") override val id: Id<SchedulerJob> = newId(),
    @Contextual override val startTime: Instant = Instant.now(),
    override val duration: Long = 10L,
    override val targetUserId: String = "id",
    override val guildId: String = "id",
    val reason: String? = null
) : SchedulerJob {
    override val action: Action = Action.UNMUTE
}

/**
 *
 */
class Issue248PolymorphismWithInstant : KMongoReactiveStreamsCoroutineBaseTest<SchedulerJob>() {

    @ExperimentalSerializationApi
    @InternalSerializationApi
    @Test
    fun `test insert and load`() = runBlocking {
        registerModule(
            SerializersModule {
                polymorphic(baseClass = SchedulerJob::class) {
                    subclass(UnmuteSchedulerJob::class)
                }
            }
        )
        val job = UnmuteSchedulerJob()
        col.insertOne(job)
        assertEquals(
            job.startTime,
            col.findOne()?.startTime
        )
    }
}