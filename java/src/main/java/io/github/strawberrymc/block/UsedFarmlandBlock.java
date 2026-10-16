package io.github.strawberrymc.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class UsedFarmlandBlock extends FarmlandBlock {
    // Define your property (e.g., values from 0 to 5)
    public static final IntegerProperty USES = IntegerProperty.create("custom_int", 0, 5);

    public UsedFarmlandBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Include your property in the default blockstate along with vanilla moisture
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MOISTURE, 0)
            .setValue(USES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(USES); // Inject your property into the blockstate container
    }

}
