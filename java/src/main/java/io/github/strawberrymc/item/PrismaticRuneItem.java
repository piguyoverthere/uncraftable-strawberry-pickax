package io.github.strawberrymc.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PrismaticRuneItem extends Item {
	public PrismaticRuneItem(Item.Properties properties) {
		super(properties.rarity(Rarity.EPIC).stacksTo(1));
	}

	@Override
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}
}