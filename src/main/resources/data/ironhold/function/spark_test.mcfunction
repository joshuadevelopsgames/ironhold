# Emits both Iron Spark variants from the same point just above the player.
# The 0.45 delta spreads particles through a volume (distance between each) and the
# low 0.01 speed keeps them roughly in place so the spacing stays readable.
# Run with: /function ironhold:spark_test   (use /reload first if freshly added)
# NOTE: command args must be single-space separated — brigadier treats extra spaces as significant.
particle ironhold:iron_spark ~ ~1 ~ 0.45 0.45 0.45 0.01 18 force
particle ironhold:iron_spark_flare ~ ~1 ~ 0.45 0.45 0.45 0.01 18 force
particle ironhold:iron_spark_pulse ~ ~1 ~ 0.45 0.45 0.45 0.01 18 force
