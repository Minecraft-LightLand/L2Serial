package dev.xkmc.l2serial.util;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;

public class ModContainerHack {

	public static ModContainer getMod(String modid) {
		var cont = ModLoadingContext.get().getActiveContainer();
		if (cont.getModId().equals(modid)) return cont;
		var opt = ModList.get().getModContainerById(modid);
		if (opt.isPresent()) return opt.get();
		throw new IllegalStateException("Class Initialized from wrong thread for " + modid);
	}

}
