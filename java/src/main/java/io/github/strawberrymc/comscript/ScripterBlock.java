package io.github.strawberrymc.comscript;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

// The Scripter: a command block that runs a whole ComScript script. A redstone pulse runs it, and operators edit it in a
// code editor (ScripterScreen) that also has Run and Stop buttons and shows the script's terminal.
public class ScripterBlock extends BaseEntityBlock implements GameMasterBlock {
	public static final MapCodec<ScripterBlock> CODEC = simpleCodec(ScripterBlock::new);

	public ScripterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ScripterBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ComScript.SCRIPTER_ENTITY.get(), ScripterBlockEntity::serverTick);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ScripterBlockEntity scripter))
			return;
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != scripter.isPowered()) {
			scripter.setPowered(powered);
			// Runs once on each rising edge, a tick later like a command block
			if (powered)
				level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof ScripterBlockEntity scripter)
			scripter.run();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof ScripterBlockEntity scripter) || !player.canUseGameMasterBlocks())
			return InteractionResult.PASS;
		// The editor is client-only, so its class is only touched on the client
		if (level.isClientSide())
			io.github.strawberrymc.comscript.client.ScripterScreen.open(scripter);
		return InteractionResult.SUCCESS;
	}
}
