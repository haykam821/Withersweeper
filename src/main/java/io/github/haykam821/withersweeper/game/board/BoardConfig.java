package io.github.haykam821.withersweeper.game.board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class BoardConfig {
	public static final Codec<BoardConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(map -> map.x),
			Codec.INT.fieldOf("z").forGetter(map -> map.z),
			Codec.INT.fieldOf("mines").forGetter(map -> map.mines)
		).apply(instance, BoardConfig::new);
	});

	public final int x;
	public final int z;
	public final int mines;

	public BoardConfig(int x, int z, int mines) {
		this.x = x;
		this.z = z;
		this.mines = mines;
	}
}