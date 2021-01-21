package tmvkrpxl0.src;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import net.minecraft.server.v1_16_R3.DataConverterRegistry;
import net.minecraft.server.v1_16_R3.DataConverterTypes;
import net.minecraft.server.v1_16_R3.SharedConstants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class tmvkrpxl0Combat extends JavaPlugin{
    protected static ConsoleCommandSender sender;
    protected final Vector ZERO = new Vector(0,0,0);
    protected final String GREEN = "[" + ChatColor.GREEN + "tCombat" + ChatColor.RESET + ']';
    protected final String RED = "[" + ChatColor.RED + "tCombat" + ChatColor.RESET + ']';
    protected final Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();
    private CombatListener listener;
    PluginDescriptionFile pdf = getDescription();

    @Override
    public void onEnable() {
        CombatUtil.main = this;
        sender = Bukkit.getConsoleSender();
        sender.sendMessage("[" + ChatColor.LIGHT_PURPLE + 't' + ChatColor.BLACK + 'm' +
                ChatColor.LIGHT_PURPLE + 'v' + ChatColor.BLACK + 'k' +
                ChatColor.LIGHT_PURPLE + 'r' + ChatColor.BLACK + 'p' +
                ChatColor.LIGHT_PURPLE + 'x' + ChatColor.BLACK + 'l' +
                ChatColor.LIGHT_PURPLE + '0' + ChatColor.RESET + ']' + ChatColor.GREEN + "Combat Plugin 0.1v");

        getCommand("tCombat").setExecutor(new CombatCommands(this));
        listener = new CombatListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        for(Player player : Bukkit.getOnlinePlayers()){
            playerConfigs.put(player.getUniqueId(), new PlayerConfig());
        }
        ConfigurationSerialization.registerClass(PlayerConfig.class, "PlayerConfig");

        File configFile = new File(getDataFolder() + File.separator + "playerConfig.yml");
        try{
            if(!configFile.exists()){
                configFile.getParentFile().mkdir();
                configFile.createNewFile();
            }
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);
            Set<String> keys = yamlConfiguration.getKeys(false);
            for(String s : keys){
                UUID uuid = UUID.fromString(s);
                PlayerConfig config = (PlayerConfig) yamlConfiguration.get(s);
                playerConfigs.put(uuid, config);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDisable() {
        Map<Object, Type<?>> typeMap = (Map<Object, Type<?>>) DataConverterRegistry.a().getSchema(DataFixUtils.makeKey(SharedConstants.getGameVersion().getWorldVersion())).findChoiceType(DataConverterTypes.ENTITY_TREE).types();
        typeMap.remove("solid_falling_block");
        File configFile = new File(getDataFolder() + File.separator + "playerConfig.yml");
        try{
            if(!configFile.exists()){
                configFile.getParentFile().mkdir();
                configFile.createNewFile();
            }
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);
            for(UUID uuid : playerConfigs.keySet()){
                yamlConfiguration.set(uuid.toString(), playerConfigs.get(uuid));
            }
            yamlConfiguration.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(UUID uuid : playerConfigs.keySet()){
            Player player = Bukkit.getPlayer(uuid);
            if(player!=null){
                if(player.getGameMode().equals(GameMode.SURVIVAL)){
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
                for(LivingEntity livingEntity : playerConfigs.get(uuid).targets){
                    CombatUtil.setGlowing(livingEntity, player, false);
                }
            }

        }
        listener.cleanUp();
    }

}
