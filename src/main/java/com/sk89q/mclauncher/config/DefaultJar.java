package com.sk89q.mclauncher.config;

import java.io.File;

public class DefaultJar extends MinecraftJar {

    public DefaultJar(File f) {
        super(f);
    }
    
    @Override
    public String toString() {
        return "<default>";
    }

}
