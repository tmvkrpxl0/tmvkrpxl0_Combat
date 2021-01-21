package tmvkrpxl0.src;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.server.v1_16_R3.EntityFishingHook;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.Tuple;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CombatListener implements Listener {
    tmvkrpxl0Combat main;
    //Function determines if entity should be deleted from processedEntity, true means delete
    private final Set<Arrow> deflectedArrows = new ConcurrentSet<>();
    private final Map<Entity, Function<Entity, Boolean>> processedEntities = new ConcurrentHashMap<>();
    private final Map<Arrow, BukkitRunnable> homingArrow = new ConcurrentHashMap<>();
    private final Set<Arrow> zeroNoDamageTickArrows = new ConcurrentSet<>();
    private final Map<Player, Tuple<LivingEntity,Integer>> striked = new HashMap<>();
    private final Map<Player, Set<CustomHook>> playerHooks = new HashMap<>();
    private final Set<Player> falling = new HashSet<>();
    CombatListener(tmvkrpxl0Combat main) {
        this.main = main;
        new BukkitRunnable(){
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()){
                    PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
                    if(config.avoidArrow) {
                        for(Entity entity : player.getNearbyEntities(2, 2, 2)){
                            if(entity instanceof Arrow){
                                Projectile projectile = (Projectile) entity;
                                if(projectile.getShooter() == player)continue;
                                if(homingArrow.containsKey(entity)){
                                    homingArrow.get(entity).cancel();
                                    homingArrow.remove(entity);
                                }
                                if(!entity.isOnGround()){
                                    if(!deflectedArrows.contains(entity)){
                                        Vector eyeVector = player.getEyeLocation().toVector();
                                        Vector direction = player.getEyeLocation().getDirection();
                                        Vector entityVector = entity.getLocation().toVector();
                                        Vector difference = entityVector.subtract(eyeVector);
                                        double angle = Math.toDegrees(direction.angle(difference));
                                        if(angle<90){
                                            if(projectile.getShooter() != player){
                                                ProjectileSource source = projectile.getShooter();
                                                if(source instanceof LivingEntity){
                                                    LivingEntity shooter = (LivingEntity) source;
                                                    Vector shooterDifference = shooter.getEyeLocation().toVector().subtract(projectile.getLocation().toVector());
                                                    double distance = shooter.getEyeLocation().distance(player.getLocation());
                                                    distance /= 20;
                                                    shooterDifference.multiply(distance);
                                                    projectile.setVelocity(shooterDifference);
                                                }
                                            }
                                        }else{
                                            Vector noY = direction.setY(0);
                                            boolean flyToLeft = -noY.getX() * difference.getZ() + noY.getZ() * difference.getX() > 0;
                                            double radian = Math.toRadians(flyToLeft ? 90 : -90);
                                            Vector velocity = entity.getVelocity();
                                            double x = velocity.getX();
                                            double z = velocity.getZ();
                                            double sin = Math.sin(radian);
                                            double cos = Math.cos(radian);
                                            velocity.setX(cos * x - sin * z);
                                            velocity.setZ(sin * x + cos * z);
                                            entity.setVelocity(velocity);
                                        }
                                        deflectedArrows.add((Arrow)entity);
                                    }

                                }
                            }
                        }
                    }
                    if(playerHooks.containsKey(player)){
                        Set<CustomHook> hooks = playerHooks.get(player);
                        if(hooks.size()==0){
                            config.isFishingRodThrown=false;
                        }else{
                            for(CustomHook hook : new HashSet<>(hooks)){
                                if(hook.dead || !hook.valid){
                                    hooks.remove(hook);
                                }
                            }
                        }
                    }
                }
                for(World world : Bukkit.getWorlds()){
                    for(Entity entity : world.getEntities()){
                        if(entity instanceof Arrow){
                            Arrow projectile = (Arrow) entity;
                            if(projectile.getShooter() instanceof Player){
                                Player player = (Player) projectile.getShooter();
                                if(!homingArrow.containsKey(entity)){
                                    if(projectile.getPassengers().size()!=0 && projectile.getPassengers().get(0) instanceof FishHook)continue;
                                    PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
                                    if(config.homing && config.targets.size()>0){
                                        LivingEntity target = CombatUtil.pickTarget(player, projectile.getLocation());
                                        if(target==null){
                                            continue;
                                        }
                                        homingArrow.put(projectile,new HomingTask(projectile, target, main));
                                    }
                                }else{
                                    if(!entity.isValid() || entity.isOnGround() || entity.getVelocity().distanceSquared(main.ZERO) <= 0.07)homingArrow.remove(entity);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(main, 0, 1);
        new BukkitRunnable(){
            @Override
            public void run(){
                for (Arrow entity : new HashSet<>(deflectedArrows)) {
                    if(entity.isOnGround() || !entity.isValid()) deflectedArrows.remove(entity);
                }
                for(Arrow entity : new HashSet<>(zeroNoDamageTickArrows)){
                    if(entity.isOnGround() || !entity.isValid()) zeroNoDamageTickArrows.remove(entity);
                }
                for(Entity entity : new HashSet<>(processedEntities.keySet())){
                    if(processedEntities.get(entity).apply(entity))processedEntities.remove(entity);
                }
                for(PlayerConfig config : main.playerConfigs.values()){
                    for(LivingEntity livingEntity : new HashSet<>(config.targets)){
                        if(!livingEntity.isValid())config.targets.remove(livingEntity);
                    }
                }
                Iterator<Player> itr2 = striked.keySet().iterator();
                while(itr2.hasNext()){
                    Player player = itr2.next();
                    Tuple<LivingEntity, Integer> temp = striked.get(player);
                    if(temp.b()==0){
                        itr2.remove();
                        continue;
                    }
                    striked.put(player, new Tuple<>(temp.a(), temp.b()-1));
                }
            }
        }.runTaskTimerAsynchronously(main, 0, 1);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event){
        if(event.getEntity() instanceof Arrow && event.getEntity().getShooter() instanceof Player){
            Player player = (Player) event.getEntity().getShooter();
            PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
            if(config.multiShot){
                if(!config.hasCoolDown || System.currentTimeMillis() > config.multiShotCoolDown){
                    config.multiShotCoolDown = System.currentTimeMillis() + 4000;
                    Inventory inventory = player.getInventory();
                    ItemStack oneArrow = new ItemStack(Material.ARROW, 1);
                    World world = event.getEntity().getWorld();
                    Projectile projectile = event.getEntity();
                    Random random = new Random();
                    int maxProjectile = Math.abs(random.nextInt());
                    maxProjectile %= 10;
                    maxProjectile += 15;
                    for(int i = 0;i<maxProjectile;i++){
                        if(!player.getInventory().contains(Material.ARROW)){
                            player.sendMessage(main.RED + "You don't have enough arrow!");
                            return;
                        }
                        Location spawnLocation = projectile.getLocation().clone().add(random.nextInt()%7, (random.nextInt()%5)/10.0, random.nextInt()%7);
                        Arrow spawned = (Arrow) world.spawnEntity(spawnLocation, EntityType.ARROW);
                        spawned.setVelocity(projectile.getVelocity());
                        spawned.setShooter(projectile.getShooter());
                        spawned.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        zeroNoDamageTickArrows.add(spawned);
                        inventory.removeItem(oneArrow);
                        player.playSound(spawnLocation, Sound.ENTITY_ARROW_SHOOT, 1, 1);
                    }
                }else{
                    event.setCancelled(true);
                    player.sendMessage(main.RED + "Multishot is not ready yet!");
                }
            }
            if(config.burst && !zeroNoDamageTickArrows.contains(event.getEntity())){
                Vector v = event.getEntity().getVelocity();
                World world = event.getLocation().getWorld();
                ItemStack oneArrow = new ItemStack(Material.ARROW, 1);
                final Location spawnLocation = event.getLocation().clone();
                final int[] i = {0};
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        if(i[0] ==5){
                            cancel();
                            return;
                        }else{
                            if(!player.getInventory().contains(Material.ARROW)){
                                player.sendMessage(main.RED + "You don't have enough arrow!");
                                cancel();
                                return;
                            }
                            Arrow arrow = (Arrow) world.spawnEntity(spawnLocation, EntityType.ARROW);
                            arrow.setVelocity(v);
                            arrow.setShooter(player);
                            zeroNoDamageTickArrows.add(arrow);
                            ProjectileLaunchEvent projectileLaunchEvent = new ProjectileLaunchEvent(arrow);
                            Bukkit.getPluginManager().callEvent(projectileLaunchEvent);
                            player.getInventory().removeItem(oneArrow);
                            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1);
                        }
                        i[0]++;
                    }
                }.runTaskTimer(main, 0, 2);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        main.playerConfigs.putIfAbsent(event.getPlayer().getUniqueId(), new PlayerConfig());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
    }

    @EventHandler
    public void onFlight(PlayerToggleFlightEvent event){
        Player player = event.getPlayer();
        if(player.getGameMode().equals(GameMode.SPECTATOR) || player.getGameMode().equals(GameMode.CREATIVE))return;
        PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
        if(config.doubleJump){
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFallDistance(0);
            Vector direction = player.getEyeLocation().getDirection();
            if(direction.getY()<0)direction.setY(0.3);
            if(direction.getY()>0.8)direction.setY(0.8);
            direction.multiply(2);
            player.setVelocity(direction);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.05F, 2F);
            player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
            new BukkitRunnable(){
                @Override
                public void run() {
                    player.setAllowFlight(true);
                }
            }.runTaskLater(main, 10);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event){
        Player player = event.getPlayer();
        PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
        if(((LivingEntity)player).isOnGround()){
            if(falling.contains(player)){
                falling.remove(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 100));
                player.getWorld().createExplosion(player.getLocation(), (player.getFallDistance()*2)/15, false, true, player);
                player.setFallDistance(0);
            }
            if(config.doubleJump){
                player.setAllowFlight(true);
            }
        }
        if(config.doubleJump){
            if(((LivingEntity)player).isOnGround())player.setAllowFlight(true);
        }
        double speed = Math.abs(event.getFrom().distance(event.getTo()));
        if(speed>1.2) {
            Vector playerLocation = player.getLocation().toVector();
            for (Entity entity : player.getNearbyEntities(1, 1, 1)) {
                if (entity instanceof LivingEntity) {
                    if(processedEntities.containsKey(entity))continue;
                    LivingEntity target = (LivingEntity)entity;
                    Vector entityLocation = entity.getLocation().toVector();
                    Vector difference = entityLocation.subtract(playerLocation);
                    double angle = difference.angle(player.getVelocity());
                    if(angle <= 15){
                        processedEntities.put(target, entity1 -> entity1.getLocation().distanceSquared(player.getLocation()) > 1.5);
                        target.setNoDamageTicks(0);
                        target.damage(2*speed, player);
                        target.setVelocity(target.getVelocity().add(difference.clone().normalize()));
                        target.setNoDamageTicks(0);
                        if(striked.containsKey(player) && striked.get(player).a()==target){
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 2));
                        }
                    }
                }
            }
            if(config.fallImpact){
                if(!((LivingEntity)player).isOnGround()){
                    if(player.getFallDistance()>15){
                        if(Math.floor(event.getFrom().getY())>Math.floor(event.getTo().getY())){
                            if(event.getFrom().getBlock().getType().equals(Material.AIR)){
                                falling.add(player);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event){
        if(event.getEntity() instanceof TNTPrimed){
            TNTPrimed primed = (TNTPrimed) event.getEntity();
            if(primed.getSource() instanceof Player){
                ((Player) primed.getSource()).addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 100));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof Player){
            if(event.getCause().equals(EntityDamageEvent.DamageCause.FALL))event.setDamage(0);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity){
            if(processedEntities.containsKey(event.getEntity()))return;
            Player player = (Player) event.getDamager();
            LivingEntity target = (LivingEntity)event.getEntity();
            striked.put(player, new Tuple<>(target, 8));
        }
        if(event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)){
            Projectile projectile = (Projectile) event.getDamager();
            if(projectile.getPassengers().size()!=0){
                Entity passenger = projectile.getPassengers().get(0);
                if(((CraftEntity)passenger).getHandle() instanceof CustomHook){
                    event.setDamage(0);
                }
            }
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event){
        Player player = event.getPlayer();
        PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
        WorldServer worldServer = ((CraftWorld)player.getWorld()).getHandle();
        EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        if(config.fishingMode==CombatUtil.ALL){
            event.setCancelled(true);
            if (config.isFishingRodThrown) {
                Set<Entity> temp = new HashSet<>();
                for(CustomHook customHook : playerHooks.get(player)){
                    net.minecraft.server.v1_16_R3.Entity nmsHooked = customHook.getHooked();
                    if(nmsHooked!=null){
                        Entity hooked = nmsHooked.getBukkitEntity();
                        if(!temp.contains(hooked)){
                            Vector v = player.getLocation().toVector().subtract(hooked.getLocation().toVector()).multiply(0.2);
                            hooked.setVelocity(hooked.getVelocity().add(v));
                            temp.add(hooked);
                        }
                    }
                    customHook.getBukkitEntity().remove();
                }
                config.isFishingRodThrown = false;
            } else {
                Set<CustomHook> toPut = new HashSet<>();
                for(LivingEntity target : config.targets){
                    if(player.hasLineOfSight(target)){
                        Vector v = new Vector(0,0,0);
                        CustomHook customHook = new CustomHook(entityPlayer, worldServer, target.getEyeLocation(), v, false);
                        customHook.onEntityHit(((CraftLivingEntity)target).getHandle());
                        //customHook.setHookedEntity(((CraftLivingEntity)target).getHandle());
                        worldServer.addEntity(customHook);
                        config.isFishingRodThrown = true;
                        toPut.add(customHook);
                    }
                }
                playerHooks.put(player, toPut);
            }
        }else if(config.fishingMode == CombatUtil.GRAPPLINGHOOK){
            event.setCancelled(true);
            if(config.isFishingRodThrown){
                CustomHook customHook = playerHooks.get(player).iterator().next();
                CraftFishHook hook = (CraftFishHook) customHook.getBukkitEntity();
                if(customHook.pullable) {
                    if(player.isSneaking()){
                        if(customHook.getHooked()!=null){
                            Entity hooked = customHook.getHooked().getBukkitEntity();
                            Vector v = player.getLocation().toVector().subtract(hooked.getLocation().toVector()).multiply(0.2);
                            hooked.setVelocity(v);
                        }else{
                            Vector v = hook.getLocation().toVector().subtract(player.getLocation().toVector()).multiply(0.2);
                            player.setVelocity(player.getVelocity().add(v));
                        }
                    }else{
                        Vector v = hook.getLocation().toVector().subtract(player.getLocation().toVector()).multiply(0.2);
                        player.setVelocity(player.getVelocity().add(v));
                    }
                }
                customHook.arrow.remove();
                hook.remove();
            }else{
                Location spawnLocation = player.getEyeLocation().clone();
                spawnLocation.add(spawnLocation.getDirection());
                Vector direction = player.getEyeLocation().getDirection();
                CustomHook customHook = new CustomHook(entityPlayer, worldServer, spawnLocation, direction, true);
                customHook.setNoGravity(true);
                worldServer.addEntity(customHook);
                Set<CustomHook> tempSet = new HashSet<>();
                tempSet.add(customHook);
                playerHooks.put(player, tempSet);
            }
            config.isFishingRodThrown=!config.isFishingRodThrown;
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event){
        Projectile projectile = event.getEntity();
        if(projectile.getShooter() instanceof Player){
            Player player = (Player)projectile.getShooter();
            PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
            if(projectile instanceof Arrow){
                List<Entity> passengers = projectile.getPassengers();
                if(passengers.size()!=0){
                    net.minecraft.server.v1_16_R3.Entity nmsPassenger = ((CraftEntity)passengers.get(0)).getHandle();
                    if(nmsPassenger instanceof CustomHook){
                        CustomHook customHook = (CustomHook)nmsPassenger;
                        customHook.pullable = true;
                        if(event.getHitEntity()!=null){
                            customHook.onEntityHit(((CraftEntity)event.getHitEntity()).getHandle());
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    }
                }
                if(event.getHitEntity() instanceof LivingEntity){
                    LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
                    if(hitEntity == player){
                        player.setNoDamageTicks(2);
                        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                        projectile.remove();
                        return;
                    }
                    if(zeroNoDamageTickArrows.contains(event.getEntity())){
                        if(event.getHitEntity() instanceof LivingEntity){
                            if(hitEntity.getNoDamageTicks()!=0){
                                if(!event.getEntity().isDead()){
                                    hitEntity.setNoDamageTicks(0);
                                }else{
                                    player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                                    projectile.remove();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            if(projectile instanceof WitherSkull){
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 100));
                projectile.getWorld().createExplosion(projectile.getLocation(), 4F, false, true, projectile);
            }
            if(homingArrow.containsKey(projectile) || zeroNoDamageTickArrows.contains(projectile)){
                if(event.getHitEntity()==null){
                    player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                }
                projectile.remove();
            }
        }

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event){
        for(UUID uuid : main.playerConfigs.keySet()){
            main.playerConfigs.get(uuid).targets.remove(event.getEntity());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)){
            if(event.getItem() != null && event.getItem().getType().equals(Material.WITHER_SKELETON_SKULL)){
                PlayerConfig config = main.playerConfigs.get(event.getPlayer().getUniqueId());
                if(!config.hasCoolDown || System.currentTimeMillis() > config.skullCoolDown){
                    Location location = event.getPlayer().getEyeLocation().clone();
                    location.add(location.getDirection());
                    WitherSkull skull = (WitherSkull) location.getWorld().spawnEntity(location, EntityType.WITHER_SKULL);
                    skull.setDirection(location.getDirection());
                    Vector v = location.getDirection().multiply(3).clone();
                    skull.setVelocity(v);
                    skull.setCharged(true);
                    skull.setShooter(event.getPlayer());
                    skull.setGravity(false);
                    config.skullCoolDown = System.currentTimeMillis() + 10000;
                    processedEntities.put(skull, new Function<Entity, Boolean>() {
                        @Override
                        public Boolean apply(Entity entity) {
                            entity.setVelocity(v);
                            if(entity.getLocation().distanceSquared(event.getPlayer().getLocation()) > 10000){
                                entity.remove();
                            }
                            return !entity.isValid();
                        }
                    });
                }else{
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1, 2);
                    event.getPlayer().sendMessage(main.RED + "Wither skull is not reloaded yet!");
                }
            }
        }
    }

    protected void cleanUp(){
        for(Player player : playerHooks.keySet()){
            for(EntityFishingHook hook : playerHooks.get(player)){
                hook.getBukkitEntity().remove();
            }
        }
    }

}
