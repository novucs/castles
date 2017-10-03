package net.novucs.castle

val DEFAULT_SETTINGS = Settings(
        warpWarmUp = 5,
        wallStrength = 20,
        rewards = mapOf("default" to Reward(
                name = "default",
                description = "The default set of rewardType",
                loss = Targets(
                        commands = listOf("f powerboost f {faction} reset")
                ),
                win = Targets(
                        commands = listOf("f powerboost f {faction} 50")
                )
        ))
)

data class Settings(
        val warpWarmUp: Int,
        val wallStrength: Int,
        val rewards: Map<String, Reward>
)

data class Reward(
        val name: String,
        val description: String,
        val loss: Targets,
        val win: Targets
)

data class Targets(
        val commands: List<String>?
)
