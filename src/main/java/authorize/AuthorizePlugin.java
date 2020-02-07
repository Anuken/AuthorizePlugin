package authorize;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.type.*;
import mindustry.net.Administration.*;
import mindustry.plugin.*;
import mindustry.world.blocks.units.*;

public class AuthorizePlugin extends Plugin{
    public static final float messageSpacing = 60f;

    private ObjectSet<String> authorized = new ObjectSet<>();
    private ObjectFloatMap<Player> messageTime = new ObjectFloatMap<>();
    private String message = "[scarlet]You are not authorized to perform this action.";

    @Override
    public void init(){
        authorized = Core.settings.getObject("authorized-list", ObjectSet.class, ObjectSet::new);
        message = Core.settings.getString("authorized-message", message);

        Vars.netServer.admins.addActionFilter(action -> {
            if(action.player == null) return true;
            if(action.player.isAdmin || authorized.contains(action.player.usid) || (action.type == ActionType.tapTile && action.tile.block() instanceof MechPad)){
                return true;
            }else{
                message(action.player);
                return false;
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("authorize", "<add/remove> <player...>", "Authorize or unauthorize player by name or UUID.", arg -> {
            Player player = Vars.playerGroup.find(p -> p.uuid.equals(arg[1]) || Strings.stripColors(p.name).equals(Strings.stripColors(arg[1])));
            if(arg[0].equals("add")){
                if(player != null){
                    authorized.add(player.usid);
                    Log.info("Authorized: &ly{0}", player.name);
                    save();
                }else{
                    Log.err("Player not found. Note that they must be online for authorization to work.");
                }
            }else if(arg[0].equals("remove")){
                if(player != null){
                    authorized.remove(player.usid);
                    Log.info("Un-authorized: &ly{0}", player.name);
                    save();
                }else{
                    Log.err("Player not found. Note that they must be online for authorization to work.");
                }
            }else{
                Log.err("Incorrect usage. First argument must be 'add' or 'remove'.");
            }
        });

        handler.register("authorize-message", "[message...]", "Set the message displayed when someone is not authorized to perform an action.", arg -> {
            if(arg.length > 0){
                message = arg[0];
                save();
                Log.info("Message set.");
            }else{
                Log.info("Current message: {0}", message);
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

    }

    private void message(Player player){
        if(Time.time() - messageTime.get(player, 0) >= messageSpacing){
            player.sendMessage(message);
            messageTime.put(player, Time.time());
        }
    }

    private void save(){
        Core.settings.put("authorized-message", message);
        Core.settings.putObject("authorized-list", authorized);
        Core.settings.save();
    }
}
