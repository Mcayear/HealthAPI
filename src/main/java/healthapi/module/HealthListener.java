package healthapi.module;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityRegainHealthEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import healthapi.HealTask;
import healthapi.HealthMainClass;
import healthapi.PlayerHealth;

import java.util.LinkedList;

/**
 * @author SmallasWater
 */
public class HealthListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player =  event.getPlayer();
        if(!HealthMainClass.MAIN_CLASS.worlds.contains(player.getLevel().getFolderName())){
            PlayerHealth health = PlayerHealth.getPlayerHealth(player.getName());
            if(health.isDeath()){
                health.setDeath(false);
            }
            player.setHealth(health.getPlayerHealth());
            if(player.getMaxHealth() != HealthMainClass.MAIN_CLASS.getDefaultHealth()) {
                player.setMaxHealth(HealthMainClass.MAIN_CLASS.getDefaultHealth());
            }
        }
        if(HealthMainClass.MAIN_CLASS.getConfig().getBoolean("生命恢复.是否开启",true)) {
            Server.getInstance().getScheduler().scheduleRepeatingTask(new HealTask(player, HealthMainClass.MAIN_CLASS)
                    , HealthMainClass.MAIN_CLASS.getConfig().getInt("生命恢复.间隔(刻)", 60));

        }

    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player) || HealthMainClass.MAIN_CLASS.worlds.contains(entity.getLevel().getFolderName())) {
            return;
        }
        PlayerHealth health = PlayerHealth.getPlayerHealth((Player)entity);
        if (event instanceof EntityDamageByBlockEvent) {// 方块造成的伤害
            // todo: 当nukkit未来修复之后可以取消掉这个部分
            // time > 当前时间 则取消这次伤害
            if (health.getBlockDamageCool() > System.currentTimeMillis()) {
                event.setCancelled();
                return;
            } else {
                health.setBlockDamageCool(System.currentTimeMillis()+1500);
            }
        }
        float damage = event.getFinalDamage();
        if (damage < 0) {
            damage = 1.0F;
        }
        if (health.isDeath()) {// 判断是否死亡
            event.setCancelled();
            if(entity.isAlive()){
                entity.setHealth(0);
            }
            return;
        }
        health.setDamageHealth(damage);
        if(health.isDeath()) {
            event.setCancelled();
            return;
        }
        double remove = (double)damage / (double)health.getMaxHealth();
        double damages = remove * (double)entity.getMaxHealth();
        if ((int)entity.getHealth() == 8 && (int)health.getHealth() > 8) {
            damages = 1;
        }
        if(damages < 0){
            damages = 1.0f;
        }
        event.setDamage((float) damages);
        entity.setHealth(health.getPlayerHealth());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        Player entity = event.getEntity();
        PlayerHealth health = PlayerHealth.getPlayerHealth(entity);
        if(!health.isDeath() && entity.isAlive()) {
            health.setDeath(true);
            health.setHealth(0,true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        if(player != null) {
            PlayerHealth health = PlayerHealth.getPlayerHealth(player);
            if(health.isDeath()){
                health.reset();
            }
            health.save();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerRespawnEvent event){
        Player entity = event.getPlayer();
        PlayerHealth health = PlayerHealth.getPlayerHealth(entity);
        if(health.isDeath()) {
            health.reset();
        }
    }



    @EventHandler
    public void onAddHealth(EntityRegainHealthEvent event){
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            if(!HealthMainClass.MAIN_CLASS.worlds.contains(entity.getLevel().getFolderName())) {
                event.setCancelled();
                if(HealthMainClass.MAIN_CLASS.getConfig().getBoolean("是否关闭饱食度回血",false)){
                    if (event.getRegainReason() == EntityRegainHealthEvent.CAUSE_EATING) {
                        return;
                    }
                }
                PlayerHealth health = PlayerHealth.getPlayerHealth((Player) entity);
                health.heal(event.getAmount());

            }
        }
    }
}
