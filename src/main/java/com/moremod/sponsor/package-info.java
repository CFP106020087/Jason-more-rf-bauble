/**
 * 赞助者物品包
 *
 * 此包包含所有赞助者专属的武器、盔甲和饰品
 *
 * 使用方式:
 * 1. 在 SponsorConfig.java 中可以通过 enabled = false 完全禁用所有赞助者物品
 * 2. 在 SponsorItems.java 中添加新的赞助者物品
 * 3. 使用基类 SponsorSword, SponsorArmor, SponsorBauble 来创建自定义物品
 *
 * 配置文件位置: config/moremod/sponsor_items.cfg
 *
 * 示例 - 添加新武器:
 * <pre>
 * // 在 SponsorItems.java 中
 * public static Item MY_SPONSOR_SWORD;
 *
 * // 在 registerItems() 方法中
 * if (SponsorConfig.isWeaponsEnabled()) {
 *     MY_SPONSOR_SWORD = newSafe(() ->
 *         new SponsorSword("my_sponsor_sword", "我的赞助者之剑", "赞助者名称"),
 *         "my_sponsor_sword"
 *     );
 * }
 * </pre>
 *
 * 示例 - 添加新饰品:
 * <pre>
 * // 在 SponsorItems.java 中
 * public static Item MY_SPONSOR_AMULET;
 *
 * // 在 registerItems() 方法中
 * if (SponsorConfig.isBaublesEnabled()) {
 *     MY_SPONSOR_AMULET = newSafe(() ->
 *         new SponsorBauble("my_sponsor_amulet", "我的赞助者护符", BaubleType.AMULET, "赞助者名称"),
 *         "my_sponsor_amulet"
 *     );
 * }
 * </pre>
 *
 * 示例 - 自定义武器效果:
 * <pre>
 * public class MyCustomSword extends SponsorSword {
 *     public MyCustomSword() {
 *         super("my_custom_sword", "自定义武器");
 *     }
 *
 *     {@literal @}Override
 *     protected void addCustomTooltip(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
 *         tooltip.add("§a特殊效果: 攻击时释放闪电");
 *     }
 *
 *     {@literal @}Override
 *     public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
 *         // 添加自定义攻击效果
 *         return super.hitEntity(stack, target, attacker);
 *     }
 * }
 * </pre>
 *
 * @author moremod
 * @version 1.0
 */
@javax.annotation.ParametersAreNonnullByDefault
package com.moremod.sponsor;
