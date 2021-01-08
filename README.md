# tmvkrpxl0_Combat
It's implementation of various skills i imagined in school because i was bored
Spigot page: https://www.spigotmc.org/resources/1-16-tmvkrpxl0-combat.87591/
Feel free to use it, i strongly recommend you to use keybind mod, since every skills are command

Usage: /tcombat <argument>
There are 2 kinds of commands, non-target commands and target commands

non-target commands are commands that don't require targets
target commands are commands that require targets

Non-target commands:

help: Displays this message

target [playername]: Sets targets to visible entities or the player, player will be added to existing target list

arrow: Toggles arrow avoiding, looking arrow directly will return arrow to the shooter

burst: Toggles whether burst 5 arrows when you fire one, cannot be used with multishot

multishot: Toggles whether fire 15~25 more arrows when you fire one, cannot be used with burst, and has 4 seconds cool down which can be disabled

burstmultishot: Makes burst and multishot to be able to be enabled together(I don't recommend using this for other purpose than fun, it's so powerful)

select: Cycles selection mode for skills that only work for one target

fishingmode: Cycles fishing modes, pressing shift while pulling grappling hook will pull hooked entity towards you

cooldown: Toggles whether enable cool down or not

jump: Toggles whether enable double jump or not, only works in survival or adventure mode

fallimpact: Toggles whether enable fall impact or not, not only you need to be fast enough, but also you need to fall more than 15 blocks

Target commands:

lift: Lifts ground below targets, you need to target at least 1 entity to use this

goback: Teleports you to back of target, only works for one target, which target you'll be teleport to depends on selection mode

tnt: Throws tnt at single target with 45°, and has 4 seconds cool down which can be disabled. Trajectory of tnt may be inaccurate

homing: Changes arrows you shot into homing arrows, can be used with multishot or burst

