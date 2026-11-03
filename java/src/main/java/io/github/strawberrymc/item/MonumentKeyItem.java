package io.github.strawberrymc.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class MonumentKeyItem extends Item {
	public MonumentKeyItem(Item.Properties properties) {
		super(properties.rarity(Rarity.RARE).stacksTo(1));
	}
}