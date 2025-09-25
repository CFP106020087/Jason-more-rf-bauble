package com.moremod.mixinhelper;

public class CapBypassFlag {
  // 阿斯摩太护盾穿透标记
    public static final ThreadLocal<Boolean> ASMODEUS_BYPASS = ThreadLocal.withInitial(() -> false);
}