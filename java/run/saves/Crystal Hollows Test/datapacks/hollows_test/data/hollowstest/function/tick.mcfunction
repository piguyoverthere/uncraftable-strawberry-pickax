execute as @a[tag=!hollows] run gamemode spectator @s
execute as @a[tag=!hollows] run effect give @s minecraft:night_vision infinite 0 true
execute as @a[tag=!hollows] at @s run tp @s ~ -8 ~ 30 20
tag @a[tag=!hollows] add hollows
execute as @a[tag=!facetest] at @s run fill ~2 ~ ~2 ~4 ~2 ~4 minecraft:stone
execute as @a[tag=!facetest] at @s run setblock ~3 ~3 ~3 strawberrymc:crystalline_calcite[facing=up]
execute as @a[tag=!facetest] at @s run setblock ~3 ~-1 ~3 strawberrymc:crystalline_calcite[facing=down]
execute as @a[tag=!facetest] at @s run setblock ~1 ~1 ~3 strawberrymc:crystalline_calcite[facing=west]
execute as @a[tag=!facetest] at @s run setblock ~5 ~1 ~3 strawberrymc:crystalline_calcite[facing=east]
execute as @a[tag=!facetest] at @s run setblock ~3 ~1 ~1 strawberrymc:crystalline_calcite[facing=north]
execute as @a[tag=!facetest] at @s run setblock ~3 ~1 ~5 strawberrymc:crystalline_calcite[facing=south]
tag @a[tag=!facetest] add facetest
