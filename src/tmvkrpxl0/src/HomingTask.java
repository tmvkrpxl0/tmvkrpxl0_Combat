package tmvkrpxl0.src;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HomingTask extends BukkitRunnable {
    private static final double MaxRotationAngle = 0.24D;

    private static final double TargetSpeed = 0.14D;

    Arrow arrow;

    tmvkrpxl0Combat main;

    LivingEntity target;

    public HomingTask(Arrow arrow, LivingEntity target, tmvkrpxl0Combat plugin) {
        this.arrow = arrow;
        this.target = target;
        runTaskTimer(plugin, 1L, 1L);
        main = plugin;
    }

    public void run() {
        try{
            Vector newVelocity;
            double speed = this.arrow.getVelocity().length();
            if (this.arrow.isOnGround() || this.arrow.isDead() || this.target.isDead() || this.arrow.getVelocity().distanceSquared(main.ZERO) <= 0.07) {
                cancel();
                return;
            }
            Vector toTarget = this.target.getLocation().clone().add(new Vector(0.0D, 0.5D, 0.0D))
                    .subtract(this.arrow.getLocation()).toVector();
            Vector dirVelocity = this.arrow.getVelocity().clone().normalize();
            Vector dirToTarget = toTarget.clone().normalize();
            double angle = dirVelocity.angle(dirToTarget);
            double newSpeed = 0.9D * speed + TargetSpeed;
            if (this.target instanceof Player) {
                this.arrow.getLocation().distance(this.target.getLocation());
            }
            if (angle < MaxRotationAngle) {
                newVelocity = dirVelocity.clone().multiply(newSpeed);
            } else {
                Vector newDir = dirVelocity.clone().multiply((angle - MaxRotationAngle) / angle)
                        .add(dirToTarget.clone().multiply(MaxRotationAngle / angle));
                newDir.normalize();
                newVelocity = newDir.clone().multiply(newSpeed);
            }
            this.arrow.setVelocity(newVelocity.add(new Vector(0.0D, 0.03D, 0.0D)));
        }catch(IllegalArgumentException exception){
            cancel();
            this.arrow.remove();
        }
    }
}