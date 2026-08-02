package com.cahcap;

import com.cahcap.common.item.flowweavering.RingInteractionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point for Once Upon a Season mod.
 * This class contains platform-independent initialization code.
 */
public class OusCommon {
    
    public static final String MOD_ID = "ous";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    /**
     * Called during mod initialization on all platforms.
     */
    public static void init() {
        LOGGER.info("Once Upon a Season is initializing...");
        RingInteractionRegistry.init();
    }
    
    /**
     * Called during common setup phase.
     */
    public static void commonSetup() {
        LOGGER.info("Once Upon a Season common setup complete");
    }
}

