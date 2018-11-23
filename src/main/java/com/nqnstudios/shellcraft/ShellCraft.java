package com.nqnstudios.shellcraft;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ShellCraft.MODID, name = ShellCraft.NAME, version = ShellCraft.VERSION)
public class ShellCraft
{
    public static final String MODID = "shellcraft";
    public static final String NAME = "ShellCraft";
    public static final String VERSION = "0.0";

    protected static Logger logger;
    private static boolean shellMode = false;
    private static ShellCore core;

    static {
        try {
            core = new ShellCore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new ShellCommand());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent evt) throws IOException {
        if (evt.side == Side.CLIENT && evt.phase == TickEvent.Phase.END)
        {
            // Update the ShellCore
            core.tick();

            // Check for messages out of the ShellCore
            String output = core.takeOutput();
            if (output.length() > 0) {
                evt.player.sendMessage(new TextComponentString(output));
            }

        }
    }

    @SubscribeEvent
    public void onPlayerAttemptChat(ClientChatEvent event) throws IOException, InterruptedException {
        if (shellMode) {
            logger.debug("player attempted chat");
            logger.debug(event.getMessage());
            if (event.getMessage().charAt(0) != '/') {
                event.setCanceled(true);
                core.process(event.getMessage());
            }
        }
    }

    private class ShellCommand extends CommandBase {
        @Override
        public String getName()
        {
            return "shell";
        }

        @Override
        public String getUsage(ICommandSender sender)
        {
            return "shell";
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender)
        {
            return true;
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
        {
            return Collections.emptyList();
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            if (shellMode){
                shellMode = false;
                sender.sendMessage(new TextComponentString("Shell mode disabled!"));
            }
            else {
                shellMode = true;
                sender.sendMessage(new TextComponentString("Shell mode enabled!"));
            }
        }
    }
}
