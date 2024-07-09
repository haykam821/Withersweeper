package io.github.haykam821.withersweeper.game;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.withersweeper.game.board.BoardConfig;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;

public class WithersweeperConfig {
	public static final Codec<WithersweeperConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			BoardConfig.CODEC.fieldOf("board").forGetter(WithersweeperConfig::getBoardConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(WithersweeperConfig::getPlayerConfig),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(WithersweeperConfig::getTicksUntilClose),
			GameStatisticBundle.NAMESPACE_CODEC.optionalFieldOf("statistic_bundle_namespace").forGetter(WithersweeperConfig::getStatisticBundleNamespace),
			ItemStack.CODEC.optionalFieldOf("flag_stack", new ItemStack(Items.RED_BANNER)).forGetter(WithersweeperConfig::getFlagStack),
			Codec.INT.optionalFieldOf("max_mistakes", 1).forGetter(WithersweeperConfig::getMaxMistakes)
		).apply(instance, WithersweeperConfig::new);
	});

	private final BoardConfig boardConfig;
	private final PlayerConfig playerConfig;
	private final IntProvider ticksUntilClose;
	private final Optional<String> statisticBundleNamespace;
	private final ItemStack flagStack;
	private final int maxMistakes;

	public WithersweeperConfig(BoardConfig boardConfig, PlayerConfig playerConfig, IntProvider ticksUntilClose, Optional<String> statisticBundleNamespace, ItemStack flagStack, int maxMistakes) {
		this.boardConfig = boardConfig;
		this.playerConfig = playerConfig;
		this.ticksUntilClose = ticksUntilClose;
		this.statisticBundleNamespace = statisticBundleNamespace;
		this.flagStack = flagStack;
		this.maxMistakes = maxMistakes;
	}

	public BoardConfig getBoardConfig() {
		return this.boardConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}

	private Optional<String> getStatisticBundleNamespace() {
		return this.statisticBundleNamespace;
	}

	public GameStatisticBundle getStatisticBundle(GameSpace gameSpace) {
		if (this.statisticBundleNamespace.isEmpty()) {
			return null;
		}
		return gameSpace.getStatistics().bundle(this.statisticBundleNamespace.get());
	}

	public ItemStack getFlagStack() {
		return this.flagStack;
	}

	public int getMaxMistakes() {
		return this.maxMistakes;
	}
}