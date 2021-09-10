package org.matsim.run.modules

import org.matsim.episim.EpisimConfigGroup
import org.matsim.episim.EpisimConfigGroup.InfectionParams
import java.nio.file.Path
import java.time.DayOfWeek


inline operator fun EpisimConfigGroup.invoke(block: EpisimConfigGroup.() -> Unit) {
    apply(block)
}

inline fun EpisimConfigGroup.addInputEventsFiles(root: Path, block: EventFileParamsBuilder.() -> Unit) {
    EventFileParamsBuilder.config = this
    EventFileParamsBuilder.root = root
    EventFileParamsBuilder.block()
}

object EventFileParamsBuilder {
    lateinit var config: EpisimConfigGroup
    lateinit var root: Path

    infix fun String.on(day: DayOfWeek) = on(*arrayOf(day))

    fun String.on(vararg days: DayOfWeek) = config.addInputEventsFile(root.resolve(this).toString()).addDays(*days)

    infix fun String.on(days: ClosedRange<DayOfWeek>) = on(*DayOfWeek.values().filter { it in days }.toTypedArray())
}

inline operator fun EpisimConfigGroup.InfectionParams.invoke(block: EpisimConfigGroup.InfectionParams.() -> Unit) {
    apply(block)
}

inline fun EpisimConfigGroup.getOrAddContainersParams(block: InfectionParamsBuilder.() -> Unit) {
    InfectionParamsBuilder.config = this
    InfectionParamsBuilder.block()
}

object InfectionParamsBuilder {

    lateinit var config: EpisimConfigGroup

    inline operator fun String.invoke(vararg mappedNames: String, block: InfectionParams.() -> Unit) =
        config.getOrAddContainerParams(this, *mappedNames).block()
}