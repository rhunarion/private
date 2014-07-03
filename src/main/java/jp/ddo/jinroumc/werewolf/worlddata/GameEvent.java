package jp.ddo.jinroumc.werewolf.worlddata;

import jp.ddo.jinroumc.werewolf.enumconstant.VillageRole;
import jp.ddo.jinroumc.werewolf.enumconstant.VillageStatus;
import jp.ddo.jinroumc.werewolf.enumconstant.VillageTime;
import jp.ddo.jinroumc.werewolf.util.C;
import jp.ddo.jinroumc.werewolf.village.Village;
import jp.ddo.jinroumc.werewolf.village.VillagePlayer;
import jp.ddo.jinroumc.werewolf.village.VillageUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GameEvent implements Listener {
	@EventHandler
	public void onPlayerAttack(EntityDamageByEntityEvent event){
		Entity attacker = event.getDamager();
		if(!(attacker instanceof Player))
			return;
		if(!VillageUtil.isInVillage((Player) attacker)){
			event.setCancelled(true);
			return;
		}
			
		Village vil = VillageUtil.getVillage((Player) attacker);
		VillagePlayer attackerVp = vil.getPlayer((Player) attacker); 

		Entity defender = event.getEntity();
		VillagePlayer defenderVp = null;
		if(defender instanceof Player){
			defenderVp = vil.getPlayer((Player) defender);
		}else if(defender.getType().equals(EntityType.VILLAGER)){
			for(VillagePlayer vp : vil.getNPCList())
				if(vp.villagerEntity==defender)
					defenderVp = vp;
		}else{
			return;
		}
		
		if(attackerVp.alive && defenderVp.alive  
				&& attackerVp.role==VillageRole.jinrou
				&& defenderVp.role!=VillageRole.jinrou
				&& vil.status==VillageStatus.ongoing && vil.time==VillageTime.night){
			
			if(vil.tryBiting){
				attackerVp.sendMessage(C.red+"Error: 今夜はすでに一人噛んでいます。");
			}
			else if(vil.day==0 && !defenderVp.getName().equals("Mr.Firvic")){
				attackerVp.sendMessage(C.red+"Error: 0日目の夜は Mr.FirstVictm しか噛み殺すことができません。");
			}else{
				attackerVp.bitePlayer(defenderVp);
			}
		}

		event.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerDeath(EntityDeathEvent event){
		Entity entity = event.getEntity();
		Location loc = entity.getLocation();
		if(!VillageUtil.isVillageName(loc.getWorld().getName()))
			return;
			
		final Village vil = VillageUtil.getVillage(loc.getWorld().getName());
		if(vil.status!=VillageStatus.ongoing
				|| vil.time!=VillageTime.execution
				|| !DefaultVillageData.isInsideScaffold(loc)){
			if(entity.getType().equals(EntityType.VILLAGER)
					&& ((LivingEntity) entity).isCustomNameVisible()){
				for(VillagePlayer npc : vil.getNPCList()){
					if(npc.villagerEntity==entity
							&& (vil.status!=VillageStatus.ongoing || npc.alive)){
						npc.spawnVillager();
						return;
					}
				}
				return;
			}else{
				return;
			}
		}
		
		VillagePlayer vp = null;
		if(entity instanceof Player){
			vp = vil.getPlayer((Player) entity);
		}else if(entity.getType().equals(EntityType.VILLAGER)){
			for(VillagePlayer npc : vil.getAliveNPCList())
				if(npc.villagerEntity==entity)
					vp = npc;
		}else{
			return;
		}
		
		if(vp!=vil.executedPlayer)
			return;
		vp.kill();
		vil.sendToVillage(vp.color+vp.getName()
				   +C.green+" さんが処刑されました。間もなく夜が訪れます。");

		vil.doTaskLaterID = Bukkit.getScheduler().runTaskLater(vil.plugin, new BukkitRunnable(){
			@Override
			public void run(){
				DefaultVillageData.postExecution(vil);
				vil.checkResult();
				vil.doTaskLaterID = -1;
			}
		}, 100).getTaskId();
	}

	@EventHandler
	public void onGhostPickupItem(PlayerPickupItemEvent event){
		Player pl = event.getPlayer();
		if(!VillageUtil.isInVillage(pl))
			return;
		
		Village vil = VillageUtil.getVillage(pl);
		VillagePlayer vp = vil.getPlayer(pl);
		if(!vp.alive && vil.status==VillageStatus.ongoing)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void onGhostDropItem(final PlayerDropItemEvent event){
		Player pl = event.getPlayer();
		if(!VillageUtil.isInVillage(pl))
			return;
		
		Village vil = VillageUtil.getVillage(pl);
		VillagePlayer vp = vil.getPlayer(pl);
		if(!vp.alive && vil.status==VillageStatus.ongoing)
			event.setCancelled(true);

		Bukkit.getScheduler().runTaskLater(vil.plugin, new BukkitRunnable(){
			@SuppressWarnings("deprecation")
			@Override
			public void run(){
				event.getPlayer().updateInventory();
			}
		}, 1);
	}
	
	@EventHandler
	public void onGhostInteract(PlayerInteractEvent event){
		Player pl = event.getPlayer();
		if(!VillageUtil.isInVillage(pl))
			return;
				
		Village vil = VillageUtil.getVillage(pl);
		VillagePlayer vp = vil.getPlayer(pl);
		if(!vp.alive && vil.status==VillageStatus.ongoing)
			event.setCancelled(true);
	}

	public static void removeDoorSound(JavaPlugin plugin){
/*		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
			ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
				@Override
				public void onPacketSending(PacketEvent event) {
					System.out.println(""+event.getPacket().getStrings().size());
					System.out.println(""+event.getPacket().getStrings().readSafely(0));

					
					
					Player pl = event.getPlayer();
					if(!VillageUtil.isInVillage(pl))
							return;
					
					//Village vil = VillageUtil.getVillage(pl);
					//VillagePlayer vp = vil.getPlayer(pl); 

					
					
					String soundName = event.getPacket().getStrings().read(0);
					if (soundName.equals("random.door_open")
							|| soundName.equals("random.door_close")) {
						event.setCancelled(true);int i;
					}
				}
			}
		);*/
	}
}
