package com.nqnstudios.shellcraft;

import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ShellCraft.MODID, name = ShellCraft.NAME, version = ShellCraft.VERSION)
public class ShellCraft
{
    public static final String MODID = "shellcraft";
    public static final String NAME = "ShellCraft";
    public static final String VERSION = "0.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {

    }
}
