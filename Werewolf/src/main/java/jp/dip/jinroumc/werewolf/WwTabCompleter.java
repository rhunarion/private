package jp.dip.jinroumc.werewolf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.dip.jinroumc.werewolf.command.CommandHelp;
import jp.dip.jinroumc.werewolf.enumconstant.VillageRole;
import jp.dip.jinroumc.werewolf.enumconstant.VillageStatus;
import jp.dip.jinroumc.werewolf.enumconstant.VillageTime;
import jp.dip.jinroumc.werewolf.util.PermissionChecker;
import jp.dip.jinroumc.werewolf.village.Village;
import jp.dip.jinroumc.werewolf.village.VillagePlayer;
import jp.dip.jinroumc.werewolf.village.VillageUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;

public class WwTabCompleter implements TabCompleter {
	private static List<String> booleanList = new ArrayList<String>();
		
	static{
		booleanList.add("true");
		booleanList.add("false");
	}
	
	public List<String> onTabComplete(CommandSender sender, Command cmd, String cmdLabel, String[] args){
		return getCompletionList(sender, cmd, cmdLabel, args);
	}
	
	public static List<String> getCompletionList(CommandSender sender, Command cmd, String cmdLabel, String[] args){
		if(args.length==1){
			if(sender instanceof ConsoleCommandSender){
				return getPartialMatchesToLowerCase(args, CommandHelp.getCommandList());
			}
			if(sender instanceof Player){
				Player pl = (Player) sender;
				return getPartialMatchesToLowerCase(args, PermissionChecker.getCommandList(pl));
			}
		}
		
		if(args.length==2 && args[0].equalsIgnoreCase("help")){
			List<String> compList = CommandHelp.getCommandList();
			compList.add("all");
			return getPartialMatchesToLowerCase(args, compList); 
		}

		if(args.length==3 && args[0].equalsIgnoreCase("help")
				&& args[1].equalsIgnoreCase("chRule"))
			return getPartialMatchesToLowerCase(args, CommandHelp.getRuleList());

		if(sender instanceof ConsoleCommandSender)
			return ImmutableList.of();
		Player pl = (Player) sender;
		if(!VillageUtil.isInVillage(pl)){
			if(args.length==2 && args[0].equalsIgnoreCase("enterVil"))
				return getPartialMatches(args, VillageUtil.getVillageNameList());

			if(args.length==2 && args[0].equalsIgnoreCase("makeVil"))
				return getPartialMatches(args, VillageUtil.getVillageTypeList());
			
			return ImmutableList.of();
		}
		
		Village vil = VillageUtil.getVillage(pl);
		VillagePlayer vp = vil.getPlayer(pl);
		
		if(args.length==2 && args[0].equalsIgnoreCase("enterVil")
				&& (!vp.alive || vil.status!=VillageStatus.ONGOING))
			return getPartialMatches(args, VillageUtil.getVillageNameList());

		if(args.length==2 && args[0].equalsIgnoreCase("makeVil")
				&& (vil.status==VillageStatus.EMPTY))
			return getPartialMatches(args, VillageUtil.getVillageTypeList());

		if(args.length==2 && args[0].equalsIgnoreCase("giveGM")
				&& (vp.gameMaster))
			return getPartialMatches(args, vil.getPlayerNameListExceptGmAndNpc());

		if(args.length==2 && args[0].equalsIgnoreCase("chRule")
				&& (vp.gameMaster && vil.status!=VillageStatus.FINISHING))
			return getPartialMatchesToLowerCase(args, CommandHelp.getRuleList());
		
		if(args.length==3 && args[0].equalsIgnoreCase("chRule")
				&& (vp.gameMaster && vil.status!=VillageStatus.FINISHING)
				&& (args[1].equalsIgnoreCase("requestRole")
						|| args[1].equalsIgnoreCase("randomVote")
						|| args[1].equalsIgnoreCase("permitWhisp")
						|| args[1].equalsIgnoreCase("reishiAllPlayers")
						|| args[1].equalsIgnoreCase("permitBite")))
			return getPartialMatches(args, booleanList);

		if(args.length==2 && args[0].equalsIgnoreCase("setRole")
				&& (vp.gameMaster
						&& (vil.status==VillageStatus.PREPARING || vil.status==VillageStatus.RECRUITING)))
			return getPartialMatches(args, vil.getJoiningPlayerNameList());

		if(args.length==3 && args[0].equalsIgnoreCase("setRole")
				&& (vp.gameMaster
						&& (vil.status==VillageStatus.PREPARING || vil.status==VillageStatus.RECRUITING)))
			return getPartialMatchesToLowerCase(args, VillageUtil.getRoleListExceptNone());

		if(args.length==2 && args[0].equalsIgnoreCase("unsetRole")
				&& (vp.gameMaster
						&& (vil.status==VillageStatus.PREPARING || vil.status==VillageStatus.RECRUITING)))
			return getPartialMatches(args, vil.getJoiningPlayerNameList());

		if(args.length==2 && args[0].equalsIgnoreCase("requestRole")
				&& (vp.joining && vil.requestRole
						&& (vil.status==VillageStatus.PREPARING || vil.status==VillageStatus.RECRUITING)))
			return getPartialMatchesToLowerCase(args, VillageUtil.getRoleList());

		if(args.length==2 && args[0].equalsIgnoreCase("kick")
				&& (vp.gameMaster))
			return getPartialMatches(args, vil.getPlayerNameListExceptGmAndNpc());

		if(args.length==2 && args[0].equalsIgnoreCase("unkick")
				&& (vp.gameMaster))
			return getPartialMatches(args, vil.getKickedPlayerNameList());
		
		if(args.length==2 && args[0].equalsIgnoreCase("kill")
				&& (vp.gameMaster && vil.status==VillageStatus.ONGOING))
			return getPartialMatches(args, vil.getAlivePlayerNameList());
		
		if(args.length==2 && args[0].equalsIgnoreCase("whisp")
				&& (vp.alive && vil.permitWhisp
						&& vil.status==VillageStatus.ONGOING && vil.time!=VillageTime.NIGHT))
			return getPartialMatches(args, vil.getAlivePlayerNameListExceptMyselfAndNpc(pl));
		
		if(args.length==2 && args[0].equalsIgnoreCase("vote")
				&& (vp.alive
						&& vil.status==VillageStatus.ONGOING && (vil.time==VillageTime.NOON || vil.time==VillageTime.REVOTE)))
			return getPartialMatches(args, vil.getAlivePlayerNameListExceptMyself(pl));
		
		if(args.length==2 && args[0].equalsIgnoreCase("uranai")
				&& (vp.alive && vp.role==VillageRole.URANAISHI && !vp.tryUranai
						&& vil.status==VillageStatus.ONGOING && vil.time==VillageTime.NIGHT))
			return getPartialMatches(args, vil.getAlivePlayerNameListExceptMyselfAddedBittenPlayer(pl));
		
		if(args.length==2 && args[0].equalsIgnoreCase("guard")
				&& (vp.alive && vp.role==VillageRole.KARIUDO && vil.day!=0
						&& vil.status==VillageStatus.ONGOING && vil.time!=VillageTime.NIGHT))
			return getPartialMatches(args, vil.getAlivePlayerNameListExceptMyself(pl));
		
		if(args.length==2 && args[0].equalsIgnoreCase("bite")
				&& (vp.alive && vp.role==VillageRole.JINROU && !vil.tryBiting && vil.permitBite
						&& vil.status==VillageStatus.ONGOING && vil.time==VillageTime.NIGHT)){
			if(vil.day==0){
				List<String> compList = new ArrayList<String>();
				compList.add("Mr.Firvic");
				return compList;
			}else{
				return getPartialMatches(args, vil.getAlivePlayerNameListExceptJinrou());
			}
		}

		return ImmutableList.of();
	}
	
	public static List<String> getPartialMatches(String[] args, List<String> list){
		List<String> compList =  StringUtil.copyPartialMatches(args[args.length-1], list, new ArrayList<String>(list.size()));
		Collections.sort(compList, String.CASE_INSENSITIVE_ORDER);
		return compList;
	}

	public static List<String> getPartialMatchesToLowerCase(String[] args, List<String> list){
		List<String> compList =  StringUtil.copyPartialMatches(args[args.length-1], list, new ArrayList<String>(list.size()));
		for(int i=0; i<compList.size(); i++)
			compList.set(i, compList.get(i).toLowerCase());
		Collections.sort(compList);
		return compList;
	}
}
