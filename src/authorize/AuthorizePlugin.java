package authorize;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration.*;

public class AuthorizePlugin extends Plugin{
    public static final float messageSpacing = 60f;

    private ObjectSet<String> authorized = new ObjectSet<>();
    private ObjectFloatMap<Player> messageTime = new ObjectFloatMap<>();
    private String message = "[scarlet]You are not authorized to perform this action.";
    private boolean authUnits = true;

    @Override
    public void init(){
        authorized = Core.settings.getJson("authorized-players", ObjectSet.class, String.class, ObjectSet::new);
        message = Core.settings.getString("authorized-message", message);
        authUnits = Core.settings.getBool("allow-unauthorized-units", true);

        Vars.netServer.admins.addActionFilter(action -> {
            if(action.player == null) return true;
            if(action.player.admin || authorized.contains(action.player.usid()) ||
                (authUnits && (action.type == ActionType.control || action.type == ActionType.command)) || //check if they can command units
                (action.type == ActionType.control && action.unit == null) //make sure they can un-control units
            ){
                return true;
            }else{
                message(action.player);
                return false;
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("auth", "<add/remove> <player...>", "Authorize or unauthorize player by name or UUID.", arg -> {
            Player player = Groups.player.find(p -> p.uuid().equals(arg[1]) || Strings.stripColors(p.name).equals(Strings.stripColors(arg[1])));
            if(arg[0].equals("add")){
                if(player != null){
                    authorized.add(player.usid());
                    Log.info("Authorized: @", player.name);
                    save();
                }else{
                    Log.err("Player not found. Note that they must be online for authorization to work.");
                }
            }else if(arg[0].equals("remove")){
                if(player != null){
                    authorized.remove(player.usid());
                    Log.info("Un-authorized: @", player.name);
                    save();
                }else{
                    Log.err("Player not found. Note that they must be online for authorization to work.");
                }
            }else{
                Log.err("Incorrect usage. First argument must be 'add' or 'remove'.");
            }
        });

        handler.register("auth-message", "[message...]", "Set the message displayed when someone is not authorized to perform an action.", arg -> {
            if(arg.length > 0){
                message = arg[0];
                save();
                Log.info("Message set.");
            }else{
                Log.info("Current message: @", message);
            }
        });

        handler.register("auth-units", "[yes/no]", "Set whether unauthorized players are able to control units. Default: yes.", arg -> {
            if(arg.length > 0){
                authUnits = arg[0].equals("yes");
                save();
                Log.info("auth-units set to '@'.", arg[0].equals("yes") ? "yes" : "no");
            }else{
                Log.info("Current value: @", authUnits ? "yes" : "no");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

    }

    private void message(Player player){
        if(Time.time - messageTime.get(player, 0) >= messageSpacing){
            player.sendMessage(message);
            messageTime.put(player, Time.time);
        }
    }

    private void save(){
        Core.settings.put("authorized-message", message);
        Core.settings.put("allow-unauthorized-units", authUnits);
        Core.settings.putJson("authorized-list", String.class, authorized);
    }
}
