package dev.xkmc.l2serial.serialization.generic_types;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

public record HolderReg<T>(CodecReg<Holder<T>> holder, CodecReg<HolderSet<T>> set) {

}
