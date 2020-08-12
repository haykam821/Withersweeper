package io.github.haykam821.withersweeper;

import io.github.haykam821.withersweeper.game.WithersweeperConfig;
import io.github.haykam821.withersweeper.game.phase.WithersweeperActivePhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "withersweeper";

	private static final Identifier WITHERSWEEPER_ID = new Identifier(MOD_ID, "withersweeper");
	public static final GameType<WithersweeperConfig> WITHERSWEEPER_TYPE = GameType.register(WITHERSWEEPER_ID, WithersweeperActivePhase::open, WithersweeperConfig.CODEC);

	@Override
	public void onInitialize() {
		return;
	}
}