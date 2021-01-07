package tmvkrpxl0.src;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.*;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CombatCommands implements CommandExecutor, TabCompleter {
    tmvkrpxl0Combat main;
    private final String[] targetCommands = new String[]{"lift", "goback", "tnt", "homing"};
    private final String[] nonTargetCommands = new String[]{"help", "target", "arrow", "multishot", "select", "fishingmode", "cooldown", "jump", "fallimpact", "burst", "burstmultishot"};
    private final String[] allCommands = (String[]) ArrayUtils.addAll(targetCommands, nonTargetCommands);
    CombatCommands(tmvkrpxl0Combat main){
        this.main = main;
    }
    @Override
    @Deprecated
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("this command is entirely player only");
            return true;
        }
        Player player = ((Player) sender);
        if(args.length>0){
            PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
            LivingEntity target = null;
            if(config.targets.size()==0){
                for(String arg : targetCommands){
                    if(arg.equals(args[0])){
                        player.sendMessage(main.RED + "You have no targets selected!");
                        return true;
                    }
                }
            }else{
                target = CombatUtil.pickTarget(player);
            }
            if(!ArrayUtils.contains(allCommands, args[0])){
                player.sendMessage(main.RED + "Unknown command!");
                displayHelp(player);
                return true;
            }
            switch(args[0].toLowerCase()){
                case "help":
                    displayHelp(player);
                    break;
                case "lift":
                    {
                        for(LivingEntity livingEntity : config.targets){
                            if(!livingEntity.isOnGround())continue;
                            Location targetLocation = livingEntity.getLocation().clone();
                            WorldServer world = ((CraftWorld)targetLocation.getWorld()).getHandle();
                            for(int x = -1;x<2;x++){
                                for(int z = -1;z<2;z++){
                                    int y = targetLocation.getWorld().getHighestBlockYAt(targetLocation.getBlockX()+x, targetLocation.getBlockZ()+z);
                                    for(;y>targetLocation.getBlockY()-3;y--){
                                        BlockPosition blockPosition = new BlockPosition(targetLocation.getX()+x, y, targetLocation.getZ()+z);
                                        Block block = world.getWorld().getBlockAt(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                                        SolidFallingBlock solidFallingBlock = new SolidFallingBlock(world,blockPosition.getX()+0.5, blockPosition.getY(), blockPosition.getZ()+0.5, world.getType(blockPosition));
                                        block.setType(Material.AIR);
                                        solidFallingBlock.setMot(0,1,0);
                                        world.addEntity(solidFallingBlock);
                                    }
                                }
                            }
                            livingEntity.setVelocity(livingEntity.getVelocity().add(new Vector(0,1,0)));
                        }
                    }
                    break;
                case "arrow":
                    if(config.avoidArrow){
                        player.sendMessage(main.RED + "Arrow avoiding is now disabled!");
                    }else{
                        player.sendMessage(main.GREEN + "Arrow avoiding is now enabled!");
                    }
                    config.avoidArrow = !config.avoidArrow;
                    break;
                case "target":
                    {
                        for(LivingEntity livingEntity : config.targets){
                            CombatUtil.setGlowing(livingEntity, player, false);
                        }
                        if(args.length>1){
                            Player targetPlayer = Bukkit.getPlayer(args[1]);
                            if(targetPlayer!=null){
                                config.targets.add(targetPlayer);
                            }else{
                                player.sendMessage(main.RED + "Cannot find " + args[1]);
                            }
                        }else{
                            CombatUtil.selectTargets(player);
                        }
                        if(config.targets.size()==0){
                            player.sendMessage(main.RED + "No target has selected");
                        }else{
                            player.sendMessage(main.GREEN + config.targets.size() + " targets selected");
                            for(LivingEntity livingEntity : config.targets){
                                CombatUtil.setGlowing(livingEntity, player, true);
                            }
                        }
                    }
                    break;
                case "goback":
                    {
                        Location teleportTo = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-2.5));
                        teleportTo.setY(target.getLocation().getY());
                        player.getLocation().setDirection(target.getLocation().clone().subtract(player.getLocation()).toVector().normalize());
                        Material check = teleportTo.getWorld().getBlockAt(teleportTo.add(0,1,0)).getType();
                        teleportTo.subtract(0,1,0);
                        if(!check.equals(Material.AIR) && check.isSolid()){
                            player.sendMessage(ChatColor.RED + "You will get hit by something");
                        }
                        else{
                            player.teleport(teleportTo);
                        }
                    }
                    break;
                case "multishot":
                    if(config.multiShot){
                        player.sendMessage(main.RED + "MultiShot is now disabled!");
                    }else{
                        player.sendMessage(main.GREEN + "MultiShot is now enabled!");
                    }
                    if(!config.burstWithMultishot && config.burst)config.burst=false;
                    config.multiShot = !config.multiShot;
                    break;
                case "tnt":
                    if(player.isOnGround()){
                        if(!config.hasCoolDown || System.currentTimeMillis() > config.tntCoolDown){
                            config.tntCoolDown = System.currentTimeMillis() + 4000;
                            Location location = player.getLocation().clone();
                            Location targetLocation = target.getLocation().clone();
                            location.add(0,-1,0);
                            Vector direction = player.getLocation().getDirection().normalize().multiply(1.3);
                            for(int i = 0;i<3;i++){
                                location.add(direction.getX(),0,direction.getZ());
                                location.getBlock().setType(Material.AIR);
                                TNTPrimed tntPrimed = (TNTPrimed) location.getWorld().spawnEntity(location, EntityType.PRIMED_TNT);
                                tntPrimed.setSource(player);
                                //EQUATION
                                //y = y0 + V0y * t + 0.5 * ay * t^2
                                //x = x0 + V0x * t + 0.5 * ax + t^2 (ax is considered as 0, but minecraft does have drag, which is 0.02, but i'm gonna ignore it)
                                //time = xDifference / sin45 * initialVelocity = xDifference * Math.sqrt(2) / initialVelocity
                                //yDifference = xDifference - 0.02 * xDifference * xDifference * 2 / (initialVelocity * initialVelocity)
                                //xDifference - yDifference= 0.02 * xDifference * xDifference * 2 / Math.pow(initialVelocity, 2)
                                //Math.pow(initialVelocity, 2) = 0.04 * xDifference * xDifference/(xDifference - yDifference)
                                //initialVelocity = Math.sqrt(0.04 * xDifference * xDifference/(xDifference - yDifference));

                                //Getting initial Velocity, equation is simplified since i'm only using 45 degree
                                Location difference = targetLocation.clone().subtract(location);
                                double temp = location.getY();
                                location.setY(targetLocation.getY());
                                double distance = location.distance(targetLocation);
                                location.setY(temp);
                                double yDifference = difference.getY();
                                double initialVelocity = Math.sqrt(0.04 * distance * distance /(distance - yDifference));

                                //Time does not need to be divided by 20
                                double time = distance * 1.6 / initialVelocity;
                                Vector originalVelocity = new Vector();
                                originalVelocity.setX(initialVelocity);
                                originalVelocity.setY(initialVelocity);
                                tntPrimed.setFuseTicks((int) Math.ceil(time));

                                //Start rotating 2D(x,z) Vector
                                Vector v1 = new Vector(1,0,0);
                                Vector v2 = difference.toVector().setY(0);
                                double angle = v1.angle(v2);
                                boolean flyToRight = -v1.getX() * difference.getZ() + v1.getZ() * difference.getX() > 0;
                                if(flyToRight)angle = -angle;

                                double cos = Math.cos(angle);
                                double sin = Math.sin(angle);

                                //Using round to solve potential issue, like Math.cos(Math.toRadians(90)) is 6.1 when it's actually 0
                                if(cos>1.0 || cos<-1.0){
                                    cos = Math.round(cos);
                                }
                                if(sin>1.0 || sin<-1.0){
                                    sin = Math.round(sin);
                                }
                                double nX = initialVelocity * cos/* - 0 * Math.sin(angle)*/;
                                double nZ = initialVelocity * sin/* + 0 * Math.cos(angle)*/;
                                originalVelocity.setX(nX);
                                originalVelocity.setZ(nZ);
                                tntPrimed.setVelocity(originalVelocity);
                            }
                        }else{
                            player.sendMessage(main.RED + "TNT is not ready yet!");
                        }
                    }else{
                        player.sendMessage(main.RED + "You need to be on ground!");
                    }
                    break;
                case "homing":
                    if(config.homing){
                        player.sendMessage(main.RED + "Projectile Homing is now disabled!");
                    }else{
                        player.sendMessage(main.GREEN + "Projectile Homing is now enabled");
                    }
                    config.homing = !config.homing;
                    break;
                case "select":
                    if(config.selectMode==CombatUtil.RANDOM){
                        player.sendMessage(main.GREEN + "Now selects closest target");
                        config.selectMode = CombatUtil.CLOSEST;
                    }else if(config.selectMode==CombatUtil.CLOSEST){
                        player.sendMessage(main.GREEN + "Now selects target you're looking at");
                        config.selectMode = CombatUtil.LOOKING;
                    }else if(config.selectMode==CombatUtil.LOOKING){
                        player.sendMessage(main.GREEN + "Now selects random target");
                        config.selectMode = CombatUtil.RANDOM;
                    }
                    break;
                case "fishingmode":
                    if(config.fishingMode==CombatUtil.DEFAULT){
                        player.sendMessage(main.GREEN + "Now fishing rod hook hooks every target");
                        config.fishingMode = CombatUtil.ALL;
                    }else if(config.fishingMode==CombatUtil.ALL){
                        player.sendMessage(main.GREEN + "Now fishing rod shoot grappling hook");
                        config.fishingMode = CombatUtil.GRAPPLINGHOOK;
                    }else if(config.fishingMode==CombatUtil.GRAPPLINGHOOK){
                        player.sendMessage(main.GREEN + "Now fishing rod has default behavior");
                        config.fishingMode = CombatUtil.DEFAULT;
                    }
                    config.isFishingRodThrown = false;
                    for(World world : Bukkit.getWorlds()){
                        for(Entity entity : world.getEntities()){
                            if(entity instanceof FishHook){
                                FishHook hook = (FishHook) entity;
                                if(hook.getShooter() == player)hook.remove();
                            }
                        }
                    }
                    break;
                case "cooldown":
                    if(config.hasCoolDown){
                        player.sendMessage(main.GREEN + "Now CoolDown is removed!");
                    }else{
                        player.sendMessage(main.RED + "Now applying CoolDown time");
                    }
                    config.hasCoolDown = !config.hasCoolDown;
                    break;
                case "jump":
                    if(config.doubleJump){
                        player.sendMessage(main.RED + "Now double jumping is disabled!");
                    }else{
                        player.sendMessage(main.GREEN + "Now double jumping is enabled!");
                    }
                    config.doubleJump = !config.doubleJump;
                    break;
                case "fallimpact":
                    if(config.fallImpact){
                        player.sendMessage(main.RED + "Now fall impact is disabled!");
                    }else{
                        player.sendMessage(main.GREEN + "Now fall impact is enabled!");
                    }
                    config.fallImpact = !config.fallImpact;
                    break;
                case "burst":
                    if(config.burst){
                        player.sendMessage(main.RED + "Now burst Fire is disabled!");
                    }else{
                        player.sendMessage(main.GREEN + "Now burst Fire is enabled!");
                    }
                    if(!config.burstWithMultishot && config.multiShot)config.multiShot = false;
                    config.burst = !config.burst;
                    break;
                case "burstmultishot":
                    if(config.burstWithMultishot){
                        player.sendMessage(main.GREEN + "Now you cannot enable both burst and multishot(balance breaking)");
                    }else{
                        player.sendMessage(main.RED + "Now you can enable both burst and multishot");
                    }
                    config.burstWithMultishot = !config.burstWithMultishot;
                    break;
                default:
                    player.sendMessage(main.RED + "ERROR! there's no handler for known command! this is plugin's fault! contact developer!");
                    break;
            }
            return true;
        }else{
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        //create new array
        final List<String> completions = new ArrayList<>();
        //copy matches of first argument from list (ex: if first arg is 'm' will return just 'minecraft')
        StringUtil.copyPartialMatches(args[0], Arrays.asList(allCommands), completions);
        //sort the list
        Collections.sort(completions);
        return completions;
    }
    //Below is to make things easier when writing help, this should be equal as ones from top of this file to avoid confusion
    //    private final String[] targetCommands = new String[]{"lift", "goback", "tnt", "homing"};
    //    private final String[] nonTargetCommands = new String[]{"help", "target", "arrow", "multishot", "select", "fishingmode", "cooldown", "jump", "fallimpact", "burst", "burstmultishot"};
    private void displayHelp(Player player){
        player.sendMessage(main.GREEN + "Usage: /tcombat <argument>");
        player.sendMessage(main.GREEN + "Non-target commands:");
        player.sendMessage(main.GREEN + "help: Displays this message");
        player.sendMessage(main.GREEN + "target [playername]: Sets targets to visible entities or the player, player will be added to existing target list");
        player.sendMessage(main.GREEN + "arrow: Toggles arrow avoiding, looking arrow directly will return arrow to the shooter");
        player.sendMessage(main.GREEN + "burst: Toggles whether burst 5 arrows when you fire one, cannot be used with multishot");
        player.sendMessage(main.GREEN + "multishot: Toggles whether fire 15~25 more arrows when you fire one, cannot be used with burst, and has 4 seconds cool down which can be disabled");
        player.sendMessage(main.GREEN + "burstmultishot: Makes burst and multishot to be able to be enabled together");
        player.sendMessage(main.GREEN + "select: Cycles selection mode for skills that only work for one target");
        player.sendMessage(main.GREEN + "fishingmode: Cycles fishing modes, pressing shift while pulling grappling hook will pull hooked entity towards you");
        player.sendMessage(main.GREEN + "cooldown: Toggles whether enable cool down or not");
        player.sendMessage(main.GREEN + "jump: Toggles whether enable double jump or not, only works in survival or adventure mode");
        player.sendMessage(main.GREEN + "fallimpact: Toggles whether enable fall impact or not, not only you need to be fast enough, but also you need to fall more than 15 blocks");
        player.sendMessage(main.GREEN + "Target commands:");
        player.sendMessage(main.GREEN + "lift: Lifts ground below targets, you need to target at least 1 entity to use this");
        player.sendMessage(main.GREEN + "goback: Teleports you to back of target, only works for one target, which target you'll be teleport to depends on selection mode");
        player.sendMessage(main.GREEN + "tnt: Throws tnt at single target with 45°, and has 4 seconds cool down which can be disabled. Trajectory of tnt may be inaccurate");
        player.sendMessage(main.GREEN + "homing: Changes arrows you shot into homing arrows, can be used with multishot or burst");
    }
}
