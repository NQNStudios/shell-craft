package com.nqnstudios.shellcraft;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraftforge.fml.common.eventhandler.EventPriority;
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
    private static ShellCore core;

    static {
        core = new ShellCore();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        core.setLogger(logger);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new ShellCommand());
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent evt) {
        //logger.debug("tick event with side " +evt.side.name() + " and phase " + evt.phase.name());
        if (evt.side == Side.CLIENT && evt.phase == TickEvent.Phase.END)
        {
            // Update the ShellCore
            core.tick();

            // Check for messages out of the ShellCore
            String output = null;
            try {
                output = core.takeOutput();
            } catch (IOException e) {
                logger.debug("ioexception from takeoutput");
                e.printStackTrace();

            }
            //logger.debug("checking for output");
            if (output != null && output.length() > 0) {
                logger.debug("returning output " + output);
                evt.player.sendMessage(new TextComponentString(output.replace("\r", "")));
            }

        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttemptChat(ClientChatEvent event) {
        try {
            ProcessResult result = core.process(event.getMessage());
            switch (result.type) {
                case NotProcessed:
                    return;
                case NoOutput:
                    event.setCanceled(true);
                case NoShell:
                    // TODO warn that there is no shell active
                case Output:
                    event.setMessage(result.output);
            }
        } catch (IOException | InterruptedException e) {
            logger.debug("ioexception from core.process(\"" + event.getMessage() + "\"");
            e.printStackTrace();
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
            String shell = "";
            if (args.length > 0) {
                shell = args[0];
            }

            if (core.start(shell)) {
                sender.sendMessage(new TextComponentString("Shell started!"));
            } else {
                sender.sendMessage(new TextComponentString("Something went wrong!"));
            }
        }
    }
}
