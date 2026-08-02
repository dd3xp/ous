package com.cahcap.neoforge.common.registry;

import com.cahcap.OusCommon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    
    public static final DeferredRegister<SoundEvent> SOUNDS = 
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, OusCommon.MOD_ID);
}

