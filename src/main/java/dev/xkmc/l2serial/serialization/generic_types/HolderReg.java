package dev.xkmc.l2serial.serialization.generic_types;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

public record HolderReg<T>(HolderCodecReg<Holder<T>> holder, HolderCodecReg<HolderSet<T>> set) {

}
