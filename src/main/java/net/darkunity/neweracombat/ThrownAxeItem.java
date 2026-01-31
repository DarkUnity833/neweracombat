package net.darkunity.neweracombat;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

public class ThrownAxeItem extends Item {
    public ThrownAxeItem(Properties props) {
        super(props);
    }
    
    // Пример спавна (если тестишь через предмет)
    private void testSpawn(Level level) {
        // Обязательно .get()
        new ThrownAxeEntity(ModEntities.THROWN_AXE.get(), level);
    }
}